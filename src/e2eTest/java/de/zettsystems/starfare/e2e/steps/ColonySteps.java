package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.game.application.GameRegistry;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.application.GameSessionStore;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.testsupport.ColonyStates;
import io.cucumber.java.After;
import io.cucumber.java.de.Dann;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.support.ui.WebDriverWait;
import static org.assertj.core.api.Assertions.*;

/** Test-only saved W1 party; the public wizard remains on released rules. */
public class ColonySteps {
    private final Browser browser;
    private final GameService games;
    private final GameRegistry registry;
    private final GameSessionStore store;
    private final UserAccountService accounts;
    private GameId id;

    public ColonySteps(Browser browser, GameService games, GameRegistry registry,
                       GameSessionStore store, UserAccountService accounts) {
        this.browser = browser; this.games = games; this.registry = registry;
        this.store = store; this.accounts = accounts;
    }
    @Dann("prüfe ich die interne Koloniewirtschaft mit Rundenwechsel und gespeicherter Nachbetrachtung")
    public void colonyJourney() throws IOException {
        String host = String.valueOf(accounts.findByEmail("colonies@example.test").orElseThrow().id());
        var state = ColonyStates.ready();
        state.seatByUser().put(host, 1); state.start();
        id = GameId.newId();
        store.save(new GameSession(id, "Colony acceptance", host, Instant.now(), state));
        browser.open("/map/" + id.value());
        selectAlpha();
        browser.awaitTextIn(".colony-details", "Nutzbare Industrie: 6 von 10");
        browser.awaitTextIn(".colony-details", "Bevölkerung: 6,9");
        assertThat(browser.awaitCss("#industry-expansion").getDomProperty("max")).isEqualTo("6");
        screenshot("initial");
        var input = browser.awaitCss("#industry-expansion input");
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"), "3", Keys.TAB);
        browser.clickCss("#industry-apply");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                registry.readState(id, current -> current.industries().get(1).expansionAllocation()) == 3);
        assertThat(games.submitTurn(id, 1)).isTrue();
        assertThat(games.submitTurn(id, 2)).isTrue();
        browser.driver().navigate().refresh(); selectAlpha();
        browser.awaitTextIn(".colony-details", "Nutzbare Industrie: 7 von 10");
        browser.awaitTextIn(".colony-details", "Bevölkerung: 7,11");
        int output = registry.readState(id, current -> current.getSystem(1).productionPerTurn());
        assertThat(output).isEqualTo(4);
        screenshot("growth");
        assertThat(games.submitTurn(id, 1)).isTrue();
        assertThat(games.submitTurn(id, 2)).isTrue();
        registry.writeState(id, current -> { current.endGame(1); return null; });
        browser.driver().navigate().refresh();
        browser.awaitCss(".review-controls"); selectAlpha();
        browser.awaitTextIn(".colony-details", "Bevölkerung: 7,33");
        ((JavascriptExecutor) browser.driver()).executeScript("""
                const slider = document.querySelector('#review-timeline');
                slider.value = slider.min;
                slider.dispatchEvent(new Event('change', {bubbles:true}));
                """);
        selectAlpha();
        browser.awaitTextIn(".colony-details", "Bevölkerung: 7,11");
        screenshot("history");
        browser.open("/archive"); browser.awaitText("Colony acceptance");
        assertThat(browser.driver().getPageSource()).contains("Spaceward");
    }
    private void selectAlpha() {
        browser.awaitCss(".map-content");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> Boolean.TRUE.equals(
                ((JavascriptExecutor) browser.driver()).executeScript("""
                    const system = [...document.querySelectorAll('#map .sys-own, #map .sys-fog')]
                        .find(element => element.textContent.startsWith('Alpha'));
                    if (!system) return false;
                    system.click(); return true;
                    """)));
        browser.awaitTextIn(".system-details-heading", "Alpha");
    }
    private void screenshot(String name) throws IOException {
        ((JavascriptExecutor) browser.driver()).executeScript("arguments[0].scrollIntoView({block:'center'});",
                browser.awaitCss(".colony-details"));
        Path target = Path.of("build", "colonies", name + ".png"); Files.createDirectories(target.getParent());
        Files.write(target, ((TakesScreenshot) browser.driver()).getScreenshotAs(OutputType.BYTES));
    }
    @After public void cleanup() { if (id != null) { store.delete(id); } }
}
