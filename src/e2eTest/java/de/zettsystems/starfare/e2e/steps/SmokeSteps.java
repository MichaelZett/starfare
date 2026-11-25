package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.e2e.E2eWorld;
import de.zettsystems.starfare.testsupport.TestPlatformAccounts;
import io.cucumber.java.After;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Die Schritte des Smoke-Szenarios: vom Konto bis zur ersten Runde. */
public class SmokeSteps {

    private final Browser browser;
    private final E2eWorld world;
    private final TestPlatformAccounts accounts;

    public SmokeSteps(Browser browser, E2eWorld world, TestPlatformAccounts accounts) {
        this.browser = browser;
        this.world = world;
        this.accounts = accounts;
    }

    @Before
    public void startLoggedOut() {
        browser.resetSession();
        browser.driver().manage().logs().get(org.openqa.selenium.logging.LogType.BROWSER);
    }

    @Dann("meldet die Browser-Konsole keine Fehler")
    public void browserHasNoErrors() {
        browser.assertNoBrowserErrors();
    }

    /** HD-Zusicherung: Keine besuchte Ansicht scrollt seitwärts (CI-Lauf vom 2026-08-28: Lobby tat es). */
    @AfterStep
    public void pageStaysWithinViewport() {
        if (String.valueOf(browser.driver().getCurrentUrl()).startsWith(browser.baseUrl())) {
            browser.assertNoSidewaysScrolling();
        }
    }

    /** Seitentext und Screenshot zum Fehlschlag — erspart das Nachstellen. */
    @After
    public void dumpPageOnFailure(Scenario scenario) {
        if (!scenario.isFailed()) {
            return;
        }
        System.err.println("Seite beim Fehlschlag von „" + scenario.getName() + "“:\n" + browser.pageText());
        byte[] screenshot = ((TakesScreenshot) browser.driver()).getScreenshotAs(OutputType.BYTES);
        if (screenshot == null) {
            return;
        }
        try {
            Path target = Path.of("build", "e2e-failures", scenario.getName().replaceAll("\\W+", "-") + ".png");
            Files.createDirectories(target.getParent());
            Files.write(target, screenshot);
            System.err.println("Screenshot: " + target.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Screenshot nicht speicherbar: " + e.getMessage());
        }
    }

    /** Ein Konto, wie es ein Plattform-Host kennt; Starfares eigener Start hat keine Registrierung. */
    @Wenn("das Konto {string} mit der E-Mail-Adresse {string} besteht")
    public void accountExists(String displayName, String email) {
        world.rememberAccount(displayName, email);
        accounts.createAccount(email, displayName);
    }

    @Wenn("ich mich mit {string} anmelde")
    public void logInWithEmail(String email) {
        browser.logIn(email);
    }

    @Wenn("ich die Browsersitzung wechsle")
    public void switchSession() {
        browser.resetSession();
    }

    @Wenn("ich ein Standardspiel anlege und beitrete")
    public void createDefaultGameAndJoin() throws IOException {
        browser.captureUiAcceptance("lobby");
        browser.openNewGameWizard();
        // Picture cards are radio buttons: a click on a card chooses it, the check mark follows.
        chooseCard("#galaxy-size", "Klein");
        chooseCard("#galaxy-size", "Mittel");
        browser.captureUiAcceptance("wizard");
        browser.clickButtonWithText("Spiel anlegen");
        browser.awaitUrl(url -> url.contains("/map/"), "Die KI-Partie wurde nicht automatisch gestartet.");
    }

    private void chooseCard(String group, String title) {
        browser.clickMatching(group + " vaadin-radio-button", card -> title.equals(
                card.findElement(org.openqa.selenium.By.cssSelector(".setup-card-title")).getText()),
                "Karte „" + title + "“ in " + group);
        browser.awaitTexts(group + " vaadin-radio-button[checked] .setup-card-title", List.of(title)::equals,
                "Karte „" + title + "“ in " + group + " ist nicht gewählt");
    }

    @Dann("zeigen die Empire-Stats {int} System und Produktion {int}")
    public void empireStatsShow(int systems, int production) {
        // The start balance sets the home garrison by position, so the ship count varies from game to game.
        browser.awaitTexts(".app-header-stat-value", texts -> texts.size() == 3
                        && texts.get(0).equals(String.valueOf(systems)) && texts.get(1).equals(String.valueOf(production))
                        && Integer.parseInt(texts.get(2)) > 0,
                "Empire-Stats zeigen nicht " + systems + " System und Produktion " + production);
    }

    @Dann("kann ich im Systeminspektor eine Flotte oder Verlegung vorbereiten und abbrechen")
    public void systemInspectorStartsAndCancelsBothTargetSelections() {
        browser.clickCss(".sys-action-source");
        browser.awaitCss(".system-metric[aria-label^='Garnison']");
        browser.awaitCss(".system-metric[aria-label^='Produktion']");
        browser.clickButtonWithText("Verlegung anlegen");
        browser.awaitText("Ziel auf der Karte auswählen");
        browser.clickButtonWithText("Zielwahl abbrechen");
        browser.clickButtonWithText("Flotte senden");
        browser.awaitText("Ziel auf der Karte auswählen");
        browser.clickCss(".sys-target:not(.sys-selected)");
        browser.awaitTextIn(".send-fleet-duration", "Ankunft Runde");
        browser.clickButtonWithText("Abbrechen");
        browser.awaitNoneDisplayed("vaadin-dialog[opened]");
        // Cancelling the dialog deliberately preserves target selection. Reset it before testing
        // the right-click shortcut, which toggles the selected source.
        browser.clickButtonWithText("Zielwahl abbrechen");
        browser.awaitNoneDisplayed(".sys-selected");
        new org.openqa.selenium.support.ui.WebDriverWait(browser.driver(), java.time.Duration.ofSeconds(10))
                .ignoring(org.openqa.selenium.StaleElementReferenceException.class)
                .until(_ -> {
                    new org.openqa.selenium.interactions.Actions(browser.driver())
                            .contextClick(browser.awaitCss(".sys-action-source")).perform();
                    return true;
                });
        browser.awaitText("Ziel auf der Karte auswählen");
        // Wait for the server-side selection before clicking the newly rendered target.
        new org.openqa.selenium.support.ui.WebDriverWait(browser.driver(), java.time.Duration.ofSeconds(10))
                .until(_ -> browser.all(".sys-selected").size() == 1);
        assertThat(browser.all("vaadin-context-menu-overlay[opened]")).isEmpty();
        browser.clickCss(".sys-target:not(.sys-selected)");
        browser.awaitTextIn(".send-fleet-duration", "Ankunft Runde");
        browser.clickButtonWithText("Abbrechen");
    }

    @Wenn("ich die nächste Runde auslöse")
    public void advanceRound() {
        browser.clickButtonWithText("Runde abgeben");
    }

    @Dann("sehe ich den Rundenbericht für Runde {int}")
    public void roundReportIsShown(int round) {
        browser.awaitUrl(url -> url.contains("/map/"), "Die Karte wurde nach dem Zug verlassen.");
        browser.awaitSelectedTabWithText("Bericht");
        browser.awaitText("Runde " + (round + 1));
    }
}
