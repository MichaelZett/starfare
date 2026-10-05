package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.starfare.diplomacy.values.*;
import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.game.application.*;
import de.zettsystems.starfare.game.domain.*;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.navigation.values.NavigationSettings;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.social.application.UserPreferencesService;
import de.zettsystems.starfare.social.values.DisplaySetting;
import io.cucumber.java.After;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import static org.assertj.core.api.Assertions.*;

/** A real resolved round covers claim selection, coalition playback and durable acknowledgements. */
public class CoalitionSteps {
    private final Browser browser;
    private final GameService games;
    private final GameRegistry registry;
    private final GameSessionStore store;
    private final UserAccountService accounts;
    private final UserPreferencesService preferences;
    private final BattleAcknowledgementService acknowledgements;
    private String host;
    private GameId id;
    private int arrival;
    public CoalitionSteps(Browser browser, GameService games, GameRegistry registry, GameSessionStore store,
                          UserAccountService accounts, UserPreferencesService preferences, BattleAcknowledgementService acknowledgements) {
        this.browser = browser; this.games = games; this.registry = registry; this.store = store;
        this.accounts = accounts; this.preferences = preferences;
        this.acknowledgements = acknowledgements;
    }
    @Wenn("eine interne Mehrparteienpartie für {string} bereitsteht")
    public void create(String email) {
        host = String.valueOf(accounts.findByEmail(email).orElseThrow().id());
        preferences.chooseDisplaySetting(host, DisplaySetting.SOUND, "false");
        preferences.chooseDisplaySetting(host, DisplaySetting.SPEED, "1.0");
        GameState state = new GameState(); state.resetForNewGame(); state.rememberSetup(GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD));
        state.configureLobby(true, true, true, 0);
        state.players().addAll(List.of(new Player(1, "Commander", false, "#56b4e9"),
                new Player(2, "Partner", false, "#d55e00"), new Player(3, "Opponent", false, "#009e73")));
        state.systems().addAll(List.of(new StarSystem(1, "Alpha", 400, 350, 1, 200, 4, false),
                new StarSystem(2, "Beta", 850, 350, 2, 100, 4, false), new StarSystem(3, "Gamma", 1300, 350, 3, 20, 4, false),
                new StarSystem(4, "Delta", 1750, 350, null, 30, 4, true)));
        for (int index = 5; index <= 8; index++) { state.systems().add(new StarSystem(index, "Remote " + index, 2500 + index * 60, 1300 + index * 50, 1, 20, 4, false)); }
        state.joinedHumanPlayerIds().addAll(List.of(1, 2, 3)); state.originalHumanPlayerIds().addAll(List.of(1, 2, 3)); state.seatByUser().put(host, 1);
        state.players().forEach(player -> state.intel().put(player.id(), new HashMap<>()));
        state.configureNavigation(new NavigationSettings(1500)); state.start();
        state.agreeTreaties(new DiplomacyState(2, List.of(new AllianceGroup(1, Set.of(1, 2), 3, Map.of())), List.of()));
        arrival = state.turn() + state.travelRounds(1, 3);
        state.fleets().addAll(List.of(new Fleet(100, 2, 1, 2, 3, 40, 1, arrival),
                new Fleet(101, 1, 2, 1, 4, 40, 1, arrival), new Fleet(102, 2, 2, 2, 4, 20, 1, arrival),
                new Fleet(103, 3, 1, 3, 4, 10, 1, arrival)));
        id = GameId.newId(); store.save(new GameSession(id, "Coalition acceptance", host, Instant.now(), state));
        browser.open("/map/" + id.value()); browser.awaitCss(".map-content");
    }
    @Dann("kann ich den Verbündeten vor dem Flottenversand als Eroberungsziel wählen")
    public void chooseBeneficiary() throws IOException {
        selectSystem("Alpha"); browser.awaitTextIn(".system-details-heading", "Alpha"); browser.clickButtonWithText("Flotte senden");
        browser.awaitCss(".sys-selected"); selectSystem("Gamma");
        browser.awaitCss(".send-fleet-dialog");
        browser.awaitCss(".send-fleet-amount input").sendKeys(Keys.chord(Keys.CONTROL, "a"), "40", Keys.TAB);
        browser.awaitCss(".conquest-selection vaadin-combo-box input").sendKeys(Keys.chord(Keys.CONTROL, "a"), "Partner", Keys.ENTER, Keys.TAB);
        browser.awaitTextIn(".conquest-selection", "Schiffe behalten ihren Eigentümer"); screenshot("claim");
        browser.clickCss(".send-fleet-dialog vaadin-button[theme~='primary']");
        waitFor(() -> games.viewFor(id, 1).plannedOrders().size() == 1);
        browser.awaitTextIn(".fleet-travel-card-planned", "Erobern für: Partner");
        assertThat(games.viewFor(id, 1).plannedOrders().getFirst().beneficiaryId()).isEqualTo(2);
        browser.driver().navigate().refresh(); browser.clickTabWithText("Befehle");
        browser.awaitTextIn(".fleet-travel-card-planned", "Erobern für: Partner");
    }
    @Wenn("beide gemeinsamen Schlachten die Partie beenden")
    public void resolve() {
        while (games.viewFor(id, 1).turn() < arrival) {
            assertThat(games.submitTurn(id, 1)).isTrue(); assertThat(games.submitTurn(id, 2)).isTrue(); assertThat(games.submitTurn(id, 3)).isTrue();
        }
        assertThat(games.viewFor(id, 1).gameOver()).isTrue();
        assertThat(registry.readState(id, state -> state.getSystem(3).ownerId()).intValue()).isEqualTo(2);
        assertThat(games.viewFor(id, 1).report().events().stream().filter(event -> event instanceof TurnEvent.CoalitionBattle).count()).isEqualTo(2);
    }
    @Dann("bleiben Unterstützung und Endergebnis bis zur persönlichen Auswertung verdeckt")
    public void conceal() {
        browser.driver().navigate().refresh(); browser.awaitCss(".map-content"); browser.clickTabWithText("Bericht"); browser.awaitCss(".event-battle-pending");
        assertThat(browser.all(".event-battle-pending")).hasSize(2); assertThat(browser.all(".event-victory")).isEmpty();
        browser.clickTabWithText("Flotten");
        assertThat(browser.textsOf(".fleet-travel-card")).noneMatch(text -> text.contains("Unterstützung in Gamma"));
        assertThat(browser.all(".fleet-badge")).isEmpty();
        browser.clickTabWithText("Bericht");
    }
    @Dann("zeigt die stumme Wiedergabe alle Seiten und ihre gespeicherten Verluste")
    public void playback() throws IOException {
        for (int index = 0; index < 2; index++) {
            browser.awaitCss(".event-battle-pending").click(); browser.awaitCss(".coalition-replay");
            assertThat(browser.all(".coalition-side")).hasSize(index == 0 ? 2 : 3);
            assertThat(browser.textsOf(".coalition-side")).anyMatch(text -> text.contains("Commander + Partner"));
            assertThat(browser.awaitCss(".battle-replay-dialog vaadin-checkbox").getDomProperty("checked")).isEqualTo("false");
            browser.awaitCss(".battle-replay-result-visible");
            var report = games.viewFor(id, 1).report();
            var battle = (TurnEvent.CoalitionBattle) report.events().stream().filter(event -> event instanceof TurnEvent.CoalitionBattle).toList().get(index);
            for (var side : battle.sides()) {
                assertThat(browser.awaitCss("[data-coalition-side='" + side.id() + "'] [data-coalition-count]").getText()).isEqualTo(Integer.toString(side.remaining()));
            }
            screenshot("battle-" + index);
            if (index == 1) {
                var size = browser.driver().manage().window().getSize();
                try { browser.driver().manage().window().setSize(new Dimension(1024, 768)); screenshot("battle-1024"); }
                finally { browser.driver().manage().window().setSize(size); }
            }
            browser.clickButtonWithText("Ergebnis übernehmen");
            if (index == 0) {
                waitFor(() -> browser.all(".event-battle-pending").size() == 1);
                assertThat(browser.all(".event-victory")).isEmpty();
                // Der Bericht rendert nach Neuladen und Reiterwechsel erst mit der nächsten Server-Antwort.
                browser.driver().navigate().refresh(); browser.awaitCss(".map-content"); browser.clickTabWithText("Bericht");
                waitFor(() -> browser.all(".event-battle-pending").size() == 1);
                assertThat(browser.all(".event-battle-pending")).hasSize(1);
            }
        }
        browser.awaitText("Sieg:"); browser.clickButtonWithText("Nachbetrachtung öffnen"); browser.awaitCss(".review-controls");
    }
    @Dann("bleiben bestätigte Schlachten nach Neuladen bestätigt")
    public void confirmationsPersist() {
        browser.driver().navigate().refresh(); browser.awaitCss(".map-content"); browser.clickTabWithText("Bericht");
        assertThat(browser.all(".event-battle-pending")).isEmpty();
        var report = games.viewFor(id, 1).report();
        var confirmed = acknowledgements.acknowledgedEventIndices(host, id, report.turn());
        for (int index = 0; index < report.events().size(); index++) {
            if (report.events().get(index) instanceof TurnEvent.CoalitionBattle) { assertThat(confirmed).contains(index); }
        }
        // The archive's final state has no report; select the preceding resolved round.
        browser.clickButtonWithText("Zurück");
        browser.awaitTextIn(".event-coalition-battle", "Besitzer: Partner");
        assertThat(registry.readState(id, state -> state.fleets().stream().anyMatch(fleet -> fleet.ownerId() == 1 && fleet.toSystemId() == 3)).booleanValue()).isTrue();
    }
    private void selectSystem(String name) {
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> Boolean.TRUE.equals(((JavascriptExecutor) browser.driver()).executeScript("const s=[...document.querySelectorAll('#map .sys-own, #map .sys-fog')].find(e=>e.textContent.startsWith(arguments[0])); if(!s)return false;s.click();return true;", name)));
    }
    private void waitFor(java.util.function.BooleanSupplier condition) { new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> condition.getAsBoolean()); }
    private void screenshot(String name) throws IOException {
        Path target = Path.of("build", "coalition", name + ".png"); Files.createDirectories(target.getParent());
        Files.write(target, ((TakesScreenshot) browser.driver()).getScreenshotAs(OutputType.BYTES));
    }
    @After public void cleanup() { if (id != null) { acknowledgements.forgetGame(id); store.delete(id); } }
}
