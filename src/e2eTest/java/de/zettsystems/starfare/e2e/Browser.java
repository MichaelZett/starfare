package de.zettsystems.starfare.e2e;

import org.jspecify.annotations.Nullable;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Die Browser-Handgriffe der Steps an einer Stelle, damit die
 * Vaadin-Eigenheiten (Wert steht im inneren {@code input}, Knöpfe ohne
 * Kennungen im Identity-Baustein, Dialoge im Overlay) nicht in jede
 * Step-Klasse wandern.
 */
@Component
public class Browser {

    private static final Duration LOAD = Duration.ofSeconds(30);

    public static final String PASSWORD = "ein-langes-testpasswort";

    private final Environment environment;

    Browser(Environment environment) {
        this.environment = environment;
    }

    public WebDriver driver() {
        return SharedBrowser.driver();
    }

    public String baseUrl() {
        // Lazy statt @Value: Der zufällige Port steht erst nach dem Serverstart fest.
        return "http://localhost:" + environment.getProperty("local.server.port");
    }

    public void open(String path) {
        driver().get(baseUrl() + path);
    }

    /** Neues Szenario, niemand angemeldet: Session-Cookie weg. */
    public void resetSession() {
        driver().get(baseUrl() + "/login");
        driver().manage().deleteAllCookies();
    }

    public void logIn(String email) {
        open("/login");
        awaitCss("input[name='username']").sendKeys(email);
        driver().findElement(By.cssSelector("input[name='password']")).sendKeys(PASSWORD + Keys.ENTER);
        awaitUrl(url -> !url.contains("/login"), "Anmeldung als " + email + " blieb auf /login hängen.");
    }

    public WebElement awaitCss(String css) {
        return new WebDriverWait(driver(), LOAD)
                .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(css)));
    }

    public List<WebElement> all(String css) {
        return driver().findElements(By.cssSelector(css));
    }

    /** Für Ansichten ohne Element-Kennungen: der Knopf mit genau diesem Text. */
    public void clickButtonWithText(String label) {
        WebElement button = new WebDriverWait(driver(), LOAD)
                .withMessage("Kein Knopf „" + label + "“ auf der Seite.")
                .until(_ -> buttonWithText(label));
        try {
            button.click();
        } catch (ElementClickInterceptedException _) {
            ((JavascriptExecutor) driver()).executeScript("arguments[0].click();", button);
        }
    }

    /** Klickt einen sichtbaren Reiter mit exakt diesem Text. */
    public void clickTabWithText(String label) {
        WebElement tab = new WebDriverWait(driver(), LOAD)
                .withMessage("Kein Reiter „" + label + "“ auf der Seite.")
                .until(_ -> driver().findElements(By.tagName("vaadin-tab")).stream()
                        .filter(WebElement::isDisplayed)
                        .filter(candidate -> label.equals(candidate.getText().strip()))
                        .findFirst().orElse(null));
        ((JavascriptExecutor) driver()).executeScript("arguments[0].click();", tab);
    }

    /** Öffnet einen sichtbaren aufklappbaren Bereich anhand seiner Zusammenfassung. */
    public void openDetailsWithText(String label) {
        WebElement details = new WebDriverWait(driver(), LOAD)
                .withMessage("Kein Bereich „" + label + "“ auf der Seite.")
                .until(_ -> driver().findElements(By.tagName("vaadin-details")).stream()
                        .filter(WebElement::isDisplayed)
                        .filter(candidate -> label.equals(candidate.getText().strip()))
                        .findFirst().orElse(null));
        ((JavascriptExecutor) driver()).executeScript("arguments[0].click();", details);
    }

    public void awaitSelectedTabWithText(String label) {
        new WebDriverWait(driver(), LOAD).withMessage("Reiter „" + label + "“ wurde nicht ausgewählt.")
                .until(_ -> driver().findElements(By.tagName("vaadin-tab")).stream()
                        .filter(WebElement::isDisplayed)
                        .anyMatch(tab -> label.equals(tab.getText().strip()) && tab.getAttribute("selected") != null));
    }

    /** {@code null}, wenn der Knopf (noch) fehlt oder die Seite gerade neu rendert — die Wait fragt erneut. */
    private @Nullable WebElement buttonWithText(String label) {
        try {
            return driver().findElements(By.tagName("vaadin-button")).stream()
                    .filter(WebElement::isDisplayed)
                    .filter(button -> label.equals(button.getText().strip()))
                    .findFirst()
                    .orElse(null);
        } catch (StaleElementReferenceException _) {
            return null;
        }
    }

    public void awaitText(String snippet) {
        new WebDriverWait(driver(), LOAD).withMessage("Erwarteter Text fehlt: " + snippet)
                .until(_ -> fullPageText().contains(snippet));
    }

    public List<WebElement> awaitAtLeast(String css, int count) {
        return new WebDriverWait(driver(), LOAD).withMessage("Zu wenige Elemente für " + css)
                .until(_ -> {
                    List<WebElement> elements = all(css);
                    return elements.size() >= count ? elements : null;
                });
    }

    public void awaitUrl(Predicate<String> condition, String message) {
        new WebDriverWait(driver(), LOAD).withMessage(message)
                .until(webDriver -> condition.test(String.valueOf(webDriver.getCurrentUrl())));
    }

    /** Wartet, bis ein Element mit dem Selektor den Text enthält. */
    public void awaitTextIn(String css, String snippet) {
        new WebDriverWait(driver(), LOAD)
                .withMessage("In " + css + " fehlt der Text: " + snippet)
                .ignoring(StaleElementReferenceException.class)
                .until(_ -> all(css).stream()
                        .map(element -> String.valueOf(element.getDomProperty("textContent")))
                        .anyMatch(text -> text.contains(snippet)));
    }

    public List<String> textsOf(String css) {
        return all(css).stream()
                .map(element -> String.valueOf(element.getDomProperty("textContent")).strip())
                .toList();
    }

    /**
     * Die Seite selbst scrollt nie seitwärts — läuft als Hook nach jedem
     * Schritt und prüft so jede besuchte Ansicht im HD-Viewport.
     */
    public void assertNoSidewaysScrolling() {
        JavascriptExecutor js = (JavascriptExecutor) driver();
        long scrollWidth = ((Number) js.executeScript("return document.documentElement.scrollWidth")).longValue();
        long clientWidth = ((Number) js.executeScript("return document.documentElement.clientWidth")).longValue();
        Object overflowing = scrollWidth > clientWidth ? js.executeScript("""
                return [...document.querySelectorAll('body *')]
                    .filter(element => element.getBoundingClientRect().right > document.documentElement.clientWidth)
                    .slice(0, 12).map(element => element.tagName + '.' + element.className
                        + ': ' + element.getBoundingClientRect().right).join('; ');
                """) : "";
        assertThat(scrollWidth)
                .withFailMessage("Seite %s scrollt seitwärts (%d > %d px): %s", driver().getCurrentUrl(), scrollWidth, clientWidth, overflowing)
                .isLessThanOrEqualTo(clientWidth + 1);
    }

    /** Der gerade gerenderte Seitentext — für Fehler-Dumps. */
    public String pageText() {
        return driver().findElement(By.tagName("body")).getText();
    }

    private String fullPageText() {
        String text = driver().findElement(By.tagName("body")).getDomProperty("textContent");
        return text == null ? "" : text;
    }
}
