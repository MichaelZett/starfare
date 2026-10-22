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
import org.jspecify.annotations.Nullable;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Das Handy-Szenario: Ein emuliertes iPhone bekommt die eigene Oberfläche, der Desktop daneben
 * die gewohnte. Jeder Fingertipp prüft, dass an der Stelle wirklich der Knopf liegt — ein
 * verdeckter Knopf fiele bei einem Skript-Klick nicht auf.
 */
public class MobileSteps {

    private static final Duration WAIT = Duration.ofSeconds(30);

    private final Browser browser;
    private final GameService games;
    private final UserAccountService accounts;
    private @Nullable Browser phone;
    private @Nullable GameId id;
    private @Nullable String guestEmail;

    public MobileSteps(Browser browser, GameService games, UserAccountService accounts) {
        this.browser = browser;
        this.games = games;
        this.accounts = accounts;
    }

    @After
    public void cleanUp() {
        if (id != null) {
            games.abortGame(id);
        }
        if (phone != null) {
            phone.closeSession();
        }
    }

    @Wenn("eine laufende Handy-Partie für {string} und {string} bereitsteht")
    public void runningGame(String hostEmail, String guestEmail) {
        this.guestEmail = guestEmail;
        GameSetup d = GameSetup.defaults();
        GameSetup setup = new GameSetup(d.systemCount(), 2, 1, List.of(4, 4, 4), d.neutralMinProduction(),
                d.neutralMaxProduction(), d.startGarrison(), d.observersAllowed(), d.reentryAllowed(),
                d.seatColorHexes(), d.productionDistribution(), d.galaxyLayout(), d.battlePresentationEnabled(),
                d.roundRules());
        String host = accountId(hostEmail);
        id = games.newGame(setup, host, "Handytest");
        assertThat(games.changeVisibility(id, host, GameVisibility.PUBLIC)).isTrue();
        assertThat(games.joinGame(id, host, "Handy Host", "Handy-Reich")).isPresent();
        assertThat(games.joinGame(id, accountId(guestEmail), "Handy Gast", "Gast-Reich")).isPresent();
        assertThat(games.startGame(id)).isTrue();
    }

    @Wenn("ich mich auf dem Handy mit {string} anmelde")
    public void logInOnPhone(String email) {
        phone = browser.newPhoneSession();
        phone().logIn(email);
        phone().awaitCss(".lobby-root");
    }

    @Dann("lässt sich der Spielassistent auf dem Handy bis zur Fußleiste bedienen")
    public void wizardIsUsable() {
        assertPhoneLayout();
        tap("Neues Spiel");
        phone().awaitCss("vaadin-dialog[opened]");
        tap("Zusammenfassung prüfen");
        assertTappable(button("Spiel anlegen"));
        tap("Abbrechen");
        phone().awaitNoneDisplayed("vaadin-dialog[opened]");
    }

    @Wenn("ich die Partie auf dem Handy öffne")
    public void openGameOnPhone() {
        phone().open("/map/" + game().value());
        phone().awaitCss(".mobile-map .round-status");
    }

    @Dann("zeigt das Handy die eigene Kartenansicht und der Desktop die geteilte Ansicht")
    public void layoutsDifferPerDevice() {
        assertPhoneLayout();
        assertThat(phone().all(".map-content")).as("Handy baut keine Desktop-Ansicht").isEmpty();
        assertThat(phone().all(".app-header-title")).as("Kompakte Kopfzeile ohne Titel").isEmpty();
        assertThat(phone().all(".mobile-map-nav vaadin-button")).hasSize(2);

        browser.logIn(Objects.requireNonNull(guestEmail));
        browser.open("/map/" + game().value());
        browser.awaitCss(".map-content");
        assertThat(browser.all(".mobile-map")).as("Desktop baut keine Handy-Ansicht").isEmpty();
        assertThat(mobileStylesheetLoaded(browser)).as("Desktop lädt das Handy-Stylesheet nicht").isFalse();
    }

