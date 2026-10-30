package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.identity.values.AccountName;
import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.game.application.GameRegistry;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.application.GameSessionStore;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.testsupport.FleetUiStates;
import io.cucumber.java.After;
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

/** Real SVG highlighting, map geometry and route-card layout after the functional migration. */
public class NavigationLayoutSteps {
    private final Browser browser;
    private final GameRegistry registry;
    private final GameService games;
    private final GameSessionStore store;
    private final UserAccountService accounts;
    private final List<GameId> created = new ArrayList<>();

    public NavigationLayoutSteps(Browser browser, GameRegistry registry, GameService games,
                                 GameSessionStore store, UserAccountService accounts) {
        this.browser = browser;
        this.registry = registry;
        this.games = games;
        this.store = store;
        this.accounts = accounts;
    }

    @Wenn("ich Reichweitenkreis, Routenvorschau und Auftankkarten im Browser prüfe")
    public void navigationLayout() throws IOException {
        String email = "navigation-layout-" + UUID.randomUUID() + "@example.test";
        String host = String.valueOf(accounts.createAccount(email, Browser.PASSWORD,
                AccountName.display("Navigator"), true).id());
        var state = FleetUiStates.navigation();
        state.seatByUser().put(host, 1);
        GameId id = GameId.newId();
        created.add(id);
        store.save(new GameSession(id, "Navigation layout", host, Instant.now(), state));
        browser.logIn(email);
        browser.open("/map/" + id.value());
        browser.awaitCss("#map .sys-own");
        inspectPreview();
        assertThat(games.sendFleet(id, 1, 1, 4, 10)).isTrue();
        advance(id);
        int stop = registry.readState(id, current -> current.fleets().getFirst().arrivalTurn());
        while (games.viewFor(id, 1).turn() < stop) { advance(id); }
        browser.driver().navigate().refresh();
        browser.clickTabWithText("Flotten");
        browser.awaitTextIn(".fleet-travel-card", "Auftanken in Beta");
        browser.openDetailsWithText("Reisedetails");
        browser.awaitTextIn(".fleet-travel-card .route-strip", "Gamma");
        captureRefuelling();
    }

    private void inspectPreview() throws IOException {
        browser.clickMatching("#map .sys-own", system -> system.getText().startsWith("Alpha"), "Alpha");
        browser.clickButtonWithText("Flotte senden");
        browser.awaitCss(".map-range-circle");
        assertThat(js().executeScript("""
                const circle = document.querySelector('.map-range-circle');
                const zoom = Number(document.querySelector('#map').style.zoom || 1);
                return Math.abs(circle.getBoundingClientRect().width - 900 * zoom) < 1
                    && getComputedStyle(circle).pointerEvents === 'none';
                """)).isEqualTo(true);
        browser.captureUiAcceptance("navigation-range-circle");
        browser.clickMatching("#map .sys-own, #map .sys-fog", system -> system.getText().startsWith("Delta"), "Delta");
        browser.awaitCss("#route-preview");
        String amount = browser.awaitCss(".send-fleet-amount input").getDomProperty("value");
        browser.clickCss(".send-fleet-route [data-route-system='2']");
        browser.awaitCss(".send-fleet-route .route-selected");
        assertThat(browser.awaitCss(".send-fleet-amount input").getDomProperty("value")).isEqualTo(amount);
        browser.captureUiAcceptance("navigation-route-preview");
        browser.clickButtonWithText("Abbrechen");
        browser.awaitNoneDisplayed(".send-fleet-dialog[opened]");
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> browser.all("#route-preview").isEmpty());
    }

    private void captureRefuelling() throws IOException {
        Dimension original = browser.driver().manage().window().getSize();
        try {
            for (int width : new int[]{1366, 1024}) {
                browser.driver().manage().window().setSize(new Dimension(width, 768));
                js().executeScript("""
                        const card = [...document.querySelectorAll('.fleet-travel-card')]
                            .find(element => element.querySelector('.fleet-travel-card-top span')?.textContent === '✦ #1');
                        card?.scrollIntoView({block:'center'});
                        """);
                browser.assertNoSidewaysScrolling();
                browser.captureUiAcceptance("navigation-refuel-" + width);
            }
        } finally {
            browser.driver().manage().window().setSize(original);
        }
        browser.assertNoBrowserErrors();
    }

    private void advance(GameId id) {
        assertThat(games.submitTurn(id, 1)).isTrue();
        assertThat(games.submitTurn(id, 2)).isTrue();
    }

    private JavascriptExecutor js() {
        if (browser.driver() instanceof JavascriptExecutor executor) { return executor; }
        throw new IllegalStateException("Browser does not support JavaScript");
    }

    @After public void cleanup() { created.forEach(store::delete); }
}
