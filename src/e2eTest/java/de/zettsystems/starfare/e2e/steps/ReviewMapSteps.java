package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.e2e.ReviewGameFixture;
import de.zettsystems.starfare.game.application.GameRegistry;
import de.zettsystems.starfare.game.application.GameService;
import io.cucumber.java.After;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** View regression journey for logistics, amount synchronization and map selection. */
public class ReviewMapSteps {
    private final Browser browser;
    private final ReviewGameFixture fixture;
    private final GameRegistry registry;
    private final GameService games;
    private ReviewGameFixture.Game first;
    private ReviewGameFixture.Game second;

    public ReviewMapSteps(Browser browser, ReviewGameFixture fixture, GameRegistry registry, GameService games) {
        this.browser = browser;
        this.fixture = fixture;
        this.registry = registry;
        this.games = games;
    }

    @Wenn("zwei deterministische Karten für {string} bereitstehen")
    public void prepareMaps(String email) {
        first = fixture.create(email, null);
        second = fixture.create(email, null);
        registry.writeState(first.id(), state -> { state.addFleet(first.hostSeat(), 1, 2, 2); return null; });
        open(first);
    }

    @Dann("kann ich Verlegungen über der Garnison planen und den Mengenmodus wechseln")
    public void relocationAmount() {
        clickSystem("Alpha");
        browser.clickButtonWithText("Verlegung anlegen");
        browser.awaitText("Ziel auf der Karte auswählen");
        clickSystem("Beta");
        browser.awaitCss(".send-fleet-dialog[opened]");
        WebElement amount = browser.awaitCss(".send-fleet-amount input");
        amount.sendKeys(Keys.chord(Keys.CONTROL, "a"), "20", Keys.TAB);
        awaitAmount(20);
        assertMax(20);
        browser.clickButtonWithText("Hälfte");
        awaitAmount(10);
        browser.clickButtonWithText("Doppelt");
        awaitAmount(20);
        browser.clickCss(".send-fleet-dialog vaadin-checkbox");
        awaitAmount(5);
        assertMax(5);
        browser.clickButtonWithText("Alle");
        awaitAmount(5);
        browser.clickCss(".send-fleet-dialog vaadin-checkbox");
        assertMax(20);
        browser.clickButtonWithText("Alle");
        awaitAmount(20);
        browser.clickButtonWithText("Verlegung speichern");
        browser.awaitCss(".standing-order-badge");
        assertThat(games.viewFor(first.id(), first.hostSeat()).standingOrders()).singleElement()
                .satisfies(order -> assertThat(order.ships()).isEqualTo(20));
    }

    @Dann("zeigt die Logistik die gespeicherte Verlegung")
    public void logisticsVisible() {
        browser.clickTabWithText("Logistik");
        browser.awaitCss(".logistics-system-card");
        browser.awaitTextIn(".logistics-route-card", "20");
        assertThat(browser.all(".logistics-route-card")).anyMatch(WebElement::isDisplayed);
    }

    @Dann("zeigt eine Flottenauswahl nach einer Systemauswahl die Flottendetails")
    public void fleetSelectionReplacesSystemDetails() {
        clickSystem("Alpha");
        browser.awaitCss(".system-metric");
        browser.clickTabWithText("Flotten");
        browser.clickCss(".fleet-travel-card");
        browser.awaitSelectedTabWithText("Details");
        browser.awaitTextIn(".map-right", "Unterwegs seit Runde");
        assertThat(browser.all(".system-metric")).noneMatch(WebElement::isDisplayed);
        clickSystem("Alpha");
        browser.awaitCss(".system-metric");
        browser.clickCss(".fleet-badge");
        browser.awaitTextIn(".map-right", "Unterwegs seit Runde");
        assertThat(browser.all(".system-metric")).noneMatch(WebElement::isDisplayed);
    }

    @Dann("bleiben Zoom und Kartenposition für beide Partien getrennt")
    public void independentViewports() {
        storeViewport(first, 1.4, 1100);
        open(second);
        assertThat(js().executeScript("return document.querySelector('#map').style.zoom")).isEqualTo("1");
        storeViewport(second, 0.8, 600);
        open(first);
        assertViewport(1.4, 1100);
        open(second);
        assertViewport(0.8, 600);
    }

    private void storeViewport(ReviewGameFixture.Game game, double zoom, int left) {
        String key = "starfare.viewport." + game.id().value();
        assertThat(js().executeScript("return document.querySelector('#scroll').__starfareViewportKey")).isEqualTo(key);
        js().executeScript("""
                const el = document.querySelector('#scroll');
                el.querySelector('#map').style.zoom = String(arguments[0]);
                el.scrollLeft = arguments[1];
                el.dispatchEvent(new Event('scroll'));
                """, zoom, left);
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> Boolean.TRUE.equals(
                js().executeScript("""
                        const saved = JSON.parse(sessionStorage.getItem(arguments[0]) || 'null');
                        return saved?.zoom === arguments[1] && saved?.left === arguments[2];
                        """, key, zoom, left)));
    }

    private void assertViewport(double zoom, int left) {
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> Boolean.TRUE.equals(
                js().executeScript("""
                        const el = document.querySelector('#scroll');
                        return Number(el.querySelector('#map').style.zoom) === arguments[0]
                            && Math.abs(el.scrollLeft - arguments[1]) < 2;
                        """, zoom, left)));
        assertThat(js().executeScript("return document.querySelector('#scroll').scrollLeft"))
                .isInstanceOf(Number.class);
    }

    private void open(ReviewGameFixture.Game game) {
        browser.open("/map/" + game.id().value());
        browser.awaitCss(".map-content");
    }

    private void clickSystem(String name) {
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> Boolean.TRUE.equals(
                js().executeScript("""
                        const system = [...document.querySelectorAll('#map .sys-own, #map .sys-fog')]
                            .find(element => element.textContent.startsWith(arguments[0]));
                        if (!system) return false;
                        system.click();
                        return true;
                        """, name)));
    }

    private void awaitAmount(int value) {
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                String.valueOf(value).equals(browser.awaitCss(".send-fleet-amount input").getDomProperty("value")));
        assertThat(browser.awaitCss(".send-fleet-slider").getDomProperty("value")).isEqualTo(String.valueOf(value));
    }

    private void assertMax(int max) {
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                String.valueOf(max).equals(browser.awaitCss(".send-fleet-slider").getDomAttribute("max")));
        assertThat(browser.awaitCss(".send-fleet-slider").getDomAttribute("max")).isEqualTo(String.valueOf(max));
    }

    private JavascriptExecutor js() {
        return (JavascriptExecutor) browser.driver();
    }

    @After
    public void cleanupGames() {
        if (first != null) { games.abortGame(first.id()); }
        if (second != null) { games.abortGame(second.id()); }
    }
}
