package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.identity.values.AccountName;
import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.game.application.GameRegistry;
import de.zettsystems.starfare.game.application.GameSessionStore;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.testsupport.EconomyUiStates;
import io.cucumber.java.de.Wenn;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Browser-only phone reachability and native input; desktop workflows live in Browserless. */
public class ResearchSteps {
    private final Browser browser;
    private final GameRegistry registry;
    private final GameSessionStore store;
    private final UserAccountService accounts;

    public ResearchSteps(Browser browser, GameRegistry registry, GameSessionStore store, UserAccountService accounts) {
        this.browser = browser;
        this.registry = registry;
        this.store = store;
        this.accounts = accounts;
    }

    @Wenn("ich die Schiffsforschung auf dem Handy prüfe")
    public void researchPhone() throws IOException {
        String email = "phone-research-" + UUID.randomUUID() + "@example.test";
        String host = String.valueOf(accounts.createAccount(email, Browser.PASSWORD,
                AccountName.display("Phone Researcher"), true).id());
        var state = EconomyUiStates.research();
        state.seatByUser().put(host, 1);
        state.planShipResearch(1, 50);
        GameId id = GameId.newId();
        store.save(new GameSession(id, "Phone research", host, Instant.now(), state));
        Browser phone = browser.newPhoneSession();
        try {
            phone.logIn(email);
            phone.open("/map/" + id.value());
            phone.awaitCss(".mobile-map");
            openResearch(phone);
            assertThat(phone.awaitCss("#research-budget input").getDomProperty("value")).isEqualTo("50");
            assertReachable(phone, "#research-budget input");
            assertReachable(phone, "#research-save");
            phone.captureUiAcceptance("research-phone");
            phone.type("#research-budget input", Keys.chord(Keys.CONTROL, "a"), "25", Keys.TAB);
            phone.clickCss("#research-save");
            phone.awaitNoneDisplayed("#research-save");
            int budget = registry.readState(id, current -> current.metals().orElseThrow().research().empires().get(1).percent());
            assertThat(budget).isEqualTo(25);
            phone.driver().navigate().refresh();
            openResearch(phone);
            assertThat(phone.awaitCss("#research-budget input").getDomProperty("value")).isEqualTo("25");
            phone.assertNoSidewaysScrolling();
            phone.assertNoBrowserErrors();
        } finally {
            phone.closeSession();
            store.delete(id);
        }
    }

    private static void openResearch(Browser phone) {
        phone.awaitCss(".app-header-menu vaadin-menu-bar-button").click();
        phone.clickMatching("vaadin-menu-bar-item", item -> item.getText().strip().equals("Schiffsforschung"), "research");
        phone.awaitCss("#research-budget input");
    }

    private static void assertReachable(Browser phone, String selector) {
        if (!(phone.driver() instanceof JavascriptExecutor js)) {
            throw new IllegalStateException("Browser does not support JavaScript");
        }
        assertThat(js.executeScript("""
                const e = arguments[0], r = e.getBoundingClientRect();
                const hit = document.elementFromPoint(r.x + r.width/2, r.y + r.height/2);
                return r.left >= 0 && r.right <= innerWidth && r.top >= 0 && r.bottom <= innerHeight
                    && (hit === e || e.contains(hit) || hit?.contains(e));
                """, phone.awaitCss(selector))).isEqualTo(true);
    }
}
