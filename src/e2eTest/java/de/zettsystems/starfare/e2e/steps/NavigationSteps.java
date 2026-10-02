package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.game.application.*;
import de.zettsystems.starfare.game.domain.*;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.navigation.values.NavigationSettings;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import io.cucumber.java.After;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

/** Internal fixture; lobby release remains reserved for the combined acceptance. */
public class NavigationSteps {
    private final Browser browser;
    private final GameService games;
    private final GameRegistry registry;
    private final GameSessionStore store;
    private final UserAccountService accounts;
    private GameId id;
    private int finalArrival;
    public NavigationSteps(Browser browser, GameService games, GameRegistry registry, GameSessionStore store, UserAccountService accounts) {
        this.browser = browser; this.games = games; this.registry = registry; this.store = store; this.accounts = accounts;
    }
    @Wenn("eine interne Spaceward-Navigationspartie für {string} bereitsteht")
    public void create(String email) {
        String host = String.valueOf(accounts.findByEmail(email).orElseThrow().id());
        GameState state = new GameState(); state.resetForNewGame();
        state.rememberSetup(GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD));
        state.players().addAll(List.of(new Player(1, "Navigator", false, "#56b4e9"), new Player(2, "Opponent", false, "#d55e00")));
        state.systems().addAll(List.of(new StarSystem(1, "Alpha", 400, 350, 1, 50, 4, false),
                new StarSystem(2, "Beta", 850, 350, 1, 5, 4, false), new StarSystem(3, "Gamma", 1300, 350, 1, 5, 4, false),
                new StarSystem(4, "Delta", 1750, 350, 2, 100, 4, false)));
        for (int i = 5; i <= 8; i++) { state.systems().add(new StarSystem(i, "Remote " + i, 2500 + i * 60, 1300 + i * 50, null, 200, 4, true)); }
        state.joinedHumanPlayerIds().addAll(List.of(1, 2)); state.originalHumanPlayerIds().addAll(List.of(1, 2)); state.seatByUser().put(host, 1);
        state.intel().put(1, new java.util.HashMap<>()); state.intel().put(2, new java.util.HashMap<>());
        state.configureNavigation(new NavigationSettings(450)); state.start();
        assertThat(new DefaultFleetService().addStandingOrder(state, 1, 1, 4, 2)).isPositive();
        id = GameId.newId(); store.save(new GameSession(id, "Spaceward navigation test", host, Instant.now(), state));
        finalArrival = 1 + games.routeFor(id, 1, 1, 4).orElseThrow().rounds();
        browser.open("/map/" + id.value()); browser.awaitCss(".map-content");
    }
    @Dann("zeigt der Flottenversand Zwischenstationen und lehnt unerreichbare Ziele ab")
    public void previewAndSend() throws IOException {
        openSend("Delta");
        browser.awaitTextIn(".send-fleet-route", "Alpha → Beta → Gamma → Delta");
        browser.awaitTextIn(".send-fleet-route", "Zwischenstopps: 2");
        browser.awaitTextIn(".send-fleet-arrival", String.valueOf(finalArrival));
        screenshot("route-preview");
        cancelDialog();
        openSend("Remote 5"); browser.awaitTextIn(".send-fleet-route", "Kein erreichbarer Weg");
        assertThat(browser.awaitCss(".send-fleet-dialog vaadin-button[theme~='primary']").getDomAttribute("disabled")).isNotNull();
        cancelDialog();
        openSend("Delta");
        WebElement input = browser.awaitCss(".send-fleet-amount input"); input.sendKeys(Keys.chord(Keys.CONTROL, "a"), "10", Keys.TAB);
        browser.clickCss(".send-fleet-dialog vaadin-button[theme~='primary']");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> games.viewFor(id, 1).plannedOrders().size() == 1);
        browser.awaitCss(".fleet-lane-planned");
        assertThat(browser.all(".fleet-lane-planned")).hasSize(3);
        assertThat(browser.all(".fleet-lane-standing")).hasSize(3);
    }
    @Dann("bleibt der Auftankaufenthalt samt Ankunftsrunde nach Neuladen erhalten")
    public void refuelAndReload() throws IOException {
        advance();
        int stopArrival = registry.readState(id, state -> state.fleets().getFirst().arrivalTurn());
        while (games.viewFor(id, 1).turn() < stopArrival) { advance(); }
        browser.driver().navigate().refresh(); showFleets();
        browser.awaitTextIn(".fleet-travel-card", "Auftanken in Beta");
        browser.awaitTextIn(".fleet-travel-card", "Alpha → Beta → Gamma → Delta");
        browser.awaitTextIn(".fleet-travel-card", "Ankunftsrunde: " + finalArrival);
        screenshot("refuel");
        var original = browser.driver().manage().window().getSize();
        try { browser.driver().manage().window().setSize(new Dimension(1024, 768)); screenshot("refuel-1024"); }
        finally { browser.driver().manage().window().setSize(original); }
        assertThat(registry.readState(id, state -> state.fleets().getFirst().inFlight()).booleanValue()).isFalse();
        advance(); assertThat(registry.readState(id, state -> state.fleets().getFirst().inFlight()).booleanValue()).isFalse();
    }
    @Dann("zeigt die Karte bei Stationsverlust eine unterbrochene Weiterreise ohne erfundene Ankunft")
    public void blockAndReload() throws IOException {
        registry.writeState(id, state -> { state.updateSystem(2, system -> system.captureBy(2, 1)); return null; });
        advance(); browser.driver().navigate().refresh(); showFleets();
        browser.awaitTextIn(".fleet-travel-card", "Weiterreise aus Beta unterbrochen");
        browser.awaitTextIn(".fleet-travel-card", "Ankunft offen");
        screenshot("blocked");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> Boolean.TRUE.equals(((JavascriptExecutor) browser.driver()).executeScript("""
                const card = [...document.querySelectorAll('.fleet-travel-card')].find(element => element.textContent.includes('Weiterreise aus Beta'));
                if (!card) return false; card.click(); return true;
                """)));
        browser.awaitText("Weiterreise aus Beta unterbrochen");
        assertThat(registry.readState(id, state -> state.fleets().getFirst().journey().blocked()).booleanValue()).isTrue();
        showRelocations(); browser.awaitTextIn(".fleet-travel-card-relocation", "Verlegung pausiert");
        browser.driver().navigate().refresh(); showRelocations();
        browser.awaitTextIn(".fleet-travel-card-relocation", "Verlegung pausiert");
    }
    private void cancelDialog() {
        browser.clickButtonWithText("Abbrechen");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> Boolean.TRUE.equals(((JavascriptExecutor) browser.driver())
                .executeScript("return [...document.querySelectorAll('vaadin-dialog')].every(dialog => !dialog.opened)")));
    }
    private void openSend(String target) {
        selectSystem("Alpha"); browser.awaitCss(".system-details-heading");
        browser.awaitTextIn(".system-details-heading", "Alpha");
        browser.clickButtonWithText("Flotte senden"); browser.awaitCss(".sys-selected");
        selectSystem(target); browser.awaitCss(".send-fleet-dialog");
    }
    private void selectSystem(String name) {
        browser.awaitCss(".map-content");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> Boolean.TRUE.equals(((JavascriptExecutor) browser.driver()).executeScript("""
                const system = [...document.querySelectorAll('#map .sys-own, #map .sys-fog')].find(element => element.textContent.startsWith(arguments[0]));
                if (!system) return false; system.click(); return true;
                """, name)));
    }
    private void showFleets() { clickTab("Flotten"); }
    private void showRelocations() { clickTab("Verlegungen"); }
    private void clickTab(String text) {
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> Boolean.TRUE.equals(((JavascriptExecutor) browser.driver()).executeScript("""
                const tab = [...document.querySelectorAll('vaadin-tab')].find(element => element.textContent.trim() === arguments[0]);
                if (!tab) return false; tab.click(); return true;
                """, text)));
    }
    private void advance() { assertThat(games.submitTurn(id, 1)).isTrue(); assertThat(games.submitTurn(id, 2)).isTrue(); }
    private void screenshot(String name) throws IOException {
        if (name.startsWith("refuel") || name.equals("blocked")) {
            ((JavascriptExecutor) browser.driver()).executeScript("""
                    const card = [...document.querySelectorAll('.fleet-travel-card')]
                        .find(element => element.querySelector('.fleet-travel-card-top span')?.textContent === '✦ #1');
                    card?.scrollIntoView({block:'center'});
                    """);
        }
        Path target = Path.of("build", "navigation", name + ".png"); Files.createDirectories(target.getParent());
        Files.write(target, ((TakesScreenshot) browser.driver()).getScreenshotAs(OutputType.BYTES));
    }
    @After public void cleanup() { if (id != null) { store.delete(id); } }
}
