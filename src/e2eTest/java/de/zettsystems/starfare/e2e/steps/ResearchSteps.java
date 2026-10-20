package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.economy.application.DefaultEconomyService;
import de.zettsystems.starfare.game.application.GameRegistry;
import de.zettsystems.starfare.game.application.GameSessionStore;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.testsupport.ColonyStates;
import io.cucumber.java.After;
import io.cucumber.java.de.Dann;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.ui.WebDriverWait;
import static org.assertj.core.api.Assertions.assertThat;

/** Real desktop and touch-emulated phone, sharing the same persisted budget. */
public class ResearchSteps {
    private final de.zettsystems.starfare.game.application.GameService games;
    private final Browser browser;
    private final GameRegistry registry;
    private final GameSessionStore store;
    private final UserAccountService accounts;
    private GameId id;
    private Browser phone;
    public ResearchSteps(Browser browser, GameRegistry registry, GameSessionStore store, UserAccountService accounts, de.zettsystems.starfare.game.application.GameService games) {
        this.games = games; this.browser = browser; this.registry = registry; this.store = store; this.accounts = accounts;
    }
    @Dann("prüfe ich Schiffsforschung auf Desktop und Handy")
    public void research() throws IOException {
        String host = String.valueOf(accounts.findByEmail("research@example.test").orElseThrow().id());
        var state = ColonyStates.ready(RulesetRef.SPACEWARD_SHIP_RESEARCH, original -> {
            var stocks = new java.util.HashMap<>(original.stocks()); stocks.replaceAll((_, _) -> 100L);
            return new de.zettsystems.starfare.economy.domain.MetalEconomy(
                    original.deposits(), stocks, original.mining(), original.recycling());
        });
        state.seatByUser().put(host, 1); state.start(); state.planShipResearch(1, 100);
        var economy = new DefaultEconomyService();
        for (int i = 0; i < 7; i++) { economy.produce(state, Map.of()); state.nextTurn(); }
        state.planShipResearch(1, 0);
        id = GameId.newId(); store.save(new GameSession(id, "Research acceptance", host, Instant.now(), state));
        browser.open("/map/" + id.value());
        openResearch(browser); browser.awaitText("Fortschritt 98/100");
        save(browser, 50); assertBudget(50);
        browser.driver().navigate().refresh(); openResearch(browser); value(browser, "50");
        assertThat(browser.pageText()).doesNotContain("Arbeitskräfte fehlen");
        browser.captureUiAcceptance("research-desktop"); browser.assertNoSidewaysScrolling();
        browser.clickButtonWithText("Schließen");
        phone = browser.newPhoneSession(); phone.logIn("research@example.test");
        phone.open("/map/" + id.value()); phone.awaitCss(".mobile-map");
        openResearch(phone); value(phone, "50");
        phone.captureUiAcceptance("research-phone"); phone.assertNoSidewaysScrolling();
        assertReachable(phone, "#research-budget input"); assertReachable(phone, "#research-save");
        save(phone, 25); assertBudget(25);
        phone.driver().navigate().refresh(); openResearch(phone); value(phone, "25");
        phone.clickButtonWithText("Schließen");
        browser.driver().navigate().refresh();
        browser.clickButtonWithText("Runde abgeben"); browser.awaitText("Abgegeben");
        openResearch(browser);
        assertThat(browser.awaitCss("#research-save").getDomProperty("disabled")).isEqualTo("true");
        browser.clickButtonWithText("Schließen");
        // A second human seat keeps submission reviewable until explicitly submitting it.
        assertThat(games.submitTurn(id, 2)).isTrue();
        browser.awaitText("Schiffsforschung: Technikstufe 1 erreicht");
        int technology = registry.readState(id, current -> current.shipTechnology(1));
        assertThat(technology).isEqualTo(1);
        openResearch(browser); browser.awaitText("Technikstufe 1");
        save(browser, 0);
        assertThat(games.submitTurn(id, 1)).isTrue(); assertThat(games.submitTurn(id, 2)).isTrue();
        browser.driver().navigate().refresh();
        Map<Integer, Integer> cohorts = registry.readState(id, current -> current.getSystem(1).composition().levels());
        assertThat(cohorts).containsKeys(0, 1);
        browser.clickCss("#map .sys-own");
        browser.awaitTextIn(".industry-panel", "Stufe 0:");
        browser.awaitTextIn(".industry-panel", "Stufe 1:");
        browser.awaitTextIn(".industry-panel", "Kampfstärke");
        ((JavascriptExecutor) browser.driver()).executeScript(
                "document.querySelector('.industry-panel').scrollIntoView({block:'start'})");
        browser.captureUiAcceptance("research-mixed-desktop");
    }
    private static void openResearch(Browser target) {
        target.awaitCss(".app-header-menu vaadin-menu-bar-button").click();
        target.clickMatching("vaadin-menu-bar-item", item -> item.getText().strip().equals("Schiffsforschung"), "Schiffsforschung");
        target.awaitCss("#research-budget input");
    }
    private void save(Browser target, int percent) {
        target.type("#research-budget input", Keys.chord(Keys.CONTROL, "a"), String.valueOf(percent), Keys.TAB);
        target.awaitCss("#research-save").click();
        target.awaitNoneDisplayed("#research-save");
        new WebDriverWait(target.driver(), Duration.ofSeconds(10)).until(_ -> budget() == percent);
    }
    private int budget() { return registry.readState(id, state -> state.metals().orElseThrow().research().empires().get(1).percent()); }
    private void assertBudget(int expected) { assertThat(budget()).isEqualTo(expected); }
    private static void value(Browser target, String expected) { assertThat(target.awaitCss("#research-budget input").getDomProperty("value")).isEqualTo(expected); }
    private static void assertReachable(Browser target, String selector) {
        assertThat(((JavascriptExecutor) target.driver()).executeScript("""
                const e = arguments[0], r = e.getBoundingClientRect();
                const hit = document.elementFromPoint(r.x + r.width/2, r.y + r.height/2);
                return r.left >= 0 && r.right <= innerWidth && r.top >= 0 && r.bottom <= innerHeight
                    && (hit === e || e.contains(hit) || hit?.contains(e));
                """, target.awaitCss(selector))).isEqualTo(true);
    }
    @After public void cleanup() { if (phone != null) { phone.closeSession(); } if (id != null) { store.delete(id); } }
}
