package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.identity.values.AccountName;
import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.game.application.BattleAcknowledgementService;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.application.GameSessionStore;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.social.application.UserPreferencesService;
import de.zettsystems.starfare.social.values.DisplaySetting;
import de.zettsystems.starfare.testsupport.FleetUiStates;
import io.cucumber.java.After;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Only browser animation, side geometry, initial colours and final loss counters remain here. */
public class CoalitionRenderingSteps {
    private final Browser browser;
    private final GameService games;
    private final GameSessionStore store;
    private final UserAccountService accounts;
    private final UserPreferencesService preferences;
    private final BattleAcknowledgementService acknowledgements;
    private final List<GameId> created = new ArrayList<>();

    public CoalitionRenderingSteps(Browser browser, GameService games, GameSessionStore store,
                                    UserAccountService accounts, UserPreferencesService preferences,
                                    BattleAcknowledgementService acknowledgements) {
        this.browser = browser;
        this.games = games;
        this.store = store;
        this.accounts = accounts;
        this.preferences = preferences;
        this.acknowledgements = acknowledgements;
    }

    @Wenn("zwei aufgelöste Bündnisschlachten für die Browser-Wiedergabe bereitstehen")
    public void prepareResolvedBattles() {
        String email = "coalition-rendering-" + UUID.randomUUID() + "@example.test";
        String host = String.valueOf(accounts.createAccount(email, Browser.PASSWORD,
                AccountName.display("Commander"), true).id());
        preferences.chooseDisplaySetting(host, DisplaySetting.SOUND, "false");
        preferences.chooseDisplaySetting(host, DisplaySetting.SPEED, "1.0");
        var state = FleetUiStates.coalition();
        state.seatByUser().put(host, 1);
        int arrival = state.turn() + state.travelRounds(1, 3);
        GameId id = GameId.newId();
        created.add(id);
        store.save(new GameSession(id, "Coalition rendering", host, Instant.now(), state));
        assertThat(games.sendFleet(id, 1, 1, 1, 3, 40, 2)).isTrue();
        while (games.viewFor(id, 1).turn() < arrival) {
            assertThat(games.submitTurn(id, 1)).isTrue();
            assertThat(games.submitTurn(id, 2)).isTrue();
            assertThat(games.submitTurn(id, 3)).isTrue();
        }
        browser.logIn(email);
        browser.open("/map/" + id.value());
        browser.awaitCss(".map-content");
        browser.clickTabWithText("Bericht");
        browser.awaitCss(".event-battle-pending");
    }

    @Dann("zeigt die Bündnisanimation neutrale Startfarben, getrennte Seiten und die gespeicherten Restschiffe")
    public void replayRendering() throws IOException {
        for (int index = 0; index < 2; index++) {
            browser.clickCss(".event-battle-pending");
            browser.awaitCss(".coalition-replay");
            assertSideLayout(index);
            browser.awaitCss(".battle-replay-result-visible");
            assertRemainingShips(index);
            capture(index);
            browser.clickButtonWithText("Ergebnis übernehmen");
            if (index == 0) {
                new WebDriverWait(browser.driver(), Duration.ofSeconds(10))
                        .until(_ -> browser.all(".event-battle-pending").size() == 1);
            }
        }
        browser.awaitText("Sieg:");
        browser.clickButtonWithText("Nachbetrachtung öffnen");
        browser.awaitCss(".review-controls");
        browser.assertNoBrowserErrors();
    }

    private void assertSideLayout(int index) {
        assertThat(browser.all(".coalition-side")).hasSize(index == 0 ? 2 : 3);
        assertThat(browser.all(".coalition-defenders .coalition-side")).hasSize(1);
        assertThat(browser.all(".coalition-attackers .coalition-side")).hasSize(index == 0 ? 1 : 2);
        assertThat(js().executeScript("""
                const a = document.querySelector('.coalition-attackers').getBoundingClientRect();
                const d = document.querySelector('.coalition-defenders').getBoundingClientRect();
                return a.left < d.left && [...document.querySelectorAll('.coalition-side')]
                    .every(card => getComputedStyle(card).color === 'rgb(255, 216, 106)');
                """)).isEqualTo(true);
        browser.awaitTextIn(".coalition-side", "Commander + Partner");
        assertThat(browser.awaitCss(".battle-replay-dialog vaadin-checkbox").getDomProperty("checked")).isEqualTo("false");
    }

    private void assertRemainingShips(int index) {
        var battles = games.viewFor(created.getFirst(), 1).report().events().stream()
                .filter(TurnEvent.CoalitionBattle.class::isInstance).map(TurnEvent.CoalitionBattle.class::cast).toList();
        for (var side : battles.get(index).sides()) {
            String count = "[data-coalition-side='" + side.id() + "'] [data-coalition-count]";
            browser.awaitTexts(count, List.of(Integer.toString(side.remaining()))::equals, "Remaining ships of side " + side.id());
        }
    }

    private void capture(int index) throws IOException {
        browser.captureUiAcceptance("coalition-battle-" + index);
        if (index != 1) { return; }
        Dimension original = browser.driver().manage().window().getSize();
        try {
            browser.driver().manage().window().setSize(new Dimension(1024, 768));
            browser.assertNoSidewaysScrolling();
            browser.captureUiAcceptance("coalition-battle-1024");
        } finally {
            browser.driver().manage().window().setSize(original);
        }
    }

    private JavascriptExecutor js() {
        if (browser.driver() instanceof JavascriptExecutor executor) { return executor; }
        throw new IllegalStateException("Browser does not support JavaScript");
    }

    @After public void cleanup() {
        created.forEach(id -> { acknowledgements.forgetGame(id); store.delete(id); });
    }
}
