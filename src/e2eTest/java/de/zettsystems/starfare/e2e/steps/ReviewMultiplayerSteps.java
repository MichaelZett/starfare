package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.testsupport.ReviewGameFixture;
import de.zettsystems.starfare.game.application.GameService;
import io.cucumber.java.After;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;

import static org.assertj.core.api.Assertions.assertThat;

/** Two browser sessions exercise live push delivery. */
public class ReviewMultiplayerSteps {
    private static final String CHAT_INPUT = ".game-chat-panel vaadin-text-area textarea";
    private final Browser browser;
    private final ReviewGameFixture fixture;
    private final GameService games;
    private Browser guest;
    private ReviewGameFixture.Game current;

    public ReviewMultiplayerSteps(Browser browser, ReviewGameFixture fixture, GameService games) {
        this.browser = browser;
        this.fixture = fixture;
        this.games = games;
    }

    @Wenn("zwei Browsersitzungen eine Partie für {string} und {string} öffnen")
    public void twoSessions(String hostEmail, String guestEmail) {
        current = fixture.create(hostEmail, guestEmail);
        browser.open(Browser.LOBBY + "/map/" + current.id().value());
        browser.awaitCss(".round-status");
        guest = browser.newSession();
        guest.logIn(guestEmail);
        guest.open(Browser.LOBBY + "/map/" + current.id().value());
        guest.awaitCss(".round-status");
    }

    @Dann("erscheinen fremde Chat-Nachrichten auch nach erneutem Öffnen live")
    public void liveChat() {
        openChat(browser);
        openChat(guest);
        browser.type(CHAT_INPUT, "Ungesendeter Entwurf");
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
        session.clickButtonWithText("Partie-Chat");
        session.awaitCss(CHAT_INPUT);
    }

    private void sendChat(Browser session, String text) {
        session.type(CHAT_INPUT, text);
        session.clickButtonWithText("Senden");
        session.awaitTextIn(".game-chat-messages", text);
    }

    @After
    public void cleanup() {
        if (current != null) { games.abortGame(current.id()); }
        if (guest != null) { guest.closeSession(); }
    }
}
