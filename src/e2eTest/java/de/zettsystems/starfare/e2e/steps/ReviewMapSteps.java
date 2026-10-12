package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.starfare.game.domain.GameState;
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

    @Dann("bleiben Systeme zwischen dreißig Flotten bei verschiedenen Zoomstufen erreichbar")
    public void crowdedMap() throws java.io.IOException {
        registry.writeState(first.id(), state -> {
            state.updateSystem(3, system -> system.relocateTo(600, 350));
            for (int system = 9; system <= 120; system++) {
                state.systems().add(new de.zettsystems.starfare.game.values.StarSystem(system, "Distant " + system,
                        2000 + system % 10 * 100, 900 + system / 10 * 50, null, 2, 1, true));
            }
            for (int index = 0; index < 30; index++) {
                state.addFleet(first.hostSeat(), index % 2 == 0 ? 1 : 2, index % 2 == 0 ? 2 : 1, 1);
            }
            return null;
        });
        browser.driver().navigate().refresh();
        browser.awaitCss("#map .fleet-lane");
        assertThat(games.viewFor(first.id(), first.hostSeat()).systems()).hasSize(120);
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> browser.all("#map .fleet-lane-hit").size() >= 30);
        for (int width : new int[]{1366, 1024}) {
            browser.driver().manage().window().setSize(new org.openqa.selenium.Dimension(width, 768));
            for (double zoom : new double[]{0.4, 1, 2.5}) {
                js().executeScript("""
                        const map = document.querySelector('#map'), scroll = document.querySelector('#scroll');
                        map.style.zoom = arguments[0];
                        scroll.scrollLeft = 600 * arguments[0] - scroll.clientWidth / 2;
                        scroll.scrollTop = 350 * arguments[0] - scroll.clientHeight / 2;
                        """, zoom);
                new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> Boolean.TRUE.equals(js().executeScript("""
                        const map = document.querySelector('#map');
                        return Math.abs(Number(map.style.getPropertyValue('--map-inverse-zoom')) - 1 / Number(map.style.zoom)) < .001;
                        """)));
                assertThat(js().executeScript("""
                        const scroll = document.querySelector('#scroll').getBoundingClientRect();
                        return [...document.querySelectorAll('#map .sys-own, #map .sys-fog')]
                            .filter(dot => ['Alpha','Beta','Gamma'].some(name => dot.textContent.startsWith(name)))
                            .every(dot => {
                                const r = dot.getBoundingClientRect(), x = r.x + r.width / 2, y = r.y + r.height / 2;
                                if (x < scroll.left || x > scroll.right) return true;
                                return document.elementFromPoint(x,y)?.closest('.sys-own, .sys-fog') === dot;
                            });
                        """)).as("system centers at width %s and zoom %s", width, zoom).isEqualTo(true);
            }
        }
        js().executeScript("document.querySelector('#map').style.zoom='1';");
        browser.clickButtonWithText("Eigenes Reich");
        browser.captureUiAcceptance("crowded-map");
        var freePoint = (java.util.List<?>) js().executeScript("""
                const map = document.querySelector('#map'), z = Number(map.style.zoom || 1);
                const bounds = map.getBoundingClientRect(), scroll = document.querySelector('#scroll').getBoundingClientRect();
                for (const lane of map.querySelectorAll('.fleet-lane-hit')) {
                    const angle = Number(lane.dataset.mapAngle);
                    for (let p = .15; p < .9; p += .05) {
                        const d = Number(lane.dataset.mapLength) * p;
                        const x = bounds.left + (parseFloat(lane.style.left) + Math.cos(angle) * d) * z;
                        const y = bounds.top + (parseFloat(lane.style.top) + parseFloat(lane.style.height) / 2 + Math.sin(angle) * d) * z;
                        if (x < scroll.left || x > scroll.right || y < scroll.top || y > scroll.bottom) continue;
                        const hit = document.elementFromPoint(x,y);
                        if (hit?.classList.contains('fleet-lane-hit')) return [x,y];
                    }
                }
                return [];
                """);
        assertThat(freePoint).hasSize(2);
        new org.openqa.selenium.interactions.Actions(browser.driver()).moveToLocation(
                ((Number) freePoint.get(0)).intValue(), ((Number) freePoint.get(1)).intValue()).click().perform();
        browser.awaitCss(".fleet-lane-highlighted");
        browser.awaitSelectedTabWithText("Details");
        WebElement beta = mapSystem("Beta");
        new org.openqa.selenium.interactions.Actions(browser.driver()).contextClick(beta).perform();
        browser.awaitCss(".sys-selected");
        assertThat(browser.all("vaadin-context-menu-overlay[opened]")).isEmpty();
    }

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
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10))
                .ignoring(org.openqa.selenium.StaleElementReferenceException.class).until(_ ->
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
        assertThat(registry.readState(copiedGame, GameState::started).booleanValue()).isFalse();
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

    @Dann("bleibt der Sendekontext bei Abbruch und Erfolg erhalten und sind Flottenaktionen direkt bedienbar")
    public void fleetContext() {
        clickSystem("Alpha");
        browser.clickButtonWithText("Flotte senden");
        browser.awaitText("Ziel auf der Karte ausw\u00e4hlen");
        browser.awaitCss(".sys-selected");
        clickSystem("Beta");
        browser.awaitCss(".send-fleet-dialog[opened]");
        browser.clickButtonWithText("Abbrechen");
        awaitSendClosed();
        assertSourceSelected();
        assertThat(games.viewFor(first.id(), first.hostSeat()).plannedOrders()).isEmpty();
        clickSystem("Beta");
        browser.awaitCss(".send-fleet-amount input").sendKeys(Keys.ESCAPE);
        awaitSendClosed();
        assertSourceSelected();
        clickSystem("Beta");
        browser.awaitCss(".send-fleet-dialog[opened]");
        new org.openqa.selenium.interactions.Actions(browser.driver()).moveToLocation(10, 100).click().perform();
        awaitSendClosed();
        assertSourceSelected();
        clickSystem("Beta");
        browser.awaitCss(".send-fleet-dialog[opened]");
        browser.all(".send-fleet-dialog[opened] vaadin-button").stream()
                .filter(button -> button.getText().equals("Flotte senden")).findFirst().orElseThrow().click();
        awaitSendClosed();
        browser.awaitSelectedTabWithText("Details");
        browser.awaitTextIn(".system-details-heading", "Alpha");
        browser.clickButtonWithText("Flotte senden");
        browser.awaitText("Ziel auf der Karte ausw\u00e4hlen");
        browser.awaitCss(".sys-selected");
        clickSystem("Beta");
        browser.awaitCss(".send-fleet-dialog[opened]");
        browser.all(".send-fleet-dialog[opened] vaadin-button").stream()
                .filter(button -> button.getText().equals("Flotte senden")).findFirst().orElseThrow().click();
        awaitSendClosed();
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                games.viewFor(first.id(), first.hostSeat()).plannedOrders().size() == 2);
        browser.awaitSelectedTabWithText("Details");
        assertThat(games.viewFor(first.id(), first.hostSeat()).plannedOrders()).hasSize(2);
        checkQueuedAndFleetActions();
    }

    private void checkQueuedAndFleetActions() {
        browser.clickTabWithText("Befehle");
        browser.clickButtonWithText("Stornieren");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                games.viewFor(first.id(), first.hostSeat()).plannedOrders().size() == 1);
        browser.clickButtonWithText("Letzten Befehlschritt zur\u00fccknehmen");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                games.viewFor(first.id(), first.hostSeat()).plannedOrders().size() == 2);
        browser.clickTabWithText("Flotten");
        browser.awaitCss(".fleet-direct-actions vaadin-button").click();
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                !games.viewFor(first.id(), first.hostSeat()).waitingFleetIds().isEmpty());
        browser.awaitSelectedTabWithText("Flotten");
        assertThat(games.viewFor(first.id(), first.hostSeat()).waitingFleetIds()).hasSize(1);
        browser.clickCss(".fleet-travel-card");
        browser.awaitSelectedTabWithText("Details");
        browser.awaitTextIn(".fleet-direct-actions", "Wartet");
        assertThat(browser.awaitCss(".fleet-direct-actions vaadin-button").getDomAttribute("disabled")).isNotNull();
        browser.clickButtonWithText("Aufl\u00f6sen");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                games.viewFor(first.id(), first.hostSeat()).plannedOrders().size() == 4);
        browser.awaitSelectedTabWithText("Details");
        assertThat(games.viewFor(first.id(), first.hostSeat()).ownFleets()).hasSize(1);
        browser.clickButtonWithText("Letzten Befehlschritt zur\u00fccknehmen");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                games.viewFor(first.id(), first.hostSeat()).plannedOrders().size() == 3);
    }

    private void awaitSendClosed() {
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                browser.all(".send-fleet-dialog[opened]").isEmpty());
    }

    private void assertSourceSelected() {
        browser.awaitSelectedTabWithText("Details");
        browser.awaitTextIn(".system-details-heading", "Alpha");
        assertThat(browser.awaitCss(".sys-selected").getText()).contains("Alpha");
    }

    @Dann("kann ich eine Verlegung per Ziehen erstellen und an ihrer Reisekarte bearbeiten")
    public void relocationDrop() {
        browser.clickButtonWithText("Eigenes Reich");
        dragRelocation();
        browser.clickButtonWithText("Verlegung speichern");
        awaitSendClosed();
        browser.clickTabWithText("Verlegungen");
        browser.clickButtonWithText("Bearbeiten");
        browser.awaitCss(".send-fleet-amount input").sendKeys(Keys.chord(Keys.CONTROL, "a"), "2", Keys.TAB);
        browser.clickButtonWithText("Verlegung speichern");
        awaitSendClosed();
        browser.awaitSelectedTabWithText("Verlegungen");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                games.viewFor(first.id(), first.hostSeat()).standingOrders().getFirst().ships() == 2);
        assertThat(games.viewFor(first.id(), first.hostSeat()).standingOrders()).singleElement()
                .satisfies(order -> assertThat(order.ships()).isEqualTo(2));
    }

    private void dragRelocation() {
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> Boolean.TRUE.equals(js().executeScript("""
                const viewport = document.querySelector('#scroll').getBoundingClientRect();
                return [...document.querySelectorAll('#map .sys-own')].every(system => {
                    const r = system.getBoundingClientRect();
                    return r.left > viewport.left && r.right < viewport.right
                        && r.top > viewport.top && r.bottom < viewport.bottom && system.draggable;
                });
                """)));
        WebElement source = mapSystem("Alpha");
        WebElement target = mapSystem("Beta");
        var points = (java.util.List<?>) js().executeScript("""
                window.__dragTrace = [];
                for (const type of ['dragstart', 'dragover', 'drop', 'dragend']) {
                    document.addEventListener(type, event => {
                        window.__dragTrace.push(type + ':' + event.target.className);
                    }, {capture:true});
                }
                const a = arguments[0].getBoundingClientRect();
                const b = arguments[1].getBoundingClientRect();
                return [a.x + a.width / 2, a.y + a.height / 4, b.x + b.width / 2, b.y + b.height / 4];
                """, source, target);
        new org.openqa.selenium.interactions.Actions(browser.driver())
                .moveToLocation(((Number) points.get(0)).intValue(), ((Number) points.get(1)).intValue())
                .clickAndHold().moveByOffset(8, 0).pause(Duration.ofMillis(300))
                .moveToLocation(((Number) points.get(2)).intValue(), ((Number) points.get(3)).intValue())
                .pause(Duration.ofMillis(300)).moveByOffset(1, 0)
                .pause(Duration.ofMillis(200)).release().perform();
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10))
                .withMessage(() -> String.valueOf(js().executeScript("return JSON.stringify(window.__dragTrace)")))
                .until(_ -> !browser.all(".send-fleet-dialog[opened]").isEmpty());
    }

    private WebElement mapSystem(String name) {
        return new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                browser.all("#map .sys-own").stream().filter(system -> system.getText().startsWith(name))
                        .findFirst().orElse(null));
    }

    @Dann("zeigt die Logistik die gespeicherte Verlegung")
    public void logisticsVisible() {
        browser.clickTabWithText("Logistik");
        browser.awaitCss(".logistics-system-card");
        browser.awaitTextIn(".logistics-route-card", "20");
        assertThat(browser.all(".logistics-route-card")).anyMatch(WebElement::isDisplayed);
    }

    @Dann("zeigt die Systemsicht direkte Aktionen und behält die geöffneten Detailbereiche")
    public void systemDetails() throws java.io.IOException {
        assertThat(browser.awaitCss(".app-header-empire").getText()).isEqualTo("Host Empire");
        checkSystemActions("de", "Versendbare Schiffe");
        switchLanguage("Englisch", "English");
        checkSystemActions("en", "Sendable ships");
        switchLanguage("German", "Deutsch");
        clickSystem("Alpha");
        browser.awaitCss(".system-metric-sendable");
        assertThat(js().executeScript("return document.querySelector('.system-operations').opened")).isEqualTo(false);
        assertThat(js().executeScript("return document.querySelector('.system-background').opened")).isEqualTo(false);
        openSystemDetails(".system-operations");
        openSystemDetails(".system-background");
        clickSystem("Beta");
        browser.awaitTextIn(".system-details-heading", "Beta");
        assertThat(js().executeScript("return document.querySelector('.system-operations').opened")).isEqualTo(true);
        assertThat(js().executeScript("return document.querySelector('.system-background').opened")).isEqualTo(true);
        assertThat(browser.awaitCss(".system-operations vaadin-integer-field input").getDomProperty("value"))
                .isEqualTo("0");
        WebElement reserve = browser.awaitCss(".system-operations vaadin-integer-field input");
        reserve.sendKeys(Keys.chord(Keys.CONTROL, "a"), "1", Keys.TAB);
        browser.clickButtonWithText("Reserve speichern");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                Integer.valueOf(1).equals(games.viewFor(first.id(), first.hostSeat()).systems().stream()
                        .filter(system -> system.id() == 2).findFirst().orElseThrow().garrisonReserve()));
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.stalenessOf(reserve));
        assertThat(js().executeScript("return document.querySelector('.system-operations').opened")).isEqualTo(true);
        browser.awaitCss(".system-operations vaadin-integer-field input")
                .sendKeys(Keys.chord(Keys.CONTROL, "a"), "0", Keys.TAB);
        browser.clickButtonWithText("Reserve speichern");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                Integer.valueOf(0).equals(games.viewFor(first.id(), first.hostSeat()).systems().stream()
                        .filter(system -> system.id() == 2).findFirst().orElseThrow().garrisonReserve()));
        js().executeScript("document.querySelector('.system-operations').opened = false; document.querySelector('.system-background').opened = false;");
        browser.driver().manage().window().setSize(new org.openqa.selenium.Dimension(1366, 768));
    }

    @Dann("kann ich den Zufluss weiterverlegen und die Verlegung direkt am Ziel zurücknehmen")
    public void incomingTransferAndCancellation() throws java.io.IOException {
        clickSystem("Beta");
        browser.awaitTextIn(".system-metric-rate", "2 (+20/Runde)");
        assertThat(browser.awaitCss(".system-details-owner").getText()).contains("Host Empire").doesNotContain("Review Host");
        browser.clickButtonWithText("Verlegung anlegen");
        browser.awaitText("Ziel auf der Karte auswählen");
        clickSystem("Alpha");
        browser.awaitTextIn(".send-fleet-production", "2 (+20/Runde)");
        WebElement input = browser.awaitCss(".send-fleet-amount input");
        assertMax(22);
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"), "22", Keys.TAB);
        assertThat(input.getDomProperty("value")).isEqualTo("22");
        browser.clickButtonWithText("Abbrechen");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                browser.all(".send-fleet-dialog[opened]").isEmpty());
        clickSystem("Beta");
        int fleets = games.viewFor(first.id(), first.hostSeat()).ownFleets().size();
        browser.awaitTextIn(".system-transfer-row", "Alpha → Beta");
        browser.awaitCss(".system-transfer-row vaadin-button").click();
        browser.awaitTextIn(".send-fleet-route", "Alpha");
        browser.awaitCss(".send-fleet-amount input").sendKeys(Keys.chord(Keys.CONTROL, "a"), "19", Keys.TAB);
        browser.clickButtonWithText("Abbrechen");
        awaitSendClosed();
        assertThat(games.viewFor(first.id(), first.hostSeat()).standingOrders()).singleElement()
                .satisfies(order -> assertThat(order.ships()).isEqualTo(20));
        browser.awaitCss(".system-transfer-row vaadin-button").click();
        browser.awaitCss(".send-fleet-amount input").sendKeys(Keys.chord(Keys.CONTROL, "a"), "19", Keys.TAB);
        browser.clickButtonWithText("Verlegung speichern");
        awaitSendClosed();
        assertThat(games.viewFor(first.id(), first.hostSeat()).standingOrders()).singleElement()
                .satisfies(order -> assertThat(order.ships()).isEqualTo(19));
        clickSystem("Beta");
        browser.awaitTextIn(".system-transfer-row", "19");
        browser.captureUiAcceptance("system-transfer-feedback");
        browser.all(".system-transfer-row vaadin-button").stream().filter(button -> button.getText().equals("Aufheben"))
                .findFirst().orElseThrow().click();
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                games.viewFor(first.id(), first.hostSeat()).standingOrders().isEmpty());
        browser.awaitTextIn(".system-metric-rate", "2 (+0/Runde)");
        browser.awaitSelectedTabWithText("Details");
        assertThat(games.viewFor(first.id(), first.hostSeat()).ownFleets()).hasSize(fleets);
    }

    private void openSystemDetails(String selector) {
        browser.awaitCss(selector + " > vaadin-details-summary").click();
    }

    private void switchLanguage(String option, String selected) {
        browser.awaitCss(".lang-switcher input").sendKeys(Keys.chord(Keys.CONTROL, "a"), option);
        selectComboItem("vaadin-combo-box-item", option);
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10))
                .ignoring(org.openqa.selenium.StaleElementReferenceException.class).until(_ ->
                        selected.equals(browser.awaitCss(".lang-switcher input").getDomProperty("value")));
    }

    private void checkSystemActions(String language, String sendableLabel) throws java.io.IOException {
        for (int width : new int[]{1366, 1024}) {
            browser.driver().manage().window().setSize(new org.openqa.selenium.Dimension(width, 768));
            clickSystem("Alpha");
            browser.awaitTextIn(".system-metric-sendable", sendableLabel);
            assertThat(js().executeScript("""
                    const metrics = document.querySelector('.system-metrics');
                    const actions = document.querySelector('.system-details-actions');
                    const image = document.querySelector('.system-orrery');
                    const sidebar = actions.closest('.map-right').getBoundingClientRect();
                    const buttons = [...actions.querySelectorAll('vaadin-button')];
                    return metrics.firstElementChild.classList.contains('system-metric-sendable')
                        && actions.getBoundingClientRect().bottom <= image.getBoundingClientRect().top
                        && buttons.every(button => {
                            const r = button.getBoundingClientRect();
                            return r.top >= sidebar.top && r.bottom <= Math.min(innerHeight, sidebar.bottom)
                                && r.right <= Math.min(innerWidth, sidebar.right);
                        });
                    """)).isEqualTo(true);
            java.nio.file.Path target = java.nio.file.Path.of("build", "system-details", language + "-" + width + ".png");
            java.nio.file.Files.createDirectories(target.getParent());
            java.nio.file.Files.write(target, ((org.openqa.selenium.TakesScreenshot) browser.driver())
                    .getScreenshotAs(org.openqa.selenium.OutputType.BYTES));
        }
    }

    @Dann("zeigt eine Flottenauswahl nach einer Systemauswahl die Flottendetails")
    public void fleetSelectionReplacesSystemDetails() {
        clickSystem("Alpha");
        browser.awaitCss(".system-metric");
        browser.clickTabWithText("Flotten");
        browser.clickCss(".fleet-travel-card");
        browser.awaitSelectedTabWithText("Details");
        browser.awaitTextIn(".map-right", "Reisedetails");
        assertThat(browser.all(".system-metric")).noneMatch(WebElement::isDisplayed);
        clickSystem("Alpha");
        browser.awaitCss(".system-metric");
        browser.clickCss(".fleet-badge");
        browser.awaitTextIn(".map-right", "Reisedetails");
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
        openSystemDetails(".system-operations");
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
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> Boolean.TRUE.equals(
                js().executeScript("""
                        return [...document.querySelectorAll('.map-right vaadin-text-field')]
                            .every(field => field.value === 'Alpha')
                            && document.querySelector('.fleet-travel-card')?.__keyboardAction === true;
                        """)));
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10))
                .ignoring(org.openqa.selenium.StaleElementReferenceException.class)
                .ignoring(org.openqa.selenium.ElementNotInteractableException.class).until(_ -> {
                    if (fleetDetailsSelected()) { return true; }
                    browser.awaitCss(".fleet-travel-card").sendKeys(Keys.SPACE);
                    return fleetDetailsSelected();
                });
        browser.awaitSelectedTabWithText("Details");
        browser.awaitTextIn(".map-right", "Reisedetails");
        browser.clickButtonWithText("Ganze Galaxie");
        assertThat(browser.all(".map-left .map-controls")).hasSize(1);
    }

    private boolean fleetDetailsSelected() {
        return Boolean.TRUE.equals(js().executeScript("""
                return [...document.querySelectorAll('vaadin-tab[selected]')]
                    .some(tab => tab.textContent.trim() === 'Details');
                """));
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
