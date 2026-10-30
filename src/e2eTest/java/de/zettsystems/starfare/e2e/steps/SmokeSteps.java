package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.e2e.E2eWorld;
import de.zettsystems.starfare.e2e.RecordingMailConfiguration.RecordingMailSender;
import io.cucumber.java.After;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Die Schritte des Smoke-Szenarios: vom Konto bis zur ersten Runde.
 *
 * <p>Die Identity-Ansichten tragen keine Element-Kennungen; ihre Felder werden
 * in DOM-Reihenfolge befüllt, Knöpfe über ihren Text gefunden. Vom
 * Bestätigungslink zählt nur Pfad und Query — der konfigurierte Host passt
 * nicht zum zufälligen Testport.
 */
public class SmokeSteps {

    private final Browser browser;
    private final E2eWorld world;
    private final RecordingMailSender mails;

    public SmokeSteps(Browser browser, E2eWorld world, RecordingMailSender mails) {
        this.browser = browser;
        this.world = world;
        this.mails = mails;
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

    @Wenn("ich mich als {string} mit der E-Mail-Adresse {string} registriere")
    public void register(String displayName, String email) {
        world.rememberAccount(displayName, email);
        browser.open("/register");
        browser.type("vaadin-text-field input", displayName);
        browser.type("vaadin-email-field input", email);
        List<WebElement> passwordFields = browser.awaitAtLeast("vaadin-password-field input", 2);
        passwordFields.get(0).sendKeys(Browser.PASSWORD);
        passwordFields.get(1).sendKeys(Browser.PASSWORD);
        browser.clickButtonWithText("Registrieren");
        browser.awaitText("Fast geschafft");
    }

    @Dann("liegt eine Bestätigungsmail für {string} vor")
    public void confirmationMailExists(String email) {
        assertThat(confirmationUrlFor(email)).isNotBlank();
    }

    @Wenn("ich den Bestätigungslink für {string} öffne")
    public void openConfirmationLink(String email) {
        URI url = URI.create(confirmationUrlFor(email));
        String pathAndQuery = url.getRawPath() + (url.getRawQuery() == null ? "" : "?" + url.getRawQuery());
        browser.open(pathAndQuery);
        // Eingelöst wird erst auf Knopfdruck — schützt das Einmal-Token vor Mail-Scannern.
        browser.awaitText("E-Mail bestätigen");
        browser.clickButtonWithText("E-Mail-Adresse bestätigen");
        browser.awaitText("Konto freigeschaltet");
    }

    @Wenn("ich mich mit {string} anmelde")
    public void logInWithEmail(String email) {
        browser.logIn(email);
    }

    @Dann("sehe ich in der Lobby den Online-Spieler {string}")
    public void lobbyShowsOnlinePlayer(String displayName) {
        browser.awaitTextIn(".presence-name", displayName);
    }

    @Wenn("ich ein Standardspiel anlege und beitrete")
    public void createDefaultGameAndJoin() throws IOException {
        browser.captureUiAcceptance("lobby");
        browser.clickButtonWithText("Neues Spiel");
        browser.clickButtonWithText("Zusammenfassung prüfen");
        browser.awaitCss(".wizard-summary-group");
        browser.captureUiAcceptance("wizard-summary");
        browser.clickCss(".wizard-summary-group vaadin-button");
        String original = wizardSystems().getDomProperty("value");
        int corrected = Integer.parseInt(original) + 1;
        typeWizardSystems(String.valueOf(corrected));
        browser.clickButtonWithText("Zusammenfassung prüfen");
        browser.awaitTextIn(".wizard-summary-line", "Systeme: " + corrected);
        assertThat(browser.textsOf(".wizard-summary-line")).contains("Systeme: " + corrected);
        browser.captureUiAcceptance("wizard-corrected");
        // Restore the standard fixture after verifying the correction round trip.
        browser.clickCss(".wizard-summary-group vaadin-button");
        typeWizardSystems(original);
        browser.clickButtonWithText("Zusammenfassung prüfen");
        browser.awaitTextIn(".wizard-summary-line", "Systeme: " + original);
        browser.clickButtonWithText("Spiel anlegen");
        browser.awaitUrl(url -> url.contains("/map/"), "Die KI-Partie wurde nicht automatisch gestartet.");
    }

    /** The wizard page is rebuilt when it opens; retry the input until the visible field accepts it. */
    private void typeWizardSystems(String value) {
        new org.openqa.selenium.support.ui.WebDriverWait(browser.driver(), java.time.Duration.ofSeconds(10))
                .ignoring(org.openqa.selenium.StaleElementReferenceException.class)
                .ignoring(org.openqa.selenium.ElementNotInteractableException.class)
                .until(_ -> {
                    WebElement systems = wizardSystems();
                    systems.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a"));
                    systems.sendKeys(value, org.openqa.selenium.Keys.TAB);
                    return true;
                });
    }

    private WebElement wizardSystems() {
        return new org.openqa.selenium.support.ui.WebDriverWait(browser.driver(), java.time.Duration.ofSeconds(10))
                .ignoring(org.openqa.selenium.StaleElementReferenceException.class)
                .until(_ -> browser.all(".create-game-dialog vaadin-integer-field").stream()
                        .filter(WebElement::isDisplayed)
                        .filter(field -> "Galaxiegröße (Systeme)".equals(field.getDomProperty("label")))
                        .findFirst().map(field -> field.findElement(org.openqa.selenium.By.cssSelector("input")))
                        .orElse(null));
    }

    @Dann("zeigt der Verwalten-Dialog den Spieler {string}")
    public void manageDialogShowsPlayer(String displayName) {
        browser.openDetailsWithText("Weitere Aktionen");
        browser.clickButtonWithText("Verwalten");
        browser.awaitTextIn(".manage-row-label", displayName);
        browser.clickButtonWithText("Schließen");
    }

    @Wenn("ich das Spiel starte und zur Karte wechsle")
    public void startGameAndOpenMap() {
        browser.clickButtonWithText("Starten");
        browser.awaitUrl(url -> url.contains("/map/"), "Die Karte wurde nicht geöffnet.");
    }

    @Dann("zeigen die Empire-Stats {int} System, Produktion {int} und {int} Schiffe")
    public void empireStatsShow(int systems, int production, int ships) {
        List<String> expected = List.of(String.valueOf(systems), String.valueOf(production), String.valueOf(ships));
        browser.awaitTexts(".app-header-stat-value", expected::equals, "Empire-Stats zeigen nicht " + expected);
    }

    @Dann("kann ich im Systeminspektor eine Flotte oder Verlegung vorbereiten und abbrechen")
    public void systemInspectorStartsAndCancelsBothTargetSelections() {
        browser.clickCss(".sys-action-source");
        browser.awaitCss(".system-metric[aria-label^='Garnison']");
        browser.awaitCss(".system-metric[aria-label^='Produktion']");
        browser.awaitCss(".system-metric[aria-label*='reserve']");
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

    @Dann("zeigt die Systemsicht stabile Himmelskörper und bleibt auf Desktopbreiten bedienbar")
    public void detailedSystemIsStableAndAccessible() throws IOException {
        browser.clickCss(".sys-action-source");
        browser.awaitCss(".system-orrery");
        Files.createDirectories(Path.of("build", "system-view"));
        Files.write(Path.of("build", "system-view", "classic-overview.png"),
                ((TakesScreenshot) browser.driver()).getScreenshotAs(OutputType.BYTES));
        assertThat(browser.all(".system-star")).hasSizeBetween(1, 2);
        assertThat(browser.all(".system-world")).hasSizeBetween(2, 7);
        List<String> original = browser.all(".system-body-target").stream()
                .map(element -> element.getAttribute("aria-label")).toList();
        browser.type(".system-world", org.openqa.selenium.Keys.ENTER);
        browser.awaitTextIn(".system-body-description", "Umlaufbahn");
        assertThat(browser.all("vaadin-dialog[opened]")).isEmpty();
        assertThat(browser.all(".system-shipyard")).hasSize(1);
        var originalSize = browser.driver().manage().window().getSize();
        try {
            for (int width : List.of(1366, 1024)) {
                browser.driver().manage().window().setSize(new org.openqa.selenium.Dimension(width, 768));
                browser.assertNoSidewaysScrolling();
                WebElement diagram = browser.awaitCss(".system-orrery");
                assertThat(diagram.getSize().getWidth()).isGreaterThan(180);
                Files.createDirectories(Path.of("build", "system-view"));
                Files.write(Path.of("build", "system-view", "classic-" + width + ".png"),
                        ((TakesScreenshot) browser.driver()).getScreenshotAs(OutputType.BYTES));
            }
            browser.driver().navigate().refresh();
            browser.clickCss(".sys-action-source");
            browser.awaitCss(".system-orrery");
            assertThat(browser.all(".system-body-target").stream()
                    .map(element -> element.getAttribute("aria-label")).toList()).isEqualTo(original);
            browser.awaitTexts(".app-header-stat-value", List.of("1", "4", "8")::equals,
                    "Empire-Stats nach dem Neuladen zeigen nicht 1, 4, 8");
            verifyEnglishSystemView();
        } finally {
            browser.driver().manage().window().setSize(originalSize);
        }
    }

    private void verifyEnglishSystemView() throws IOException {
        changeLanguage("Englisch", "Shipyard");
        browser.awaitTextIn(".system-schematic-caption", "Star reduced");
        Files.write(Path.of("build", "system-view", "classic-english.png"),
                ((TakesScreenshot) browser.driver()).getScreenshotAs(OutputType.BYTES));
        changeLanguage("German", "Werft");
        browser.awaitTextIn(".system-schematic-caption", "Stern verkleinert");
    }

    private void changeLanguage(String label, String shipyard) {
        browser.type("vaadin-combo-box input", org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a"), label);
        browser.awaitTextIn("vaadin-combo-box-item", label);
        browser.clickCss("vaadin-combo-box-item");
        browser.awaitText(shipyard.equals("Werft") ? "Runde abgeben" : "Submit turn");
        browser.clickCss(".sys-action-source");
        browser.awaitTextIn(".system-shipyard", shipyard);
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

    /** Mail delivery may run after the confirmation page is shown; poll instead of reading once. */
    private String confirmationUrlFor(String email) {
        return new org.openqa.selenium.support.ui.WebDriverWait(browser.driver(), java.time.Duration.ofSeconds(10))
                .withMessage("Keine aufgezeichnete Mail an " + email + ".")
                .until(_ -> mails.sent().stream()
                        .filter(mail -> mail.recipient().equals(email))
                        .reduce((_, second) -> second)
                        .map(RecordingMailSender.Sent::url)
                        .orElse(null));
    }
}
