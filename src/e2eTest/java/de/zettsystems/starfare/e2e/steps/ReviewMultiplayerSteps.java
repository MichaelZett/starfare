package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.e2e.ReviewGameFixture;
import de.zettsystems.starfare.game.application.Broadcaster;
import de.zettsystems.starfare.game.application.GameEvent;
import de.zettsystems.starfare.game.application.GameRegistry;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;
import io.cucumber.java.After;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Two browser sessions exercise push delivery and outcome continuation. */
public class ReviewMultiplayerSteps {
    private static final String OUTCOME = ".game-outcome-dialog[opened]";
    private static final String CHAT_INPUT = "vaadin-dialog[opened] vaadin-text-area textarea";
    private final Browser browser;
    private final ReviewGameFixture fixture;
    private final GameRegistry registry;
    private final GameService games;
    private final Broadcaster broadcaster;
    private Browser guest;
    private ReviewGameFixture.Game current;
    private Instant beforeContinuation;

    public ReviewMultiplayerSteps(Browser browser, ReviewGameFixture fixture, GameRegistry registry,
                                  GameService games, Broadcaster broadcaster) {
        this.browser = browser;
        this.fixture = fixture;
        this.registry = registry;
        this.games = games;
        this.broadcaster = broadcaster;
    }

    @Wenn("zwei Browsersitzungen eine Partie für {string} und {string} öffnen")
    public void twoSessions(String hostEmail, String guestEmail) {
        current = fixture.create(hostEmail, guestEmail);
        browser.open("/map/" + current.id().value());
        browser.awaitCss(".round-status");
        guest = browser.newSession();
        guest.logIn(guestEmail);
        guest.open("/map/" + current.id().value());
        guest.awaitCss(".round-status");
    }

    @Dann("erscheinen fremde Chat-Nachrichten auch nach erneutem Öffnen live")
    public void liveChat() {
        openChat(browser);
        openChat(guest);
        browser.awaitCss(CHAT_INPUT).sendKeys("Ungesendeter Entwurf");
        sendChat(guest, "Erste Nachricht");
        browser.awaitTextIn(".game-chat-messages", "Erste Nachricht");
        assertThat(browser.awaitCss(CHAT_INPUT).getDomProperty("value")).isEqualTo("Ungesendeter Entwurf");
        browser.clickButtonWithText("Schließen");
        sendChat(guest, "Während der Chat geschlossen ist");
        openChat(browser);
        browser.awaitTextIn(".game-chat-messages", "Während der Chat geschlossen ist");
        sendChat(guest, "Dritte Nachricht nach erneutem Öffnen");
        browser.awaitTextIn(".game-chat-messages", "Dritte Nachricht nach erneutem Öffnen");
        assertThat(browser.awaitCss(CHAT_INPUT).getDomProperty("value")).isEqualTo("Ungesendeter Entwurf");
        assertThat(browser.textsOf(".game-chat-messages")).anyMatch(text -> text.contains("Erste Nachricht"));
    }

    private void openChat(Browser session) {
        ((JavascriptExecutor) session.driver()).executeScript("""
                const menu = document.querySelector('.app-header-menu');
                const button = menu.querySelector('vaadin-menu-bar-button')
                    || menu.shadowRoot.querySelector('vaadin-menu-bar-button');
                button.click();
                """);
        WebElement item = new WebDriverWait(session.driver(), Duration.ofSeconds(10)).until(_ ->
                session.all("vaadin-menu-bar-item").stream()
                        .filter(WebElement::isDisplayed)
                        .filter(candidate -> candidate.getText().strip().equals("Partie-Chat"))
                        .findFirst().orElse(null));
        ((JavascriptExecutor) session.driver()).executeScript("arguments[0].click();", item);
        session.awaitCss(CHAT_INPUT);
    }

    private void sendChat(Browser session, String text) {
        session.awaitCss(CHAT_INPUT).sendKeys(text);
        session.clickButtonWithText("Senden");
        session.awaitTextIn(".game-chat-messages", text);
    }

    @Dann("hat ein Sieg mit 0:0 Restschiffen die richtige Farbe und Siegschwelle")
    public void firstVictory() {
        finishRound(false);
        guest.awaitCss(OUTCOME);
        browser.awaitCss(".event-battle-pending");
        acknowledgeBattle(browser, true);
        browser.awaitCss(OUTCOME);
        browser.awaitTextIn(".event-victory", "60");
        assertThat(browser.all(".review-controls")).noneMatch(WebElement::isDisplayed);
    }

    @Wenn("der Sieger bis zur vollständigen Eroberung weiterspielt")
    public void continueGame() {
        beforeContinuation = Instant.now();
        browser.clickButtonWithText("Bis zur vollständigen Eroberung weiterspielen");
    }

