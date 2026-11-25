package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.starfare.testsupport.TestPlatformAccounts;
import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.game.application.GameSessionStore;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.testsupport.EconomyUiStates;
import io.cucumber.java.After;
import io.cucumber.java.de.Wenn;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Retains geometry, keyboard activation and acceptance pictures from migrated economy journeys. */
public class EconomyLayoutSteps {
    private final Browser browser;
    private final TestPlatformAccounts accounts;
    private final GameSessionStore store;
    private final List<GameId> created = new ArrayList<>();

    public EconomyLayoutSteps(Browser browser, TestPlatformAccounts accounts, GameSessionStore store) {
        this.browser = browser;
        this.accounts = accounts;
        this.store = store;
    }

    @Wenn("ich die Industrie bei beiden Desktopbreiten prüfe")
    public void economyLayouts() throws IOException {
        String email = "economy-layout-" + UUID.randomUUID() + "@example.test";
        String host = String.valueOf(accounts.createAccount(email, "Economy Layout").id());
        browser.logIn(email);
        GameId industry = save(EconomyUiStates.industry(), host);
        Dimension original = browser.driver().manage().window().getSize();
        try {
            for (int width : new int[]{1366, 1024}) {
                browser.driver().manage().window().setSize(new Dimension(width, 768));
                checkIndustry(industry, width);
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

    private void checkIndustry(GameId id, int width) throws IOException {
        openAlpha(id);
        browser.awaitTextIn(".industry-panel", "Industrie");
        js().executeScript("arguments[0].scrollIntoView({block:'start'});", browser.awaitCss(".industry-panel"));
        // The expansion control is always visible; no separate editor has to be opened.
        browser.awaitCss("#industry-expansion input");
        browser.awaitTextIn(".industry-panel .production-result", "Schiffe/Runde");
        capture("industry", width);
    }

    private void openAlpha(GameId id) {
        browser.open(Browser.LOBBY + "/map/" + id.value());
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
