package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.GameVisibility;
import io.cucumber.java.After;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RoundSteps {
    private static final String CLOCK = "\\d+:\\d{2}(:\\d{2})?";

    private final Browser browser;
    private final GameService games;
    private final UserAccountService accounts;
    private GameId id;
    private String guestAccountEmail;
    private Browser guestSession;

    public RoundSteps(Browser browser, GameService games, UserAccountService accounts) {
        this.browser = browser;
        this.games = games;
        this.accounts = accounts;
    }

    private String accountId(String email) {
        return String.valueOf(accounts.findByEmail(email).orElseThrow().id());
    }

    @Wenn("eine laufende Partie für {string} und {string} mit Standardfristen bereitsteht")
    public void runningGameForTwo(String hostEmail, String guestEmail) {
        guestAccountEmail = guestEmail;
        GameSetup d = GameSetup.defaults();
        GameSetup setup = new GameSetup(d.systemCount(), 2, 1, List.of(4, 4, 4), d.neutralMinProduction(),
                d.neutralMaxProduction(), d.startGarrison(), d.observersAllowed(), d.reentryAllowed(),
                d.seatColorHexes(), d.productionDistribution(), d.galaxyLayout(), d.battlePresentationEnabled(),
                d.roundRules());
        String host = accountId(hostEmail);
        String guest = accountId(guestEmail);
        id = games.newGame(setup, host, "Rundentest");
        assertThat(games.changeVisibility(id, host, GameVisibility.PUBLIC)).isTrue();
        assertThat(games.joinGame(id, host, "Runden Host", "Host-Reich")).isPresent();
        assertThat(games.joinGame(id, guest, "Runden Gast", "Gast-Reich")).isPresent();
        assertThat(games.startGame(id)).isTrue();
    }

    /** Die laufende Partie stünde sonst in der Lobby der folgenden Szenarien obenan. */
    @After
    public void abortGame() {
        if (id != null) {
            games.abortGame(id);
        }
        if (guestSession != null) {
            guestSession.closeSession();
        }
    }

    @Wenn("ich die Partie öffne")
    public void openGame() {
        browser.open("/map/" + id.value());
        browser.awaitCss(".round-status");
        if (guestSession == null) {
            guestSession = browser.newSession();
            guestSession.logIn(guestAccountEmail);
            guestSession.open("/map/" + id.value());
            guestSession.awaitCss(".round-status");
        }
    }

    @Dann("kann ich meine Runde abgeben")
    public void submissionIsAvailable() {
        assertSubmissionAvailable(browser);
    }

    @Dann("ist meine Runde abgegeben und wartet auf {string}")
    public void submittedAndWaiting(String player) {
        assertWaiting(browser, player);
        assertWaiting(guestSession, player);
        browser.driver().navigate().refresh();
        assertWaiting(browser, player);
    }

    @Dann("sehen beide Sitzungen die nächste Runde mit freigegebener Abgabe")
    public void nextRoundIsAvailableInBothSessions() {
        for (Browser session : List.of(browser, guestSession)) {
            session.awaitTextIn(".app-header-round", "Runde 2");
            assertSubmissionAvailable(session);
            assertThat(session.all(".round-status-waiting")).isEmpty();
            assertThat(session.all(".round-status-seat.submitted")).isEmpty();
        }
    }

    private static void assertSubmissionAvailable(Browser session) {
        session.awaitTextIn(".round-submit", "Runde abgeben");
        new WebDriverWait(session.driver(), Duration.ofSeconds(10))
                .until(_ -> session.awaitCss(".round-submit").getDomAttribute("disabled") == null);
        assertThat(session.awaitCss(".round-submit").getDomAttribute("disabled")).isNull();
    }

    private static void assertWaiting(Browser session, String player) {
        session.awaitTextIn(".round-status-waiting", "Abgegeben – warte auf " + player);
        assertThat(session.awaitCss(".round-submit").getText()).isEqualTo("Abgegeben");
        assertThat(session.awaitCss(".round-submit").getDomAttribute("disabled")).isNotNull();
    }

    @Dann("zeigt die Rundenleiste {int} Spieler, davon {int} mit Haken")
    public void seatsAndSubmissions(int seats, int submitted) {
        browser.awaitAtLeast(".round-status-seat", seats);
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10))
                .until(_ -> browser.all(".round-status-seat.submitted").size() == submitted);
        assertThat(browser.all(".round-status-seat")).hasSize(seats);
    }

    @Dann("läuft die Uhr bis zum Rundenende")
    public void roundClockRuns() {
        browser.awaitCss(".round-status-ends .round-status-clock");
        assertThat(browser.textsOf(".round-status-ends .round-status-clock")).singleElement().matches(t -> t.matches(CLOCK));
    }

    @Dann("läuft die Nachzügler-Uhr bei {string}")
    public void stragglerClockRuns(String label) {
        browser.awaitTextIn(".round-status-seat.straggler", label);
        assertThat(browser.textsOf(".round-status-seat.straggler .round-status-clock"))
                .singleElement().matches(t -> t.matches(CLOCK));
        assertThat(browser.all(".round-status-ends")).as("statt Rundenende steht die Uhr beim Nachzügler").isEmpty();
    }
}
