package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.game.application.GameRegistry;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.application.GameSessionStore;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.testsupport.MetalStates;
import io.cucumber.java.After;
import io.cucumber.java.de.Dann;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.WebDriverWait;
import static org.assertj.core.api.Assertions.assertThat;

/** Full inline journey, including two systems and an external saved-plan conflict. */
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
    @Dann("prüfe ich Produktionsvorgaben und mehrere lokale Entwürfe")
    public void planning() throws IOException {
        String host = String.valueOf(accounts.findByEmail("production@example.test").orElseThrow().id());
        var state = MetalStates.ready(2, 100); state.seatByUser().put(host, 1); state.start();
        id = GameId.newId(); store.save(new GameSession(id, "Production planning", host, Instant.now(), state));
        browser.open("/map/" + id.value());
        browser.clickCss("#economy-notice-metal"); browser.awaitTextIn(".economy-issue-card", "Alpha");
        browser.clickButtonWithText("Produktion ändern"); browser.awaitCss(".production-functions");
        var before = registry.readState(id, GameState::toSnapshot);
        browser.clickCss("#production-preset-autark");
        field("#industry-shipbuilding input", "3"); field("#industry-mining input", "3");
        assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(before);
        browser.clickCss("#industry-expansion-pin");
        select("Beta"); browser.clickCss("#production-preset-metal");
        browser.awaitTextIn(".economy-notices", "2 Entwürfe");
        select("Alpha"); field("#industry-mining input", "3");
        assertThat(browser.awaitCss("#industry-expansion-pin").getDomProperty("checked")).isEqualTo("true");
        for (int width : new int[]{1366, 1024}) {
            browser.driver().manage().window().setSize(new Dimension(width, 768));
            ((JavascriptExecutor) browser.driver()).executeScript("document.querySelector('.production-functions').scrollIntoView({block:'start'})");
            browser.captureUiAcceptance("production-planning-de-" + width);
            browser.assertNoSidewaysScrolling();
        }
        browser.driver().manage().window().setSize(new Dimension(1366, 768));
        browser.clickButtonWithText("Runde abgeben"); browser.awaitCss("#production-submit-warning");
        browser.clickButtonWithText("Entwürfe ansehen"); browser.awaitTextIn(".economy-issue-card", "Beta");
        browser.clickButtonWithText("Produktion ändern");
        ((JavascriptExecutor) browser.driver()).executeScript("document.querySelector('#industry-apply').scrollIntoView({block:'nearest'})");
        browser.clickCss("#industry-apply");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                registry.readState(id, current -> current.metals().orElseThrow().mining().get(1)) == 3);
        browser.awaitTextIn(".economy-notices", "1 Entwurf");
        assertThat(games.allocateProduction(id, 1, 1, 2, 1, 0)).isTrue();
        select("Beta"); browser.awaitTextIn(".production-editor", "veraltet");
        ((JavascriptExecutor) browser.driver()).executeScript("document.querySelector('#industry-apply').scrollIntoView({block:'nearest'})");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                "true".equals(browser.awaitCss("#industry-apply").getDomProperty("disabled")));
        assertThat(browser.awaitCss("#industry-apply").getDomProperty("disabled")).isEqualTo("true");
        browser.clickButtonWithText("Neu prüfen"); browser.awaitTextIn(".production-editor", "Ungespeicherte Änderungen");
        browser.clickButtonWithText("Gespeicherten Plan laden");
        browser.clickCss("#production-preset-metal");
        browser.clickButtonWithText("Runde abgeben"); browser.awaitCss("#production-submit-warning");
        browser.clickButtonWithText("Mit gespeicherten Plänen abgeben"); browser.awaitText("Abgegeben");
        assertThat(registry.readState(id, current -> current.metals().orElseThrow().mining().get(2)).intValue()).isZero();
        assertThat(registry.readState(id, current -> current.industries().get(2).expansionAllocation()).intValue()).isEqualTo(1);
        browser.driver().navigate().refresh(); select("Alpha");
        assertThat(browser.all("#industry-apply")).isEmpty();
        assertThat(browser.all("#production-drafts-open")).isEmpty();
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
