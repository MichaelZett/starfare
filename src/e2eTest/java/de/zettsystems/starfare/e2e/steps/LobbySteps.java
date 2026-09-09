package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.game.application.Broadcaster;
import de.zettsystems.starfare.game.application.GameEvent;
import de.zettsystems.starfare.game.application.GameRegistry;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameVisibility;
import de.zettsystems.starfare.social.application.InvitationService;
import de.zettsystems.starfare.social.application.PresenceTracker;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;

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
}