    @Dann("schließen beide Ergebnisdialoge und die Rundenfrist beginnt neu")
    public void bothSessionsContinue() {
        awaitNoOutcome(browser);
        awaitNoOutcome(guest);
        browser.awaitCss(".round-status-ends .round-status-clock");
        guest.awaitCss(".round-status-ends .round-status-clock");
        Instant started = registry.readState(current.id(), GameState::turnStartedAt);
        assertThat(started).isBetween(beforeContinuation, Instant.now());
        assertThat(registry.readState(current.id(), GameState::gameOver).booleanValue()).isFalse();
        assertThat(registry.readState(current.id(), GameState::victorySystemPercent).intValue()).isEqualTo(100);
    }

    @Dann("wartet das zweite Spielende in beiden Sitzungen wieder auf die offenen Schlachten")
    public void secondOutcomeWaits() {
        finishRound(true);
        assertPending(browser);
        assertPending(guest);
        browser.driver().navigate().refresh();
        browser.awaitCss(".map-content");
        browser.clickTabWithText("Bericht");
        assertPending(browser);
        acknowledgeBattle(browser, true);
        browser.awaitCss(OUTCOME);
        browser.awaitTextIn(".event-victory", "100");
        assertPending(guest);
        guest.openDetailsWithText("Filter: 6 von 6 Ereignisarten");
        var speed = guest.awaitCss(".report-filter-options vaadin-combo-box input");
        speed.sendKeys(Keys.chord(Keys.CONTROL, "a"), "Sofort");
        new WebDriverWait(guest.driver(), Duration.ofSeconds(10)).until(_ ->
                guest.all("vaadin-combo-box-item").stream().filter(WebElement::isDisplayed)
                        .filter(item -> item.getText().strip().equals("Sofort")).findFirst().orElse(null)).click();
        guest.clickButtonWithText("Alle offenen Schlachten auswerten");
        guest.awaitCss(OUTCOME);
        assertThat(guest.textsOf(".game-outcome-personal-result")).contains("Dein Ergebnis: Niederlage");
    }

    private void finishRound(boolean fullConquest) {
        registry.writeState(current.id(), state -> {
            int systemId = fullConquest ? 3 : 4;
            String name = state.getSystem(systemId).name();
            if (fullConquest) {
                List.copyOf(state.systems()).forEach(system ->
                        state.updateSystem(system.id(), before -> before.captureBy(current.hostSeat(), 0)));
            } else {
                state.configureVictorySystemPercent(60);
                state.updateSystem(systemId, system -> system.captureBy(current.hostSeat(), 0));
            }
            state.reports().put(current.hostSeat(), new TurnReport(state.turn(), List.of(), List.of(
                    new TurnEvent.BattleWon(current.hostSeat(), systemId, name, 1, 1, 0, !fullConquest, 1.1, 0.9),
                    new TurnEvent.Victory(current.hostSeat(), state.victorySystemPercent()))));
            List<TurnEvent> guestEvents = fullConquest ? List.of(
                    new TurnEvent.SystemLost(current.guestSeat(), current.hostSeat(), systemId, name,
                            1, 1, 0, 1.1, 0.9), new TurnEvent.Defeat(current.hostSeat(), "Review Host"))
                    : List.of(new TurnEvent.Defeat(current.hostSeat(), "Review Host"));
            state.reports().put(current.guestSeat(), new TurnReport(state.turn(), List.of(), guestEvents));
            state.captureReplayFrame();
            state.endGame(current.hostSeat());
            state.nextTurn();
            return null;
        });
        broadcaster.publish(new GameEvent.TurnAdvanced(current.id(), registry.readState(current.id(), GameState::turn)));
        broadcaster.publish(new GameEvent.GameFinished(current.id(), current.hostSeat()));
    }

    private void acknowledgeBattle(Browser session, boolean capture) {
        session.clickCss(".event-battle-pending");
        session.awaitCss(".battle-replay-result-visible");
        assertThat(session.all(".battle-replay-attacker-won")).hasSize(1);
        assertThat(session.textsOf("[data-battle-attacking]")).contains("0");
        assertThat(session.textsOf("[data-battle-defending]")).contains("0");
        session.clickButtonWithText("Ergebnis übernehmen");
        if (capture) {
            session.clickCss(".capture-summary-dialog vaadin-button");
        }
    }

    private void assertPending(Browser session) {
        session.awaitCss(".event-battle-pending");
        assertThat(session.all(".event-battle-pending")).hasSize(1);
        assertThat(session.all(OUTCOME)).isEmpty();
        assertThat(session.all(".review-controls")).noneMatch(WebElement::isDisplayed);
    }

    private void awaitNoOutcome(Browser session) {
        new WebDriverWait(session.driver(), Duration.ofSeconds(10)).until(_ -> session.all(OUTCOME).isEmpty());
        assertThat(session.all(OUTCOME)).isEmpty();
    }

    @After
    public void cleanup() {
        if (current != null) { games.abortGame(current.id()); }
        if (guest != null) { guest.closeSession(); }
    }
}
