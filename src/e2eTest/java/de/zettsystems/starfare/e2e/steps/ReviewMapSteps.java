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
    private final de.zettsystems.starfare.game.application.Broadcaster broadcaster;
    private ReviewGameFixture.Game first;
    private ReviewGameFixture.Game second;
    private de.zettsystems.starfare.game.values.GameId copiedGame;

    @Dann("kann ich Reserven und Flotten gesammelt planen und Befehle bearbeiten")
    public void bulkPlanning() {
        browser.clickButtonWithText("Reserven gesammelt setzen");
        selectSources();
        WebElement amount = browser.awaitCss(".planning-dialog vaadin-integer-field input");
        amount.sendKeys(Keys.chord(Keys.CONTROL, "a"), "1", Keys.TAB);
        browser.clickButtonWithText("Speichern");
        awaitPlanningClosed();
        assertThat(games.viewFor(first.id(), first.hostSeat()).systems().stream()
                .filter(s -> s.id() == 1 || s.id() == 2)).allMatch(s -> Integer.valueOf(1).equals(s.garrisonReserve()));
        browser.clickButtonWithText("Flotten gesammelt senden");
        selectSources();
        selectTarget("Gamma");
        browser.awaitTextIn(".planning-dialog", "Schiffe");
        browser.all(".planning-dialog vaadin-button").stream().filter(button -> button.getText().equals("Flotten gesammelt senden"))
                .findFirst().orElseThrow().click();
        awaitPlanningClosed();
        browser.awaitSelectedTabWithText("Befehle");
        assertThat(games.viewFor(first.id(), first.hostSeat()).plannedOrders()).hasSize(2);
        browser.all(".fleet-travel-card-planned").stream().filter(card -> card.getText().contains("Alpha → Gamma"))
                .findFirst().orElseThrow().findElements(org.openqa.selenium.By.tagName("vaadin-button")).stream()
                .filter(button -> button.getText().equals("Befehl bearbeiten")).findFirst().orElseThrow().click();
        selectTarget("Beta");
        amount = browser.awaitCss(".planning-dialog vaadin-integer-field input");
        amount.sendKeys(Keys.chord(Keys.CONTROL, "a"), "2", Keys.TAB);
        browser.clickButtonWithText("Speichern");
        awaitPlanningClosed();
        browser.awaitTextIn(".fleet-travel-card-planned", "Beta");
        assertThat(games.viewFor(first.id(), first.hostSeat()).plannedOrders().getFirst().ships()).isEqualTo(2);
        browser.clickButtonWithText("Letzten Befehlschritt zurücknehmen");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                Integer.valueOf(4).equals(games.viewFor(first.id(), first.hostSeat()).plannedOrders().getFirst().ships()));
        browser.awaitTextIn(".fleet-travel-card-planned", "Gamma");
        assertThat(games.viewFor(first.id(), first.hostSeat()).plannedOrders().getFirst().ships()).isEqualTo(4);
        browser.clickTabWithText("Flotten");
        browser.awaitTextIn(".map-right", "Gemeinsame Ankünfte");
    }

    @Dann("bleibt mein Routenfilter nach einer neuen Anmeldung erhalten und kann ich eine Revanche anlegen")
    public void persistentFilterAndTemplate() {
        WebElement filter = visibleRouteFilter();
        filter.sendKeys("Gamma", Keys.TAB);
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                browser.all(".fleet-travel-card").stream().noneMatch(WebElement::isDisplayed));
        browser.resetSession();
        browser.logIn("comfort@example.test");
        open(first);
        browser.clickTabWithText("Flotten");
        assertThat(visibleRouteFilter().getDomProperty("value")).isEqualTo("Gamma");
        var before = registry.listIds();
        browser.open("/");
        browser.clickButtonWithText("Als Vorlage / Revanche");
        browser.awaitCss("vaadin-dialog[opened] vaadin-checkbox").click();
        browser.all("vaadin-dialog[opened] vaadin-button").stream().filter(button -> button.getText().equals("Neues Spiel"))
                .findFirst().orElseThrow().click();
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> registry.listIds().size() > before.size());
        copiedGame = registry.listIds().stream().filter(id -> !before.contains(id)).findFirst().orElseThrow();
        assertThat(registry.readState(copiedGame, state -> state.invitedSeats().size()).intValue()).isEqualTo(1);
        assertThat(registry.readState(copiedGame, state -> state.started()).booleanValue()).isFalse();
    }

    private WebElement visibleRouteFilter() {
        return new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                browser.all(".map-right vaadin-text-field input").stream().filter(WebElement::isDisplayed)
                        .findFirst().orElse(null));
    }

    private void selectSources() {
        WebElement input = browser.awaitCss(".planning-dialog vaadin-multi-select-combo-box input");
        input.sendKeys("Alpha");
        selectComboItem("vaadin-multi-select-combo-box-item", "Alpha");
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"), "Beta");
        selectComboItem("vaadin-multi-select-combo-box-item", "Beta");
        input.sendKeys(Keys.TAB);
    }

    private void selectTarget(String name) {
        WebElement target = browser.awaitCss(".planning-dialog vaadin-combo-box input");
        target.sendKeys(Keys.chord(Keys.CONTROL, "a"), name);
        selectComboItem("vaadin-combo-box-item", name);
        target.sendKeys(Keys.TAB);
    }

    private void selectComboItem(String selector, String name) {
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10))
                .ignoring(org.openqa.selenium.StaleElementReferenceException.class)
                .until(_ -> browser.all(selector).stream().filter(WebElement::isDisplayed)
                        .filter(item -> item.getText().strip().equals(name)).findFirst().orElse(null)).click();
    }

    private void awaitPlanningClosed() {
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                browser.all(".planning-dialog[opened]").isEmpty());
    }

    public ReviewMapSteps(Browser browser, ReviewGameFixture fixture, GameRegistry registry, GameService games,
                          de.zettsystems.starfare.game.application.Broadcaster broadcaster) {
        this.browser = browser;
        this.fixture = fixture;
        this.registry = registry;
        this.games = games;
        this.broadcaster = broadcaster;
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

    @Dann("bleiben Eingaben bei Aktualisierungen erhalten und ist die Karte per Tastatur bedienbar")
    public void stableInputsAndKeyboard() {
        open(first);
        WebElement system = browser.awaitCss("#map .sys-own");
        system.sendKeys(Keys.ENTER);
        browser.awaitCss(".system-metric");
        WebElement reserve = browser.awaitCss(".map-right vaadin-integer-field input");
        reserve.sendKeys(Keys.chord(Keys.CONTROL, "a"), "3");
        broadcaster.publish(new de.zettsystems.starfare.game.application.GameEvent.PlayerSubmitted(first.id(), 99));
        browser.clickButtonWithText("Vergrößern");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                Double.parseDouble(String.valueOf(js().executeScript("return document.querySelector('#map').style.zoom"))) > 1);
        assertThat(reserve.getDomProperty("value")).isEqualTo("3");
        browser.clickTabWithText("Flotten");
        WebElement filter = visibleRouteFilter();
        filter.sendKeys("Alpha");
        broadcaster.publish(new de.zettsystems.starfare.game.application.GameEvent.PlayerSubmitted(first.id(), 99));
        assertThat(filter.getDomProperty("value")).isEqualTo("Alpha");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10))
                .ignoring(org.openqa.selenium.StaleElementReferenceException.class).until(_ -> {
                    browser.awaitCss(".fleet-travel-card").sendKeys(Keys.SPACE);
                    return true;
                });
        browser.awaitSelectedTabWithText("Details");
        browser.awaitTextIn(".map-right", "Unterwegs seit Runde");
        browser.clickButtonWithText("Ganze Galaxie");
        assertThat(browser.all(".map-left .map-controls")).hasSize(1);
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
        if (copiedGame != null) { games.abortGame(copiedGame); }
        if (first != null) { games.abortGame(first.id()); }
        if (second != null) { games.abortGame(second.id()); }
    }
}
