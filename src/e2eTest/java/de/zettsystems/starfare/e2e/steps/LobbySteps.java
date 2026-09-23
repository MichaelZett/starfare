package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.game.application.Broadcaster;
import de.zettsystems.starfare.game.application.GameEvent;
import de.zettsystems.starfare.game.application.GameRegistry;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameVisibility;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.social.application.InvitationService;
import de.zettsystems.starfare.social.application.PresenceTracker;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class LobbySteps {
    private final Browser browser;
    private final GameService games;
    private final GameRegistry registry;
    private final Broadcaster broadcaster;
    private final InvitationService invitations;
    private final PresenceTracker presence;
    private final PlayerDirectory players;
    private GameId id;
    private String name;
    private String host;

    public LobbySteps(Browser browser, GameService games, GameRegistry registry, Broadcaster broadcaster,
                      InvitationService invitations, PresenceTracker presence, PlayerDirectory players) {
        this.browser = browser;
        this.games = games;
        this.registry = registry;
        this.broadcaster = broadcaster;
        this.invitations = invitations;
        this.presence = presence;
        this.players = players;
    }

    @Wenn("ich eine private Lobby-Partie ohne Beitritt anlege")
    public void createPrivateGame() {
        browser.clickButtonWithText("Neues Spiel");
        browser.awaitText("Neue Partien sind privat.");
        WebElement joinAfterCreate = browser.awaitCss("#join-after-create");
        ((JavascriptExecutor) browser.driver()).executeScript(
                "arguments[0].scrollIntoView({block: 'center'}); arguments[0].click();", joinAfterCreate);
        browser.clickButtonWithText("Spiel anlegen");
        browser.awaitText("Privat");
        id = games.listGames().getLast();
        name = games.gameNameOf(id);
        host = games.hostPlayerIdOf(id).orElseThrow();
        assertThat(games.summaryOf(id).visibility()).isEqualTo(GameVisibility.PRIVATE);
    }

    @Wenn("ich die Browsersitzung wechsle")
    public void switchSession() { browser.resetSession(); }

    @Dann("ist die private Lobby-Partie verborgen")
    public void privateGameHidden() {
        browser.awaitCss(".lobby-root");
        assertThat(browser.pageText()).doesNotContain(name);
        browser.open("/map/" + id.value());
        browser.awaitCss(".lobby-root");
        assertThat(browser.driver().getCurrentUrl()).doesNotContain("/map/");
    }

    @Wenn("ich die Partie im Verwalten-Dialog veröffentliche")
    public void publishGame() {
        browser.openDetailsWithText("Weitere Aktionen");
        browser.clickButtonWithText("Verwalten");
        browser.awaitCss("vaadin-checkbox").click();
        browser.clickButtonWithText("Schließen");
        browser.awaitText("Öffentlich");
        assertThat(games.summaryOf(id).visibility()).isEqualTo(GameVisibility.PUBLIC);
    }

    @Dann("ist die Lobby-Partie sichtbar")
    public void gameVisible() {
        browser.awaitText(name);
        assertThat(browser.pageText()).contains(name);
    }

    @Wenn("der Host die Partie wieder privat stellt und {string} einlädt")
    public void invite(String displayName) {
        String guest = presence.onlineUsers().stream().map(p -> p.playerId())
                .filter(account -> players.displayName(account).equals(displayName)).findFirst().orElseThrow();
        assertThat(games.changeVisibility(id, host, GameVisibility.PRIVATE)).isTrue();
        assertThat(invitations.inviteUser(id, host, guest)).isPresent();
    }

    @Wenn("ich die Lobby-Einladung annehme")
    public void acceptInvite() { browser.clickButtonWithText("Annehmen"); }

    @Wenn("die Lobby-Partie regulär beendet wird")
    public void finishGame() {
        // Deterministic fixture: exercise the real finish notification without an AI simulation.
        assertThat(games.startGame(id)).isTrue();
        registry.writeState(id, state -> { state.endGame(1); return null; });
        broadcaster.publish(new GameEvent.GameFinished(id, 1));
    }

    @Dann("liegt die Partie nur im Archiv und lässt sich lesend öffnen")
    public void reviewArchive() {
        browser.open("/");
        browser.awaitCss(".lobby-root");
        assertThat(browser.pageText()).doesNotContain(name);
        browser.clickButtonWithText("Archiv");
        browser.awaitText(name);
        browser.clickButtonWithText("Ansehen");
        browser.awaitUrl(url -> url.contains("/map/"), "Archive map did not open");
        browser.awaitText(name);
        assertThat(browser.pageText()).doesNotContain("Nächste Runde", "Verlegungen verwalten");
    }
    @Dann("kann ich Perspektive und Kriegsnebel der Nachbetrachtung wechseln")
    public void switchReviewPerspective() {
        browser.awaitCss("#review-fog").click();
        browser.awaitCss(".sys-fog");
        String aiName = games.reviewFor(id, host).orElseThrow().players().stream()
                .filter(Player::ai).findFirst().orElseThrow().label();
        var selection = browser.awaitCss("#review-perspective input");
        selection.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        selection.sendKeys(aiName);
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10))
                .until(_ -> browser.all("vaadin-combo-box-item").stream()
                        .filter(item -> item.isDisplayed() && item.getText().equals(aiName))
                        .findFirst().orElse(null)).click();
        var ai = games.reviewFor(id, host).orElseThrow().players().stream()
                .filter(player -> player.label().equals(aiName)).findFirst().orElseThrow();
        var home = games.reviewFor(id, host, ai.id(), true).orElseThrow().systems().stream()
                .filter(system -> Objects.equals(system.ownerId(), ai.id()))
                .findFirst().orElseThrow();
        browser.awaitTextIn(".sys-own", home.name());
        assertThat(browser.awaitCss("#review-perspective input").getDomProperty("value")).isEqualTo(aiName);
        browser.awaitCss("#review-fog").click();
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10))
                .until(_ -> browser.all(".sys-fog").isEmpty());
        assertThat(browser.all(".sys-fog")).isEmpty();
        assertThat(browser.pageText()).doesNotContain("Nächste Runde", "Verlegungen verwalten");
    }

    @Dann("kann ich die Inhalte der Karten-Seitenleiste wählen")
    public void switchMapSidebarSections() {
        browser.clickTabWithText("Kontakte");
        browser.awaitText("Noch keine Feinderkenntnisse.");
        browser.clickTabWithText("Details");
        browser.awaitText("System oder Flotte auf der Karte auswählen.");
        browser.clickTabWithText("Flotten");
        browser.awaitText("Eigene Flotten");
        browser.clickTabWithText("Befehle");
        browser.awaitText("Geplante Befehle");
        browser.clickTabWithText("Verlegungen");
        browser.awaitText("Produktionsverlegungen");
        browser.clickTabWithText("Bericht");
        browser.awaitSelectedTabWithText("Bericht");
    }

}
