package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.game.application.GameRegistry;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.application.GameSessionStore;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.economy.domain.MetalEconomy;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.game.values.StandingOrder;
import de.zettsystems.starfare.testsupport.ColonyStates;
import io.cucumber.java.After;
import io.cucumber.java.de.Dann;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.ui.WebDriverWait;
import static org.assertj.core.api.Assertions.assertThat;

/** Direct production journey: presets and inputs save at once, relocations shrink with shipbuilding. */
public class ProductionPlanningSteps {
    private final Browser browser;
    private final GameService games;
    private final GameRegistry registry;
    private final GameSessionStore store;
    private final UserAccountService accounts;
    private GameId id;
    public ProductionPlanningSteps(Browser browser, GameService games, GameRegistry registry,
                                   GameSessionStore store, UserAccountService accounts) {
        this.browser = browser; this.games = games; this.registry = registry; this.store = store; this.accounts = accounts;
    }
    @Dann("prüfe ich direkt gespeicherte Produktion und angepasste Verlegungen")
    public void planning() throws IOException {
        String host = String.valueOf(accounts.findByEmail("production@example.test").orElseThrow().id());
        var state = ColonyStates.ready(RulesetRef.SPACEWARD_DIRECT_ECONOMY, original -> {
            var stocks = new java.util.HashMap<>(original.stocks());
            stocks.replaceAll((_, _) -> 100L);
            return new MetalEconomy(original.deposits(), stocks, original.mining(), original.recycling());
        });
        state.seatByUser().put(host, 1); state.start();
        var fleets = new DefaultFleetService();
        assertThat(fleets.addStandingOrder(state, 1, 1, 2, 6)).isPositive();
        assertThat(fleets.addStandingOrder(state, 1, 1, 3, 4)).isPositive();
        id = GameId.newId(); store.save(new GameSession(id, "Production planning", host, Instant.now(), state));
        browser.open("/map/" + id.value());
        select("Alpha");
        browser.awaitTextIn(".production-functions", "10 Punkte → 10 Schiffe/Runde");
        save(() -> browser.clickCss("#production-preset-autark"), () -> mining(1) == 5);
        browser.awaitTextIn(".production-save-status", "Verlegung nach Beta: 6 → 3/Runde");
        browser.awaitTextIn(".production-save-status", "Verlegung nach Enemy: 4 → 2/Runde");
        assertThat(routes()).containsEntry(2, 3).containsEntry(3, 2);
        browser.awaitTextIn(".production-functions", "5 Punkte → 5 Schiffe/Runde");
        browser.clickCss("#production-plan-open");
        field("#industry-mining input", "5");
        save(() -> enter("#industry-mining input", "3"), () -> mining(1) == 3);
        assertThat(routes()).containsEntry(2, 3).containsEntry(3, 2);
        browser.clickCss("#industry-expansion-pin");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                "true".equals(browser.awaitCss("#industry-expansion-pin").getDomProperty("checked")));
        select("Beta");
        save(() -> browser.clickCss("#production-preset-metal"), () -> mining(2) == 4);
        select("Alpha");
        assertThat(browser.awaitCss("#industry-expansion-pin").getDomProperty("checked")).isEqualTo("true");
        field("#industry-mining input", "3");
        for (int width : new int[]{1366, 1024}) {
            browser.driver().manage().window().setSize(new Dimension(width, 768));
            ((JavascriptExecutor) browser.driver()).executeScript("document.querySelector('.production-functions').scrollIntoView({block:'start'})");
            browser.captureUiAcceptance("production-planning-de-" + width);
            browser.assertNoSidewaysScrolling();
        }
        browser.driver().manage().window().setSize(new Dimension(1366, 768));
        assertThat(games.allocateProduction(id, 1, 1, 2, 1, 0)).isTrue();
        select("Beta"); field("#industry-mining input", "0");
        save(() -> browser.clickCss("#production-preset-ships"),
                () -> registry.readState(id, current -> current.industries().get(2).expansionAllocation()) == 0);
        browser.driver().navigate().refresh(); select("Alpha");
        browser.clickCss("#production-plan-open");
        field("#industry-mining input", "3");
        browser.clickButtonWithText("Runde abgeben"); browser.awaitText("Abgegeben");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> browser.all("#industry-mining").isEmpty());
        assertThat(mining(1)).isEqualTo(3);
        assertThat(browser.all("#production-submit-warning")).isEmpty();
    }
    private void save(Runnable input, java.util.function.BooleanSupplier stored) {
        browser.saveAndAwaitRebuild(".industry-panel", input, stored);
    }
    private int mining(int system) {
        return registry.readState(id, current -> current.metals().orElseThrow().mining().get(system));
    }
    private java.util.Map<Integer, Integer> routes() {
        return registry.readState(id, current -> current.standingOrders().get(1).stream().collect(
                java.util.stream.Collectors.toMap(StandingOrder::toSystemId, StandingOrder::ships)));
    }
    private void enter(String selector, String value) {
        browser.awaitCss(selector).sendKeys(Keys.chord(Keys.CONTROL, "a"), value, Keys.TAB);
    }
    private void select(String name) {
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> Boolean.TRUE.equals(
                ((JavascriptExecutor) browser.driver()).executeScript("""
                    const heading = document.querySelector('.system-details-heading');
                    if (heading?.textContent === $0 && heading.checkVisibility()) {
                        heading.scrollIntoView({block:'start'}); return true;
                    }
                    const system = [...document.querySelectorAll('#map .sys-own')].find(item => item.textContent.startsWith($0));
                    if (system) system.click(); return false;
                    """.replace("$0", "arguments[0]"), name)));
        browser.awaitTextIn(".system-details-heading", name);
    }
    private void field(String selector, String expected) {
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                browser.awaitCss(selector).getDomProperty("value").equals(expected));
        assertThat(browser.awaitCss(selector).getDomProperty("value")).isEqualTo(expected);
    }
    @After public void cleanup() { if (id != null) { store.delete(id); } }
}
