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
        browser.awaitCss("vaadin-text-field input").sendKeys(displayName);
        browser.all("vaadin-email-field input").getFirst().sendKeys(email);
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
    public void createDefaultGameAndJoin() {
        browser.clickButtonWithText("Neues Spiel");
        browser.clickButtonWithText("Spiel anlegen");
        browser.awaitUrl(url -> url.contains("/map/"), "Die KI-Partie wurde nicht automatisch gestartet.");
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
        browser.awaitTextIn(".app-header-stat-value", String.valueOf(ships));
        assertThat(browser.textsOf(".app-header-stat-value"))
                .containsExactly(String.valueOf(systems), String.valueOf(production), String.valueOf(ships));
    }

    @Wenn("ich die nächste Runde auslöse")
    public void advanceRound() {
        browser.clickButtonWithText("Nächste Runde");
    }

    @Dann("sehe ich den Rundenbericht für Runde {int}")
    public void roundReportIsShown(int round) {
        browser.awaitUrl(url -> url.contains("/map/"), "Die Karte wurde nach dem Zug verlassen.");
        browser.awaitSelectedTabWithText("Bericht");
        browser.awaitText("Runde " + (round + 1));
    }

    private String confirmationUrlFor(String email) {
        return mails.sent().stream()
                .filter(mail -> mail.recipient().equals(email))
                .reduce((_, second) -> second)
                .map(RecordingMailSender.Sent::url)
                .orElseThrow(() -> new AssertionError("Keine aufgezeichnete Mail an " + email + "."));
    }
}
