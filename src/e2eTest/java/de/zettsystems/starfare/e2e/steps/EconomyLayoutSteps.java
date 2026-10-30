package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.identity.values.AccountName;
import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.economy.application.DefaultEconomyService;
import de.zettsystems.starfare.game.application.GameSessionStore;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.testsupport.ColonyStates;
import de.zettsystems.starfare.testsupport.EconomyUiStates;
import io.cucumber.java.After;
import io.cucumber.java.de.Wenn;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Retains geometry, keyboard activation and acceptance pictures from migrated economy journeys. */
public class EconomyLayoutSteps {
    private final Browser browser;
    private final UserAccountService accounts;
    private final GameSessionStore store;
    private final List<GameId> created = new ArrayList<>();

    public EconomyLayoutSteps(Browser browser, UserAccountService accounts, GameSessionStore store) {
        this.browser = browser;
        this.accounts = accounts;
        this.store = store;
    }

    @Wenn("ich Mondkolonie, Wirtschaftsplanung und Forschung bei beiden Desktopbreiten prüfe")
    public void economyLayouts() throws IOException {
        String email = "economy-layout-" + UUID.randomUUID() + "@example.test";
        String host = String.valueOf(accounts.createAccount(email, Browser.PASSWORD,
                AccountName.display("Economy Layout"), true).id());
        browser.logIn(email);
        GameId colony = save(ColonyStates.readyWithMoon(), host);
        GameId research = save(mixedResearch(), host);
        Dimension original = browser.driver().manage().window().getSize();
        try {
            for (int width : new int[]{1366, 1024}) {
                browser.driver().manage().window().setSize(new Dimension(width, 768));
                checkColony(colony, width);
                checkIndustry(research, width);
            }
        } finally {
            browser.driver().manage().window().setSize(original);
        }
    }

    private GameId save(GameState state, String host) {
        state.seatByUser().put(host, 1);
        if (!state.started()) { state.start(); }
        GameId id = GameId.newId();
        created.add(id);
        store.save(new GameSession(id, "Economy layout", host, Instant.now(), state));
        return id;
    }

    private static GameState mixedResearch() {
        var state = EconomyUiStates.research();
        var economy = new DefaultEconomyService();
        state.planShipResearch(1, 25);
        economy.produce(state, Map.of());
        state.nextTurn();
        state.planShipResearch(1, 0);
        economy.produce(state, Map.of());
        state.nextTurn();
        return state;
    }

    private void checkColony(GameId id, int width) throws IOException {
        openAlpha(id);
        var moon = browser.awaitCss(".system-colony-moon");
        assertThat(moon.getRect().getWidth()).isGreaterThanOrEqualTo(44);
        browser.type(".system-colony-moon", Keys.ENTER);
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> Boolean.TRUE.equals(js().executeScript(
                "return document.querySelector('.system-operations')?.opened")));
        browser.awaitTextIn(".colony-details", "Hauptkolonie auf einem Mond");
        js().executeScript("arguments[0].scrollIntoView({block:'center'});", browser.awaitCss(".colony-details"));
        capture("colony", width);
    }

    private void checkIndustry(GameId id, int width) throws IOException {
        openAlpha(id);
        js().executeScript("document.querySelectorAll('.industry-panel vaadin-details, .system-background').forEach(d => d.opened=true)");
        browser.awaitTextIn(".industry-panel", "Stufe 0:");
        browser.awaitTextIn(".industry-panel", "Stufe 1:");
        js().executeScript("arguments[0].scrollIntoView({block:'start'});", browser.awaitCss(".industry-panel"));
        capture("research-mixed", width);
        browser.clickCss("#production-plan-open");
        browser.awaitCss("#industry-mining input");
        capture("production-editor", width);
        browser.awaitCss(".app-header-menu vaadin-menu-bar-button").click();
        browser.clickMatching("vaadin-menu-bar-item", item -> item.getText().strip().equals("Schiffsforschung"), "research");
        browser.awaitCss("#research-budget input");
        capture("research-dialog", width);
        browser.clickButtonWithText("Schließen");
    }

    private void openAlpha(GameId id) {
        browser.open("/map/" + id.value());
        browser.awaitCss("#map .sys-own");
        browser.clickMatching("#map .sys-own", system -> system.getText().startsWith("Alpha"), "Alpha");
        browser.awaitTextIn(".system-details-heading", "Alpha");
    }

    private JavascriptExecutor js() {
        if (browser.driver() instanceof JavascriptExecutor executor) { return executor; }
        throw new IllegalStateException("Browser does not support JavaScript");
    }

    private void capture(String name, int width) throws IOException {
        browser.assertNoSidewaysScrolling();
        browser.assertNoBrowserErrors();
        browser.captureUiAcceptance("layout-economy-" + name + "-" + width);
    }

    @After
    public void cleanup() {
        created.forEach(store::delete);
    }
}
