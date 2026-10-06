package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.game.application.GameRegistry;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.application.GameSessionStore;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.Player;
import io.cucumber.java.After;
import io.cucumber.java.de.Dann;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.ui.WebDriverWait;
import static org.assertj.core.api.Assertions.assertThat;

public class AiStrategySteps {
    private final Browser browser;
    private final GameService games;
    private final GameRegistry registry;
    private final GameSessionStore store;
    private GameId created;

    public AiStrategySteps(Browser browser, GameService games, GameRegistry registry, GameSessionStore store) {
        this.browser = browser;
        this.games = games;
        this.registry = registry;
        this.store = store;
    }

    @Dann("kann ich unterschiedliche KI-Strategien im Wizard wählen und dauerhaft speichern")
    public void chooseAndPersist() {
        browser.clickButtonWithText("Neues Spiel");
        awaitChoices(5);
        choose(1, "Früher Angriff");
        choose(2, "Verteidigung");
        changeCount(1);
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10))
                .until(_ -> browser.driver().findElements(By.id("ai-strategy-2")).isEmpty());
        changeCount(2);
        assertSelection(1, "Früher Angriff");
        assertSelection(2, "Verteidigung");
        variant("Spaceward");
        awaitChoices(8);
        choose(1, "Adaptiver Industrieausbau");
        variant("SectorForces");
        awaitChoices(5);
        assertSelection(1, "Basisstrategie");
        assertSelection(2, "Verteidigung");
        variant("Spaceward");
        awaitChoices(8);
        choose(1, "Adaptiver Industrieausbau");
        browser.clickCss("#join-after-create");
        browser.clickButtonWithText("Zusammenfassung prüfen");
        browser.awaitText("KI 1: Adaptiver Industrieausbau");
        browser.awaitText("KI 2: Verteidigung");
        browser.clickButtonWithText("Zurück zu den Einstellungen");
        assertSelection(1, "Adaptiver Industrieausbau");
        browser.clickButtonWithText("Zusammenfassung prüfen");
        List<GameId> before = games.listGames();
        browser.clickButtonWithText("Spiel anlegen");
        created = new WebDriverWait(browser.driver(), Duration.ofSeconds(15)).until(_ -> games.listGames().stream()
                .filter(id -> !before.contains(id)).findFirst().orElse(null));
        List<Player> players = registry.readState(created, state -> List.copyOf(state.players()));
        assertThat(players).extracting(Player::aiStrategy)
                .containsExactly(AiStrategy.BASELINE, AiStrategy.INDUSTRY_ADAPTIVE, AiStrategy.DEFENSE);
        browser.driver().navigate().refresh();
        browser.awaitText(games.gameNameOf(created));
        var restored = store.load(created).orElseThrow().readState(state -> state.originalSetup().orElseThrow());
        assertThat(restored.aiStrategies()).containsExactly(AiStrategy.INDUSTRY_ADAPTIVE, AiStrategy.DEFENSE);
    }

    private void choose(int seat, String label) {
        var input = browser.awaitCss("#ai-strategy-" + seat + " input");
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"), label);
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10))
                .ignoring(org.openqa.selenium.StaleElementReferenceException.class)
                .until(_ -> browser.all("vaadin-combo-box-item").stream()
                        .filter(item -> item.isDisplayed() && item.getText().strip().equals(label))
                        .findFirst().orElse(null)).click();
        input.sendKeys(Keys.TAB);
        assertSelection(seat, label);
    }

    private void assertSelection(int seat, String label) {
        Boolean matched = new WebDriverWait(browser.driver(), Duration.ofSeconds(15))
                .ignoring(org.openqa.selenium.StaleElementReferenceException.class)
                .until(_ -> label.equals(browser.awaitCss("#ai-strategy-" + seat + " input").getDomProperty("value")));
        assertThat(matched).isTrue();
    }

    private void changeCount(int count) {
        var field = new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                browser.all("vaadin-integer-field, vaadin-number-field").stream()
                        .filter(element -> "KI-Spieler".equals(element.getDomProperty("label")))
                        .findFirst().orElse(null));
        ((JavascriptExecutor) browser.driver()).executeScript("arguments[0].scrollIntoView({block:'center'});", field);
        field.findElement(By.cssSelector("input"))
                .sendKeys(Keys.chord(Keys.CONTROL, "a"), String.valueOf(count), Keys.TAB);
    }

    private void variant(String name) {
        ((JavascriptExecutor) browser.driver()).executeScript("""
                [...document.querySelectorAll('#ruleset-selection vaadin-radio-button')]
                    .find(r => r.textContent.includes(arguments[0])).click();
                """, name);
    }

    private void awaitChoices(int count) {
        Boolean matched = new WebDriverWait(browser.driver(), Duration.ofSeconds(15))
                .ignoring(org.openqa.selenium.StaleElementReferenceException.class).until(_ -> {
            var input = browser.awaitCss("#ai-strategy-1 input");
            boolean opened = Boolean.TRUE.equals(((JavascriptExecutor) browser.driver()).executeScript(
                    "return document.querySelector('#ai-strategy-1').opened;"));
            if (!opened) {
                ((JavascriptExecutor) browser.driver()).executeScript("arguments[0].scrollIntoView({block:'center'});", input);
                input.click();
                input.sendKeys(Keys.ARROW_DOWN);
            }
            var choices = ((JavascriptExecutor) browser.driver()).executeScript(
                    "const field = document.querySelector('#ai-strategy-1'); return field?.opened ? field.size : 0;");
            return choices instanceof Number number && number.intValue() == count;
        });
        assertThat(matched).isTrue();
        browser.awaitCss("#ai-strategy-1 input").sendKeys(Keys.ESCAPE, Keys.TAB);
    }

    @After
    public void cleanup() { if (created != null) { store.delete(created); } }
}
