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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

/** Internal Spaceward fixture; both browsers are muted by SharedBrowser. */
public class DiplomacySteps {
    private final Browser browser;
    private final GameService games;
    private final GameRegistry registry;
    private final GameSessionStore store;
    private final UserAccountService accounts;
    private Browser partner;
    private GameId id;
    public DiplomacySteps(Browser browser, GameService games, GameRegistry registry, GameSessionStore store, UserAccountService accounts) {
        this.browser = browser; this.games = games; this.registry = registry; this.store = store; this.accounts = accounts;
    }
    @Wenn("zwei Imperien die interne Bündnispartie für {string} und {string} öffnen")
    public void create(String hostEmail, String partnerEmail) {
        String host = String.valueOf(accounts.findByEmail(hostEmail).orElseThrow().id());
        String guest = String.valueOf(accounts.findByEmail(partnerEmail).orElseThrow().id());
        GameState state = new GameState(); state.resetForNewGame(); state.rememberSetup(GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD));
        state.players().addAll(List.of(new Player(1, "Diplomat", false, "#56b4e9"), new Player(2, "Partner", false, "#d55e00")));
        state.systems().addAll(List.of(new StarSystem(1, "Alpha", 400, 350, 1, 50, 4, false),
                new StarSystem(2, "Beta", 850, 350, 2, 5, 4, false), new StarSystem(3, "Gamma", 1300, 350, 1, 5, 4, false),
                new StarSystem(4, "Delta", 1750, 350, 2, 100, 4, false)));
        for (int index = 5; index <= 8; index++) { state.systems().add(new StarSystem(index, "Remote " + index, 2500 + index * 60, 1300 + index * 50, null, 200, 4, true)); }
        state.joinedHumanPlayerIds().addAll(List.of(1, 2)); state.originalHumanPlayerIds().addAll(List.of(1, 2));
        state.seatByUser().put(host, 1); state.seatByUser().put(guest, 2);
        state.intel().put(1, new java.util.HashMap<>()); state.intel().put(2, new java.util.HashMap<>());
        state.configureNavigation(new NavigationSettings(450)); state.start();
        id = GameId.newId(); store.save(new GameSession(id, "Spaceward treaty test", host, Instant.now(), state));
        browser.open("/map/" + id.value()); browser.awaitCss(".map-content");
        partner = browser.newSession(); partner.logIn(partnerEmail); partner.open("/map/" + id.value()); partner.awaitCss(".map-content");
        openTreaties(browser); openTreaties(partner);
    }
    @Dann("erfordert die Bündnisgründung die Zustimmung des Partners")
    public void unanimousFounding() throws IOException {
        choosePartner(); browser.clickButtonWithText("Bündnis vorschlagen");
        partner.awaitTextIn(".diplomacy-content", "Neues Bündnis");
        assertThat(registry.readState(id, state -> state.allied(1, 2)).booleanValue()).isFalse();
        partner.clickButtonWithText("Ablehnen / zurückziehen");
        waitFor(() -> registry.readState(id, state -> state.diplomacy().proposals().isEmpty()).booleanValue());
        browser.driver().navigate().refresh(); openTreaties(browser);
        choosePartner(); browser.clickButtonWithText("Bündnis vorschlagen");
        partner.awaitTextIn(".diplomacy-content", "Neues Bündnis"); partner.clickButtonWithText("Zustimmen");
        browser.awaitTextIn(".diplomacy-content", "Kündigungsfrist: 3 Runden");
        assertThat(registry.readState(id, state -> state.allied(1, 2)).booleanValue()).isTrue();
        screenshot("group");
    }
    @Dann("vereinbaren beide eine neue Kündigungsfrist ohne gemeinsame Schiffseigentümer")
    public void changeNoticeAndDock() {
        WebElement notice = browser.awaitCss(".diplomacy-card vaadin-integer-field input");
        notice.sendKeys(Keys.chord(Keys.CONTROL, "a"), "0", Keys.TAB); browser.clickButtonWithText("Neue Frist vorschlagen");
        partner.awaitTextIn(".diplomacy-content", "Frist von 0 Runden"); partner.clickButtonWithText("Zustimmen");
        browser.awaitTextIn(".diplomacy-content", "Kündigungsfrist: 0 Runden");
        browser.clickButtonWithText("Schließen"); partner.clickButtonWithText("Schließen");
        assertThat(registry.writeState(id, state -> new DefaultFleetService().sendFleet(state, 1, 1, 2, 10)).booleanValue()).isTrue();
        int arrival = registry.readState(id, state -> state.fleets().getFirst().arrivalTurn());
        while (games.viewFor(id, 1).turn() < arrival) { advance(); }
        assertThat(registry.readState(id, state -> state.fleets().getFirst().journey().stationed()).booleanValue()).isTrue();
        assertThat(registry.readState(id, state -> state.getSystem(2).garrison()).intValue()).isEqualTo(21);
        openTreaties(browser); openTreaties(partner);
    }
    @Dann("bleibt die sichtbare Kündigung nach Neuladen erhalten und löst eine Heimreise aus")
    public void leaveAndReturn() throws IOException {
        int effective = games.viewFor(id, 1).turn() + 1; browser.clickButtonWithText("Bündnis kündigen");
        String text = "Diplomat scheidet zu Beginn von Runde " + effective + " aus.";
        partner.awaitTextIn(".diplomacy-content", text);
        browser.driver().navigate().refresh(); openTreaties(browser); browser.awaitTextIn(".diplomacy-content", text);
        screenshot("notice");
        var size = browser.driver().manage().window().getSize();
        try { browser.driver().manage().window().setSize(new Dimension(1024, 768)); screenshot("notice-1024"); }
        finally { browser.driver().manage().window().setSize(size); }
        browser.clickButtonWithText("Schließen"); partner.clickButtonWithText("Schließen"); advance();
        assertThat(registry.readState(id, state -> state.allied(1, 2)).booleanValue()).isFalse();
        advance(); browser.driver().navigate().refresh(); clickFleets();
        browser.awaitTextIn(".fleet-travel-card", "Automatische Heimreise"); screenshot("return");
        int home = registry.readState(id, state -> state.fleets().getFirst().arrivalTurn());
        while (games.viewFor(id, 1).turn() < home) { advance(); }
        assertThat(registry.readState(id, state -> state.fleets().isEmpty()).booleanValue()).isTrue();
        assertThat(registry.readState(id, state -> state.getSystem(2).ownerId()).intValue()).isEqualTo(2);
    }
    private void choosePartner() { browser.awaitCss(".diplomacy-card vaadin-combo-box input").sendKeys("Partner", Keys.ENTER, Keys.TAB); }
    private static void openTreaties(Browser target) { target.clickButtonWithText("Bündnisse"); target.awaitCss(".diplomacy-content"); }
    private void advance() { assertThat(games.submitTurn(id, 1)).isTrue(); assertThat(games.submitTurn(id, 2)).isTrue(); }
    private void waitFor(java.util.function.BooleanSupplier condition) { new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> condition.getAsBoolean()); }
    private void clickFleets() {
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> Boolean.TRUE.equals(((JavascriptExecutor) browser.driver()).executeScript("const t=[...document.querySelectorAll('vaadin-tab')].find(t=>t.textContent.trim()==='Flotten');if(!t)return false;t.click();return true;")));
    }
    private void screenshot(String name) throws IOException {
        if (name.equals("return")) { ((JavascriptExecutor) browser.driver()).executeScript("document.querySelector('.fleet-travel-card')?.scrollIntoView({block:'center'});"); }
        Path target = Path.of("build", "diplomacy", name + ".png"); Files.createDirectories(target.getParent());
        Files.write(target, ((TakesScreenshot) browser.driver()).getScreenshotAs(OutputType.BYTES));
    }
    @After public void cleanup() { if (partner != null) { partner.closeSession(); } if (id != null) { store.delete(id); } }
}
