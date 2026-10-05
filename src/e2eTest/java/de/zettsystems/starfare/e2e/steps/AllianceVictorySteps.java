package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.starfare.diplomacy.values.*;
import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.game.application.*;
import de.zettsystems.starfare.game.domain.*;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.social.application.UserPreferencesService;
import de.zettsystems.starfare.social.values.DisplaySetting;
import io.cucumber.java.After;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openqa.selenium.*;
import static org.assertj.core.api.Assertions.*;

public class AllianceVictorySteps {
    private final Browser browser;
    private final GameService games;
    private final GameSessionStore store;
    private final UserAccountService accounts;
    private final UserPreferencesService preferences;
    private GameId id;
    public AllianceVictorySteps(Browser browser, GameService games, GameSessionStore store,
                                UserAccountService accounts, UserPreferencesService preferences) {
        this.browser = browser; this.games = games; this.store = store; this.accounts = accounts; this.preferences = preferences;
    }
    @Dann("prüfe ich die getrennten Bündnisoptionen in der internen Wizard-Vorschau")
    public void options() throws java.io.IOException {
        browser.open("/acceptance/victory-wizard"); browser.clickButtonWithText("Wizard");
        browser.awaitCss(".wizard-advanced");
        ((JavascriptExecutor) browser.driver()).executeScript("document.querySelector('.wizard-advanced').opened = true;");
        assertThat(browser.awaitCss("#alliances-allowed").getAttribute("disabled")).isNotNull();
        ((JavascriptExecutor) browser.driver()).executeScript("const radios=[...document.querySelectorAll('#ruleset-selection vaadin-radio-button')]; radios.find(r=>r.textContent.includes('Spaceward')).click();");
        awaitEnabled("#alliance-victory-allowed", true);
        browser.clickCss("#alliance-victory-allowed");
        awaitEnabled("#alliance-victory-percent", true);
        browser.clickCss("#alliances-allowed");
        awaitEnabled("#alliance-victory-allowed", false);
        browser.clickCss("#alliances-allowed");
        awaitEnabled("#alliance-victory-allowed", true);
        browser.clickCss("#alliance-victory-allowed");
        awaitEnabled("#alliance-victory-percent", true);
        Path target = Path.of("build", "alliance-victory", "wizard.png"); Files.createDirectories(target.getParent());
        ((JavascriptExecutor) browser.driver()).executeScript("arguments[0].scrollIntoView({block:'center'});", browser.awaitCss("#alliances-allowed"));
        Files.write(target, ((TakesScreenshot) browser.driver()).getScreenshotAs(OutputType.BYTES));
        browser.clickButtonWithText("Zusammenfassung prüfen");
        browser.awaitText("Bündnissiege: aktiv (70%)");
        browser.open("/");
    }
    private void awaitEnabled(String selector, boolean enabled) {
        Boolean matched = new org.openqa.selenium.support.ui.WebDriverWait(browser.driver(), java.time.Duration.ofSeconds(30))
                .until(_ -> (browser.awaitCss(selector).getAttribute("disabled") == null) == enabled);
        assertThat(matched).isTrue();
    }
    @Wenn("eine interne Bündnissiegpartie für {string} endet")
    public void finish(String email) {
        String host = String.valueOf(accounts.findByEmail(email).orElseThrow().id());
        preferences.chooseDisplaySetting(host, DisplaySetting.SOUND, "false");
        var state = new GameState(); state.resetForNewGame();
        state.rememberSetup(GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD_ALLIANCE)
                .chooseVictoryRules(new VictoryRules(70, true, true, 70)));
        state.players().addAll(List.of(new Player(1, "Commander", false, "#56b4e9"),
                new Player(2, "Partner", false, "#d55e00"), new Player(3, "Opponent", true, "#009e73")));
        for (int system = 1; system <= 16; system++) {
            state.systems().add(new StarSystem(system, "S" + system, system * 100, 100, system <= 11 ? 1 : 2, 5, 2, false));
        }
        state.seatByUser().put(host, 1); state.joinedHumanPlayerIds().addAll(Set.of(1, 2));
        state.originalHumanPlayerIds().addAll(Set.of(1, 2));
        state.players().forEach(p -> state.intel().put(p.id(), new HashMap<>())); state.start();
        state.agreeTreaties(new DiplomacyState(2, List.of(new AllianceGroup(1, Set.of(1, 2), 3, Map.of())), List.of()));
        id = GameId.newId(); store.save(new GameSession(id, "Alliance victory acceptance", host, Instant.now(), state));
        browser.open("/map/" + id.value());
        browser.awaitCss(".map-content");
        assertThat(games.submitTurn(id, 1)).isTrue(); assertThat(games.submitTurn(id, 2)).isTrue();
        assertThat(games.viewFor(id, 1).outcome().winnerIds()).containsExactly(1, 2);
    }
    @Dann("zeigt die Oberfläche beide Sieger auch nach Neuladen")
    public void outcome() throws java.io.IOException {
        browser.awaitTextIn(".game-outcome-winner", "Commander, Partner");
        Path target = Path.of("build", "alliance-victory", "outcome.png");
        Files.write(target, ((TakesScreenshot) browser.driver()).getScreenshotAs(OutputType.BYTES));
        assertThat(browser.awaitCss(".game-outcome-personal-result").getText()).contains("Sieg");
        browser.driver().navigate().refresh();
        browser.awaitText("Spiel beendet. Sieger: Bündnis 1: Commander, Partner");
        assertThat(browser.awaitCss(".review-controls")).isNotNull();
    }
    @Dann("zeigt das Archiv den gemeinsamen Sieg")
    public void archive() {
        browser.open("/archive"); browser.awaitText("Alliance victory acceptance");
        assertThat(browser.driver().getPageSource()).contains("Commander, Partner");
    }
    @After public void cleanup() { if (id != null) { store.delete(id); } }
}
