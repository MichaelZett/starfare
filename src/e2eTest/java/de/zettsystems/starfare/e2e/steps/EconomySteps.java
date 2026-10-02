package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.application.GameRegistry;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.application.GameSessionStore;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import io.cucumber.java.After;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.assertj.core.api.Assertions.*;

/** Isolated test fixture; product creation stays disabled until the combined acceptance. */
public class EconomySteps {
    private final Browser browser; private final GameService games; private final GameRegistry registry;
    private final GameSessionStore store; private final UserAccountService accounts;
    private GameId id;
    public EconomySteps(Browser browser, GameService games, GameRegistry registry, GameSessionStore store, UserAccountService accounts) {
        this.browser = browser; this.games = games; this.registry = registry; this.store = store; this.accounts = accounts;
    }
    @Wenn("eine interne Spaceward-Testpartie für {string} bereitsteht")
    public void create(String email) {
        String host = String.valueOf(accounts.findByEmail(email).orElseThrow().id());
        GameState state = new GameState(); state.resetForNewGame();
        state.rememberSetup(GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD));
        state.players().addAll(List.of(new Player(1, "Industrialist", false, "#56b4e9"), new Player(2, "Opponent", false, "#d55e00")));
        state.systems().add(new StarSystem(1, "Alpha", 400, 350, 1, 12, 10, false, 10));
        state.systems().add(new StarSystem(2, "Beta", 800, 350, 1, 3, 4, false));
        state.systems().add(new StarSystem(3, "Enemy", 2800, 1600, 2, 100, 8, false));
        for (int i = 4; i <= 8; i++) { state.systems().add(new StarSystem(i, "Remote " + i, 2000 + i * 80, 1300 + i * 50, null, 200, 2, true)); }
        state.joinedHumanPlayerIds().addAll(List.of(1, 2)); state.originalHumanPlayerIds().addAll(List.of(1, 2));
        state.seatByUser().put(host, 1); state.configureNavigation(new de.zettsystems.starfare.navigation.values.NavigationSettings(5000)); state.start();
        var fleets = new DefaultFleetService();
        assertThat(fleets.addStandingOrder(state, 1, 1, 3, 7)).isPositive();
        assertThat(fleets.addStandingOrder(state, 1, 1, 4, 3)).isPositive();
        id = GameId.newId(); store.save(new GameSession(id, "Spaceward industry test", host, Instant.now(), state));
        browser.open("/map/" + id.value()); browser.awaitCss(".map-content"); selectAlpha();
    }
    @Dann("kann ich den Schiffbau zugunsten der Industrie umschalten und sehe den Lieferengpass")
    public void allocate() throws IOException {
        browser.awaitTextIn(".industry-panel", "Kapazität 10 / 50");
        WebElement input = browser.awaitCss("#industry-expansion input");
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"), "4", Keys.TAB);
        browser.awaitTextIn(".industry-panel", "Schiffbau: 6 · Ausbau: 4");
        browser.awaitTextIn(".industry-panel", "8 / 10 Schiffe");
        assertThat(registry.readState(id, state -> state.industries().get(1).expansionAllocation()).intValue()).isZero();
        browser.clickCss("#industry-apply");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> registry.readState(id, state -> state.industries().get(1).expansionAllocation()) == 4);
        browser.awaitCss(".industry-bottleneck"); browser.awaitCss(".system-industry-buildings");
        ((JavascriptExecutor) browser.driver()).executeScript("document.querySelector('.system-details-heading').scrollIntoView({block:'start'})");
        screenshot("spaceward-system");
        ((JavascriptExecutor) browser.driver()).executeScript("document.querySelector('.industry-panel').scrollIntoView({block:'center'})");
        screenshot("spaceward-industry");
        var original = browser.driver().manage().window().getSize();
        try { browser.driver().manage().window().setSize(new Dimension(1024, 768)); screenshot("spaceward-industry-1024"); }
        finally { browser.driver().manage().window().setSize(original); }
    }
    @Dann("bleibt die Verteilung nach Neuladen erhalten und sperrt sich nach Abgabe")
    public void reloadAndSubmit() {
        browser.driver().navigate().refresh(); selectAlpha();
        assertThat(browser.awaitCss("#industry-expansion input").getDomProperty("value")).isEqualTo("4");
        browser.clickButtonWithText("Runde abgeben");
        browser.awaitText("Abgegeben");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> browser.all("#industry-apply").isEmpty());
        assertThat(games.allocateExpansion(id, 1, 1, 1, 0)).isFalse();
    }
    @Dann("erhöht sich die Kapazität nach fünf Runden ohne vorzeitige Schiffsproduktion")
    public void grow() {
        assertThat(games.submitTurn(id, 2)).isTrue();
        for (int round = 2; round <= 5; round++) { assertThat(games.submitTurn(id, 1)).isTrue(); assertThat(games.submitTurn(id, 2)).isTrue(); }
        selectAlpha(); browser.awaitTextIn(".industry-panel", "Kapazität 11 / 50");
        browser.awaitTextIn(".industry-panel", "Schiffbau: 7 · Ausbau: 4");
        browser.awaitTextIn(".industry-panel", "Ausbaufortschritt: 0 / 22");
        assertThat(games.viewFor(id, 1).systems().getFirst().garrison()).isEqualTo(10);
        assertThat(registry.readState(id, state -> state.turn()).intValue()).isEqualTo(6);
        registry.writeState(id, state -> { state.updateSystem(1, system -> system.afterDefense(1)); return null; });
        browser.driver().navigate().refresh(); selectAlpha();
        browser.awaitTextIn(".industry-panel", "0 / 10 Schiffe");
        browser.awaitCss("#industry-expansion input").sendKeys(Keys.chord(Keys.CONTROL, "a"), "0", Keys.TAB);
        browser.awaitTextIn(".industry-panel", "Schiffbau: 11 · Ausbau: 0");
        browser.awaitTextIn(".industry-panel", "2 / 10 Schiffe");
        assertThat(registry.readState(id, state -> state.industries().get(1).expansionAllocation()).intValue()).isEqualTo(4);
    }
    private void selectAlpha() {
        browser.awaitCss(".map-content");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> Boolean.TRUE.equals(((JavascriptExecutor) browser.driver()).executeScript("""
                const system = [...document.querySelectorAll('#map .sys-own')].find(element => element.textContent.startsWith('Alpha'));
                if (!system) return false;
                system.click(); return true;
                """)));
        browser.awaitCss(".industry-panel");
    }
    private void screenshot(String name) throws IOException {
        Path target = Path.of("build", "economy", name + ".png"); Files.createDirectories(target.getParent());
        Files.write(target, ((TakesScreenshot) browser.driver()).getScreenshotAs(OutputType.BYTES));
    }
    @After public void cleanup() { if (id != null) { store.delete(id); } }
}
