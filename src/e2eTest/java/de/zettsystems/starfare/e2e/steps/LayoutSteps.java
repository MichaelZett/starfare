package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.identity.values.AccountName;
import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.game.application.GameRegistry;
import de.zettsystems.starfare.testsupport.ReviewGameFixture;
import io.cucumber.java.de.Wenn;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Browser-only layout and production-bundle checks retained when functional journeys move. */
public class LayoutSteps {
    private final Browser browser;
    private final UserAccountService accounts;
    private final ReviewGameFixture fixtures;
    private final GameRegistry registry;

    public LayoutSteps(Browser browser, UserAccountService accounts, ReviewGameFixture fixtures, GameRegistry registry) {
        this.browser = browser;
        this.accounts = accounts;
        this.fixtures = fixtures;
        this.registry = registry;
    }

    @Wenn("ich den Layout-Rundgang bei 1366 und 1024 Pixeln durchlaufe")
    public void walkLayouts() throws IOException {
        String email = "layout-" + UUID.randomUUID() + "@example.test";
        accounts.createAccount(email, Browser.PASSWORD, AccountName.display("Layout Tester"), true);
        browser.logIn(email);
        var game = fixtures.create(email, null);
        Dimension original = browser.driver().manage().window().getSize();
        try {
            for (int width : new int[]{1366, 1024}) {
                browser.driver().manage().window().setSize(new Dimension(width, 768));
                visitLobbyAndWizard(width);
                browser.open("/map/" + game.id().value());
                browser.awaitCss("#map .sys-own");
                browser.clickMatching("#map .sys-own", element -> element.getText().startsWith("Alpha"), "Alpha");
                browser.awaitCss(".system-metric");
                capture("system-details", width);
            }
            registry.writeState(game.id(), state -> { state.endGame(game.hostSeat()); return null; });
            for (int width : new int[]{1366, 1024}) {
                browser.driver().manage().window().setSize(new Dimension(width, 768));
                browser.open("/archive");
                browser.awaitCss(".archive-game-card");
                capture("archive", width);
                browser.clickMatching(".archive-game-card vaadin-button", button ->
                        button.getText().strip().equals("Ansehen") && button.findElement(By.xpath(
                                "ancestor::*[contains(@class, 'archive-game-card')]"))
                                .getText().contains("Review regression"), "Review regression archive action");
                browser.awaitUrl(url -> url.contains(game.id().value()), "Archive opened the wrong game");
                browser.awaitCss("#map .sys-own");
                capture("review", width);
            }
        } finally {
            browser.driver().manage().window().setSize(original);
            registry.abortGame(game.id());
        }
    }

    private void visitLobbyAndWizard(int width) throws IOException {
        browser.open("/");
        browser.awaitCss(".lobby-root");
        capture("lobby", width);
        browser.clickButtonWithText("Neues Spiel");
        browser.awaitCss("vaadin-dialog[opened] .wizard-section");
        assertWizardFits();
        capture("wizard", width);
        browser.clickButtonWithText("Zusammenfassung prüfen");
        browser.awaitCss(".wizard-confirmation-content");
        assertWizardFits();
        capture("wizard-summary", width);
        browser.clickButtonWithText("Abbrechen");
        browser.awaitNoneDisplayed("vaadin-dialog[opened]");
    }

    private void assertWizardFits() {
        if (!(browser.driver() instanceof JavascriptExecutor js)) {
            throw new IllegalStateException("Browser does not support JavaScript");
        }
        Object result = new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                js.executeScript("""
                        const dialog = document.querySelector('vaadin-dialog[opened]');
                        const contents = [];
                        function inspect(root) {
                            for (const element of root.querySelectorAll('*')) {
                                if (element.part.contains('content') && element.clientWidth > 0) contents.push(element);
                                if (element.shadowRoot) inspect(element.shadowRoot);
                            }
                        }
                        inspect(dialog);
                        if (dialog.shadowRoot) inspect(dialog.shadowRoot);
                        return contents.length ? Math.max(...contents.map(e => e.scrollWidth - e.clientWidth)) : null;
                        """));
        if (!(result instanceof Number overflow)) {
            throw new IllegalStateException("Wizard overflow measurement is not numeric");
        }
        assertThat(overflow.intValue()).as("Wizard horizontal overflow").isBetween(0, 1);
    }

    private void capture(String name, int width) throws IOException {
        browser.assertNoSidewaysScrolling();
        browser.assertNoBrowserErrors();
        browser.captureUiAcceptance("layout-" + name + "-" + width);
    }
}
