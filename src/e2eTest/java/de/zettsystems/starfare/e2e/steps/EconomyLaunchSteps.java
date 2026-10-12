package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.game.application.GameRegistry;
import de.zettsystems.starfare.game.application.GameSessionStore;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.RulesetRef;
import io.cucumber.java.After;
import io.cucumber.java.de.Dann;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import static org.assertj.core.api.Assertions.assertThat;

/** The same launch journey checks the released default and the opt-in acceptance profile. */
public class EconomyLaunchSteps {
    private final Browser browser;
    private final GameRegistry registry;
    private final GameSessionStore store;
    private GameId id;
    public EconomyLaunchSteps(Browser browser, GameRegistry registry, GameSessionStore store) {
        this.browser = browser; this.registry = registry; this.store = store;
    }

    @Dann("prüfe ich die Spaceward-Neuanlage und die freigeschaltete Wirtschaft")
    public void launch() {
        var rules = registry.rulesets().find("spaceward").orElseThrow().defaultRef();
        browser.clickButtonWithText("Neues Spiel");
        browser.awaitText("Regeln 1.0.0");
        var choices = browser.all("#ruleset-selection vaadin-radio-button");
        assertThat(choices.getFirst().getDomAttribute("checked")).isNotNull();
        ((JavascriptExecutor) browser.driver()).executeScript("arguments[0].click();", choices.get(1));
        browser.clickButtonWithText("Zusammenfassung prüfen");
        browser.awaitText("Spaceward · Regeln " + rules.version());
        browser.clickButtonWithText("Spiel anlegen");
        browser.awaitUrl(url -> url.contains("/map/"), "Spaceward did not start");
        String value = browser.driver().getCurrentUrl().split("/map/", 2)[1].split("[?#]", 2)[0];
        id = new GameId(value);
        var snapshot = registry.readState(id, de.zettsystems.starfare.game.domain.GameState::toSnapshot);
        assertThat(snapshot.ruleset()).isEqualTo(rules);
        if (!rules.equals(RulesetRef.SPACEWARD_ECONOMY_PREVIEW)) {
            assertThat(snapshot.metals()).isNull();
            return;
        }
        assertThat(snapshot.metals()).isNotNull();
        int home = snapshot.systems().stream().filter(system -> Integer.valueOf(1).equals(system.ownerId())).findFirst().orElseThrow().id();
        clickHome();
        browser.awaitTextIn(".app-header-stats", "Metall: 10");
        browser.clickCss("#production-plan-open");
        enter("#industry-mining input", "1");
        browser.clickCss("#industry-apply");
        browser.awaitTextIn(".metal-details", "Verteilung: 1 Förderung");
        ((JavascriptExecutor) browser.driver()).executeScript(
                "document.querySelectorAll('.recycling-details vaadin-details').forEach(d => d.opened = true);");
        browser.clickCss("#recycling-open");
        enter("#recycling-ships input", "1");
        browser.clickCss("#recycling-apply");
        browser.awaitTextIn(".recycling-details", "Vorgemerkt: 1 Schiffe");
        browser.driver().navigate().refresh(); clickHome();
        browser.awaitTextIn(".recycling-details", "Vorgemerkt: 1 Schiffe");
        browser.clickButtonWithText("Runde abgeben");
        new org.openqa.selenium.support.ui.WebDriverWait(browser.driver(), java.time.Duration.ofSeconds(10)).until(_ ->
                registry.readState(id, GameState::turn) == snapshot.turn() + 1);
        browser.awaitText("1 Schiffe verschrottet, 0,7 Metall zurückgewonnen.");
        browser.driver().navigate().refresh(); clickHome();
        ((JavascriptExecutor) browser.driver()).executeScript(
                "document.querySelectorAll('.recycling-details vaadin-details').forEach(d => d.opened = true);");
        browser.awaitTextIn(".recycling-details", "Gespeicherter Metallrest: 0,7");
        var after = registry.readState(id, de.zettsystems.starfare.game.domain.GameState::toSnapshot);
        assertThat(after.metals().deposits().get(home).remaining()).isEqualTo(snapshot.metals().deposits().get(home).remaining() - 3);
        assertThat(after.metals().recycling().orders().get(home)).isZero();
        assertThat(after.turn()).isEqualTo(snapshot.turn() + 1);
    }

    private void clickHome() {
        browser.awaitCss(".map-content");
        ((JavascriptExecutor) browser.driver()).executeScript("document.querySelector('#map .sys-own').click();");
        browser.awaitCss(".industry-panel");
    }
    private void enter(String selector, String value) { browser.awaitCss(selector).sendKeys(Keys.chord(Keys.CONTROL, "a"), value, Keys.TAB); }
    @After public void cleanup() { if (id != null) { store.delete(id); } }
}
