package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.game.application.GameRegistry;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.application.GameSessionStore;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.testsupport.MetalStates;
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

public class MetalSteps {
    private final Browser browser;
    private final GameService games;
    private final GameRegistry registry;
    private final GameSessionStore store;
    private final UserAccountService accounts;
    private GameId id;
    public MetalSteps(Browser browser, GameService games, GameRegistry registry,
                      GameSessionStore store, UserAccountService accounts) {
        this.browser = browser; this.games = games; this.registry = registry;
        this.store = store; this.accounts = accounts;
    }
    @Dann("prüfe ich die interne Metallwirtschaft mit Förderung und gespeichertem Engpass")
    public void metalJourney() throws IOException {
        String host = String.valueOf(accounts.findByEmail("metals@example.test").orElseThrow().id());
        var state = MetalStates.ready(2, 5);
        state.seatByUser().put(host, 1); state.start();
        id = GameId.newId();
        store.save(new GameSession(id, "Metal acceptance", host, Instant.now(), state));
        browser.open("/map/" + id.value()); selectAlpha();
        browser.awaitTextIn(".metal-details", "6 Punkte → 1 von 6 Schiffen/Runde · Metall fehlt");
        browser.awaitTextIn(".metal-details", "Metall fördern · 3 je Punkt");
        browser.awaitTextIn(".app-header-stats", "2 (\u22122)");
        screenshot("initial");
        switchLanguage("Englisch", "2 (\u22122)");
        selectAlpha(); browser.awaitTextIn(".metal-details", "6 points → 1 of 6 ships/round · metal missing");
        browser.clickCss("#production-plan-open");
        browser.awaitTextIn(".production-editor", "Distribute 6 capacity points");
        screenshot("allocation-en");
        switchLanguage("German", "2 (\u22122)");
        selectAlpha(); browser.clickCss("#production-plan-open");
        browser.awaitCss("#industry-mining input");
        var before = registry.readState(id, de.zettsystems.starfare.game.domain.GameState::toSnapshot);
        // Dragging only previews; nothing is stored before the slider is released.
        ((JavascriptExecutor) browser.driver()).executeScript("""
                const slider = document.querySelectorAll('.production-allocation-row input[type=range]')[1];
                slider.value = 6; slider.dispatchEvent(new Event('input', {bubbles:true}));
                """);
        browser.awaitTextIn(".metal-details", "0 Punkte → 0 Schiffe/Runde");
        assertFieldValue("#industry-shipbuilding input", "0");
        assertThat(registry.readState(id, de.zettsystems.starfare.game.domain.GameState::toSnapshot)).isEqualTo(before);
        save(() -> ((JavascriptExecutor) browser.driver()).executeScript("""
                const slider = document.querySelectorAll('.production-allocation-row input[type=range]')[1];
                slider.value = 6; slider.dispatchEvent(new Event('change', {bubbles:true}));
                """), () -> mining() == 6);
        save(() -> enter("#industry-expansion input", "2"), () -> mining() == 4);
        browser.clickCss("#industry-expansion-pin");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                "true".equals(browser.awaitCss("#industry-expansion-pin").getDomProperty("checked")));
        enter("#industry-mining input", "6");
        assertFieldValue("#industry-expansion input", "2");
        assertFieldValue("#industry-mining input", "4");
        assertThat(mining()).isEqualTo(4);
        browser.clickCss("#industry-expansion-pin");
        save(() -> browser.clickCss("#production-preset-ships"), () -> mining() == 0);
        save(() -> enter("#industry-mining input", "2"), () -> mining() == 2);
        save(() -> enter("#industry-expansion input", "1"), () -> registry.readState(id, current -> current.industries().get(1).expansionAllocation()) == 1);
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                registry.readState(id, current -> current.industries().get(1).expansionAllocation()) == 1);
        screenshot("allocation-saved");
        browser.awaitTextIn(".metal-details", "3 Punkte → 3 Schiffe/Runde");
        browser.awaitTextIn(".metal-details", "2 Punkte → 5 Metall/Runde · Vorkommen erschöpft");
        browser.awaitTextIn(".production-save-status", "Gespeichert");
        browser.driver().navigate().refresh(); selectAlpha();
        browser.awaitTextIn(".metal-details", "3 Punkte → 3 Schiffe/Runde");
        browser.awaitTextIn(".app-header-stats", "2 (\u22122)");
        assertThat(games.submitTurn(id, 1)).isTrue();
        assertThat(games.submitTurn(id, 2)).isTrue();
        browser.driver().navigate().refresh(); selectAlpha();
        browser.awaitTextIn(".system-background", "Metallvorkommen: 0");
        browser.awaitTextIn(".app-header-stats", "0 (");
        browser.awaitTextIn(".metal-details", "→ 0 von 4 Schiffen/Runde · Metall fehlt");
        assertThat(registry.<Integer>readState(id, current -> current.getSystem(1).garrison())).isEqualTo(15);
        screenshot("exhausted");
        assertThat(games.allocateProduction(id, 1, 2, 1, 1, 0)).isTrue();
        assertThat(games.submitTurn(id, 1)).isTrue();
        assertThat(games.submitTurn(id, 2)).isTrue();
        assertThat(registry.<Integer>readState(id, current -> current.getSystem(1).garrison())).isEqualTo(15);
        registry.writeState(id, current -> { current.endGame(1); return null; });
        browser.driver().navigate().refresh(); browser.awaitCss(".review-controls"); selectAlpha();
        browser.awaitTextIn(".metal-details", "0 Punkte × 3 → 0 Metall/Runde");
        ((JavascriptExecutor) browser.driver()).executeScript("""
                const slider = document.querySelector('#review-timeline');
                slider.value = slider.min;
                slider.dispatchEvent(new Event('change', {bubbles:true}));
                """);
        browser.awaitTextIn(".review-notice", "Historischer Stand: vollständig aufgedeckt.");
        selectAlpha(); browser.awaitTextIn(".system-background", "Metallvorkommen: 0");
        browser.awaitTextIn(".metal-details", "2 Punkte → 0 Metall/Runde · Vorkommen erschöpft");
        assertThat(browser.driver().findElements(org.openqa.selenium.By.cssSelector("#industry-mining"))).isEmpty();
        screenshot("history");
        browser.open("/archive"); browser.awaitText("Metal acceptance");
        assertThat(browser.driver().getPageSource()).contains("Spaceward");
    }
    @Dann("prüfe ich Recycling mit einmaliger Gutschrift und gespeicherten Metallresten")
    public void recyclingJourney() throws IOException {
        String host = String.valueOf(accounts.findByEmail("recycling@example.test").orElseThrow().id());
        var state = de.zettsystems.starfare.testsupport.RecyclingStates.ready();
        state.seatByUser().put(host, 1); state.start();
        id = GameId.newId();
        store.save(new GameSession(id, "Recycling acceptance", host, Instant.now(), state));
        browser.open("/map/" + id.value()); selectAlpha();
        openDetails(); browser.clickCss("#recycling-open");
        enter("#recycling-ships input", "1"); browser.clickCss("#recycling-apply");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                registry.readState(id, current -> current.recyclingShips(1)) == 1);
        browser.awaitTextIn(".recycling-details", "Vorgemerkt: 1 Schiffe");
        openDetails(); browser.clickCss("#recycling-open");
        enter("#recycling-ships input", "0"); browser.clickCss("#recycling-apply");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                registry.readState(id, current -> current.recyclingShips(1)) == 0);
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10))
                .ignoring(org.openqa.selenium.StaleElementReferenceException.class).until(_ ->
                !browser.all(".recycling-details").getFirst().getText().contains("Vorgemerkt:"));
        assertThat(browser.all(".recycling-details").getFirst().getText()).doesNotContain("Vorgemerkt:");
        openDetails(); browser.clickCss("#recycling-open");
        enter("#recycling-ships input", "1"); browser.clickCss("#recycling-apply");
        browser.awaitTextIn(".recycling-details", "Vorgemerkt: 1 Schiffe");
        assertThat(registry.<Integer>readState(id, current -> current.getSystem(1).garrison())).isEqualTo(12);
        screenshot("recycling-plan");
        browser.driver().navigate().refresh(); selectAlpha();
        browser.awaitTextIn(".recycling-details", "Vorgemerkt: 1 Schiffe");
        assertThat(games.submitTurn(id, 1)).isTrue(); assertThat(games.submitTurn(id, 2)).isTrue();
        browser.driver().navigate().refresh(); selectAlpha();
        openDetails(); browser.awaitTextIn(".recycling-details", "Gespeicherter Metallrest: 0,7");
        assertThat(browser.all(".recycling-details").getFirst().getText()).doesNotContain("Vorgemerkt:");
        assertThat(registry.<Integer>readState(id, current -> current.getSystem(1).garrison())).isEqualTo(11);
        openDetails(); browser.clickCss("#recycling-open");
        enter("#recycling-ships input", "9"); browser.clickCss("#recycling-apply");
        browser.awaitTextIn(".recycling-details", "Vorgemerkt: 9 Schiffe");
        browser.awaitTextIn(".metal-details", "7 Punkte → 4 von 7 Schiffen/Runde · Metall fehlt");
        assertThat(games.submitTurn(id, 1)).isTrue(); assertThat(games.submitTurn(id, 2)).isTrue();
        browser.driver().navigate().refresh(); selectAlpha();
        openDetails(); browser.awaitTextIn(".recycling-details", "Gespeicherter Metallrest: 0,0");
        assertThat(registry.<Integer>readState(id, current -> current.getSystem(1).garrison())).isEqualTo(6);
        browser.driver().manage().window().setSize(new org.openqa.selenium.Dimension(1024, 768));
        openDetails(); browser.awaitTextIn(".recycling-details", "Gespeicherter Metallrest: 0,0");
        screenshot("recycling-credit");
        assertThat(games.submitTurn(id, 1)).isTrue(); assertThat(games.submitTurn(id, 2)).isTrue();
        assertThat(registry.<Integer>readState(id, current -> current.getSystem(1).garrison())).isEqualTo(6);
        registry.writeState(id, current -> { current.endGame(1); return null; });
        browser.driver().navigate().refresh(); browser.awaitCss(".review-controls");
        ((JavascriptExecutor) browser.driver()).executeScript("""
                const slider = document.querySelector('#review-timeline');
                slider.value = slider.min;
                slider.dispatchEvent(new Event('change', {bubbles:true}));
                """);
        browser.awaitTextIn(".review-notice", "Historischer Stand: vollständig aufgedeckt.");
        selectAlpha(); browser.awaitTextIn(".recycling-details", "Gespeicherter Metallrest: 0,7");
        assertThat(browser.driver().findElements(org.openqa.selenium.By.cssSelector("#recycling-ships"))).isEmpty();
        screenshot("recycling-history");
    }
    private void switchLanguage(String label, String expected) {
        browser.awaitCss(".lang-switcher input").sendKeys(Keys.chord(Keys.CONTROL, "a"), label);
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10))
                .ignoring(org.openqa.selenium.StaleElementReferenceException.class).until(_ ->
                        browser.all("vaadin-combo-box-item").stream()
                                .filter(item -> item.isDisplayed() && item.getText().strip().equals(label))
                                .findFirst().orElse(null)).click();
        browser.awaitTextIn(".app-header-stats", expected);
    }
    private int mining() {
        return registry.readState(id, current -> current.metals().orElseThrow().mining().get(1));
    }
    private void save(Runnable input, java.util.function.BooleanSupplier stored) {
        browser.saveAndAwaitRebuild(".industry-panel", input, stored);
    }
    private void assertFieldValue(String selector, String value) {
        Boolean matched = new WebDriverWait(browser.driver(), Duration.ofSeconds(10))
                .until(_ -> value.equals(browser.awaitCss(selector).getDomProperty("value")));
        assertThat(matched).isTrue();
    }
    private void enter(String selector, String value) {
        browser.awaitCss(selector).sendKeys(Keys.chord(Keys.CONTROL, "a"), value, Keys.TAB);
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
        openDetails();
    }
    private void openDetails() {
        ((JavascriptExecutor) browser.driver()).executeScript(
                "document.querySelectorAll('.industry-panel vaadin-details, .system-background').forEach(d => d.opened = true);");
    }
    private void screenshot(String name) throws IOException {
        if (browser.all(".production-editor[opened]").isEmpty()) {
            ((JavascriptExecutor) browser.driver()).executeScript("arguments[0].scrollIntoView({block:'center'});",
                    browser.awaitCss(".metal-details"));
        }
        Path target = Path.of("build", "metals", name + ".png"); Files.createDirectories(target.getParent());
        Files.write(target, ((TakesScreenshot) browser.driver()).getScreenshotAs(OutputType.BYTES));
    }
    @After public void cleanup() { if (id != null) { store.delete(id); } }
}
