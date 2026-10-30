package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.testsupport.ReviewGameFixture;
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

/** Browser checks for map geometry, native dragging, keyboard actions and preserved inputs. */
public class ReviewMapSteps {
    private final Browser browser;
    private final ReviewGameFixture fixture;
    private final GameRegistry registry;
    private final GameService games;
    private final de.zettsystems.starfare.game.application.Broadcaster broadcaster;
    private ReviewGameFixture.Game first;
    private ReviewGameFixture.Game second;


    @Dann("bleiben Systeme zwischen dreißig Flotten bei verschiedenen Zoomstufen erreichbar")
    public void crowdedMap() throws java.io.IOException {
        prepareCrowdedMap();
        assertSystemHitTargets();
        selectCrowdedFleetLane();
        WebElement beta = mapSystem("Beta");
        new org.openqa.selenium.interactions.Actions(browser.driver()).contextClick(beta).perform();
        browser.awaitCss(".sys-selected");
        assertThat(browser.all("vaadin-context-menu-overlay[opened]")).isEmpty();
    }

    private void prepareCrowdedMap() {
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
    }

    private void assertSystemHitTargets() {
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
    }

    private void selectCrowdedFleetLane() throws java.io.IOException {
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

    @Dann("kann ich eine Verlegung per Ziehen erstellen")
    public void relocationDrop() {
        browser.clickButtonWithText("Eigenes Reich");
        dragRelocation();
        browser.clickButtonWithText("Verlegung speichern");
        awaitSendClosed();
        assertThat(games.viewFor(first.id(), first.hostSeat()).standingOrders()).singleElement()
                .satisfies(order -> {
                    assertThat(order.fromSystemId()).isEqualTo(1);
                    assertThat(order.toSystemId()).isEqualTo(2);
                });
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

    @Dann("zeigt die Systemsicht direkte Aktionen in beiden Sprachen auf Desktopbreiten")
    public void systemDetails() throws java.io.IOException {
        assertThat(browser.awaitCss(".app-header-empire").getText()).isEqualTo("Host Empire");
        checkSystemActions("de", "Versendbare Schiffe");
        switchLanguage("Englisch", "English");
        checkSystemActions("en", "Sendable ships");
        switchLanguage("German", "Deutsch");
        browser.driver().manage().window().setSize(new org.openqa.selenium.Dimension(1366, 768));
    }

    private void openSystemDetails(String selector) {
        browser.clickCss(selector + " > vaadin-details-summary");
    }

    private void switchLanguage(String option, String selected) {
        browser.type(".lang-switcher input", Keys.chord(Keys.CONTROL, "a"), option);
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

    @Dann("bleiben Zoom und Kartenposition für beide Partien getrennt")
    public void independentViewports() {
        storeViewport(first, 1.4, 1100);
        open(second);
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                "1".equals(js().executeScript("return document.querySelector('#map').style.zoom")));
        storeViewport(second, 0.8, 600);
        open(first);
        assertViewport(1.4, 1100);
        open(second);
        assertViewport(0.8, 600);
    }

    @Dann("bleiben Eingaben bei Aktualisierungen erhalten und ist die Karte per Tastatur bedienbar")
    public void stableInputsAndKeyboard() {
        open(first);
        browser.type("#map .sys-own", Keys.ENTER);
        browser.awaitCss(".system-metric");
        openSystemDetails(".system-operations");
        WebElement reserve = browser.type(".map-right vaadin-integer-field input", Keys.chord(Keys.CONTROL, "a"), "3");
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
                            && [...document.querySelectorAll('.fleet-travel-card')]
                                .some(card => card.checkVisibility() && card.__keyboardAction === true);
                        """)));
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10))
                .ignoring(org.openqa.selenium.StaleElementReferenceException.class)
                .ignoring(org.openqa.selenium.ElementNotInteractableException.class).until(_ -> {
                    if (fleetDetailsSelected()) { return true; }
                    WebElement card = browser.all(".fleet-travel-card").stream().filter(WebElement::isDisplayed)
                            .findFirst().orElse(null);
                    if (card == null) { return false; }
                    card.sendKeys(Keys.SPACE);
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
                    .some(tab => tab.textContent.trim() === 'Details')
                    || document.querySelector('.map-sidebar-select input')?.value === 'Details';
                """));
    }

    private void storeViewport(ReviewGameFixture.Game game, double zoom, int left) {
        String key = "starfare.viewport." + game.id().value();
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                key.equals(js().executeScript("return document.querySelector('#scroll').__starfareViewportKey")));
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

    private WebElement visibleRouteFilter() {
        return new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                browser.all(".map-right vaadin-text-field input").stream().filter(WebElement::isDisplayed)
                        .findFirst().orElse(null));
    }

    private void selectComboItem(String selector, String name) {
        browser.clickMatching(selector, item -> item.getText().strip().equals(name), "Option " + name);
    }

    private void awaitSendClosed() {
        browser.awaitNoneDisplayed(".send-fleet-dialog[opened]");
    }

    private JavascriptExecutor js() {
        if (browser.driver() instanceof JavascriptExecutor executor) {
            return executor;
        }
        throw new IllegalStateException("Browser does not support JavaScript");
    }

    @After
    public void cleanupGames() {

        if (first != null) { games.abortGame(first.id()); }
        if (second != null) { games.abortGame(second.id()); }
    }
}