    @Wenn("ich auf dem Handy mein Heimatsystem antippe")
    public void tapHomeSystem() {
        WebElement home = new WebDriverWait(phone().driver(), WAIT).ignoring(StaleElementReferenceException.class)
                .until(_ -> phone().all(".sys-own").stream().filter(WebElement::isDisplayed).findFirst().orElse(null));
        assertTappable(home);
        home.click();
    }

    @Dann("öffnet sich auf dem Handy die Übersicht mit den Systemdetails")
    public void sheetShowsDetails() {
        phone().awaitCss(".mobile-map-sheet .system-details-heading");
        phone().awaitSelectedTabWithText("Details");
        phone().assertNoSidewaysScrolling();
    }

    @Wenn("ich auf dem Handy „Flotte senden“ wähle")
    public void chooseSendFleet() {
        tap("Flotte senden");
    }

    @Dann("zeigt die Handy-Karte den Zielhinweis und die Zielwahl lässt sich abbrechen")
    public void targetBannerCancels() {
        phone().awaitCss(".mobile-map-target");
        phone().awaitNoneDisplayed(".mobile-map-sheet");
        tap("Zielwahl abbrechen");
        phone().awaitNoneDisplayed(".mobile-map-target");
    }

    @Dann("kann ich auf dem Handy die Runde abgeben")
    public void submitRoundOnPhone() {
        tap("Runde abgeben");
        phone().awaitTextIn(".round-submit", "Abgegeben");
    }

    private void assertPhoneLayout() {
        new WebDriverWait(phone().driver(), WAIT).withMessage("Handy-Stylesheet nicht geladen")
                .until(_ -> mobileStylesheetLoaded(phone()));
        phone().assertNoSidewaysScrolling();
    }

    private static boolean mobileStylesheetLoaded(Browser session) {
        return Boolean.TRUE.equals(((JavascriptExecutor) session.driver()).executeScript(
                "return document.documentElement.classList.contains('starfare-mobile')"
                        + " && [...document.styleSheets].some(s => (s.href || '').includes('starfare-mobile.css'));"));
    }

    private void tap(String label) {
        new WebDriverWait(phone().driver(), WAIT).ignoring(StaleElementReferenceException.class)
                .withMessage("„" + label + "“ lässt sich auf dem Handy nicht antippen")
                .until(_ -> {
                    WebElement button = button(label);
                    if (button == null || !tappable(button)) {
                        return false;
                    }
                    button.click();
                    return true;
                });
    }

    private void assertTappable(@Nullable WebElement element) {
        new WebDriverWait(phone().driver(), WAIT).ignoring(StaleElementReferenceException.class)
                .withMessage("Element liegt auf dem Handy verdeckt oder außerhalb")
                .until(_ -> element != null && tappable(element));
    }

    /** Was der Finger an der Mitte des Elements träfe. */
    private boolean tappable(WebElement element) {
        return Boolean.TRUE.equals(((JavascriptExecutor) phone().driver()).executeScript("""
                const target = arguments[0];
                target.scrollIntoView({ block: 'center', inline: 'center' });
                const rect = target.getBoundingClientRect();
                if (rect.width === 0 || rect.height === 0) return false;
                const hit = document.elementFromPoint(rect.left + rect.width / 2, rect.top + rect.height / 2);
                return hit !== null && (hit === target || target.contains(hit));
                """, element));
    }

    private @Nullable WebElement button(String label) {
        return phone().driver().findElements(By.tagName("vaadin-button")).stream()
                .filter(WebElement::isDisplayed)
                .filter(candidate -> label.equals(candidate.getText().strip()))
                .findFirst().orElse(null);
    }

    private String accountId(String email) {
        return String.valueOf(accounts.findByEmail(email).orElseThrow().id());
    }

    private Browser phone() {
        return Objects.requireNonNull(phone, "Keine Handy-Sitzung");
    }

    private GameId game() {
        return Objects.requireNonNull(id, "Keine Partie");
    }
}
