package de.zettsystems.starfare.e2e;

import org.jspecify.annotations.Nullable;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

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
    private final @Nullable WebDriver ownedDriver;

    @Autowired
    Browser(Environment environment) {
        this(environment, null);
    }

    private Browser(Environment environment, @Nullable WebDriver ownedDriver) {
        this.environment = environment;
        this.ownedDriver = ownedDriver;
    }

    public WebDriver driver() {
        return ownedDriver != null ? ownedDriver : SharedBrowser.driver();
    }

    public Browser newSession() {
        return new Browser(environment, SharedBrowser.createDriver());
    }

    /** Eigene Sitzung in einem emulierten Handy. */
    public Browser newPhoneSession() {
        return new Browser(environment, SharedBrowser.createPhoneDriver());
    }

    public void closeSession() {
        if (ownedDriver != null) {
            ownedDriver.quit();
        }
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
        type("input[name='username']", email);
        type("input[name='password']", PASSWORD + Keys.ENTER);
        awaitUrl(url -> !url.contains("/login"), "Anmeldung als " + email + " blieb auf /login hängen.");
    }

    public WebElement awaitCss(String css) {
        return new WebDriverWait(driver(), LOAD)
                .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(css)));
    }

    public void clickCss(String css) {
        new WebDriverWait(driver(), LOAD).ignoring(StaleElementReferenceException.class).until(_ -> {
            WebElement element = awaitCss(css);
            ((JavascriptExecutor) driver()).executeScript("arguments[0].click();", element);
            return true;
        });
    }

    /**
     * Runs a directly saved input and waits until the server stored it and replaced the given
     * panel. Interactions with the old panel would otherwise be lost while it is rebuilt.
     */
    public void saveAndAwaitRebuild(String panelCss, Runnable input, java.util.function.BooleanSupplier stored) {
        WebElement panel = awaitCss(panelCss);
        input.run();
        new WebDriverWait(driver(), LOAD).until(_ -> stored.getAsBoolean());
        new WebDriverWait(driver(), LOAD).until(ExpectedConditions.stalenessOf(panel));
        awaitCss(panelCss);
    }

    public List<WebElement> all(String css) {
        return driver().findElements(By.cssSelector(css));
    }

    /**
     * Für Ansichten ohne Element-Kennungen: der Knopf mit genau diesem Text. Rendert Vaadin
     * zwischen Suchen und Klicken neu (alter Knopf weg, neuer noch nicht bedienbar), sucht die
     * Wait erneut statt abzubrechen.
     */
    public void clickButtonWithText(String label) {
        new WebDriverWait(driver(), LOAD)
                .withMessage("Kein bedienbarer Knopf „" + label + "“ auf der Seite.")
                .ignoring(StaleElementReferenceException.class)
                .ignoring(ElementNotInteractableException.class)
                .until(_ -> {
                    WebElement button = buttonWithText(label);
                    if (button == null) {
                        return false;
                    }
                    try {
                        button.click();
                    } catch (ElementClickInterceptedException _) {
                        ((JavascriptExecutor) driver()).executeScript("arguments[0].click();", button);
                    }
                    return true;
                });
    }

    /**
     * Klickt das erste sichtbare Element zum Selektor, das die Bedingung erfüllt. Liste und
     * Klick werden bei einem Neu-Rendern gemeinsam wiederholt.
     */
    public void clickMatching(String css, Predicate<WebElement> filter, String description) {
        new WebDriverWait(driver(), LOAD)
                .withMessage("Kein bedienbares Element: " + description)
                .ignoring(StaleElementReferenceException.class)
                .ignoring(ElementNotInteractableException.class)
                .until(_ -> {
                    WebElement element = all(css).stream().filter(WebElement::isDisplayed).filter(filter)
                            .findFirst().orElse(null);
                    if (element == null) {
                        return false;
                    }
                    try {
                        element.click();
                    } catch (ElementClickInterceptedException _) {
                        ((JavascriptExecutor) driver()).executeScript("arguments[0].click();", element);
                    }
                    return true;
                });
    }

    /**
     * Tippt in das sichtbare Element zum Selektor. Ein Feld eines sich öffnenden Dialogs steht
     * schon im DOM, bevor es Eingaben annimmt; die Wait wiederholt dann die Eingabe.
     */
    public WebElement type(String css, CharSequence... keys) {
        return new WebDriverWait(driver(), LOAD)
                .withMessage("Feld " + css + " nimmt keine Eingabe an.")
                .ignoring(StaleElementReferenceException.class)
                .ignoring(ElementNotInteractableException.class)
                .until(_ -> {
                    WebElement field = awaitCss(css);
                    field.sendKeys(keys);
                    return field;
                });
    }

    /** Wählt eine Ansicht über den sichtbaren Reiter oder die kompakte Navigation. */
    public void clickTabWithText(String label) {
        WebElement tab = new WebDriverWait(driver(), LOAD)
                .withMessage("Keine Navigation für „" + label + "“ auf der Seite.")
                .until(_ -> driver().findElements(By.cssSelector("vaadin-tab, .map-sidebar-select")).stream()
                        .filter(WebElement::isDisplayed)
                        .filter(candidate -> candidate.getTagName().equals("vaadin-combo-box")
                                || label.equals(candidate.getText().strip()))
                        .findFirst().orElse(null));
        if (tab.getTagName().equals("vaadin-tab")) {
            ((JavascriptExecutor) driver()).executeScript("arguments[0].click();", tab);
        } else {
            type(".map-sidebar-select input", Keys.chord(Keys.CONTROL, "a"), label);
            clickMatching("vaadin-combo-box-item", item -> item.getText().strip().equals(label), "Ansicht " + label);
        }
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
                        .anyMatch(tab -> label.equals(tab.getText().strip()) && tab.getAttribute("selected") != null)
                        || driver().findElements(By.cssSelector(".map-sidebar-select input")).stream()
                        .filter(WebElement::isDisplayed).anyMatch(input -> label.equals(input.getDomProperty("value"))));
    }

    /** {@code null}, wenn der Knopf (noch) fehlt oder die Seite gerade neu rendert — die Wait fragt erneut. */
    private @Nullable WebElement buttonWithText(String label) {
        try {
            return driver().findElements(By.tagName("vaadin-button")).stream()
                    .filter(WebElement::isDisplayed)
                    .filter(button -> label.equals(button.getText().strip()) || label.equals(button.getDomAttribute("aria-label")))
                    .findFirst()
                    .orElse(null);
        } catch (StaleElementReferenceException _) {
            return null;
        }
    }

    public void awaitText(String snippet) {
        new WebDriverWait(driver(), LOAD).ignoring(StaleElementReferenceException.class)
                .withMessage("Erwarteter Text fehlt: " + snippet)
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

    /** Momentaufnahme der Texte; ein Neu-Rendern während des Auslesens liest erneut. */
    public List<String> textsOf(String css) {
        return new WebDriverWait(driver(), LOAD).ignoring(StaleElementReferenceException.class)
                .until(_ -> all(css).stream()
                        .map(element -> String.valueOf(element.getDomProperty("textContent")).strip())
                        .toList());
    }

    /** Wartet, bis die Texte zum Selektor die Bedingung erfüllen, statt eine Momentaufnahme zu prüfen. */
    public List<String> awaitTexts(String css, Predicate<List<String>> condition, String message) {
        return new WebDriverWait(driver(), LOAD).withMessage(message)
                .ignoring(StaleElementReferenceException.class)
                .until(_ -> {
                    List<String> texts = all(css).stream()
                            .map(element -> String.valueOf(element.getDomProperty("textContent")).strip())
                            .toList();
                    return condition.test(texts) ? texts : null;
                });
    }

    /** Wartet, bis kein Element zum Selektor mehr sichtbar ist (entfernte Elemente zählen als unsichtbar). */
    public void awaitNoneDisplayed(String css) {
        new WebDriverWait(driver(), LOAD).withMessage("Noch sichtbar: " + css)
                .ignoring(StaleElementReferenceException.class)
                .until(_ -> all(css).stream().noneMatch(WebElement::isDisplayed));
    }

    /** Wartet, bis genau {@code count} Elemente zum Selektor im DOM stehen. */
    public void awaitCount(String css, int count) {
        new WebDriverWait(driver(), LOAD).withMessage("Erwartet " + count + " Elemente für " + css)
                .until(_ -> all(css).size() == count);
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

    /** Reading browser logs drains them; call after each visited view to retain attribution. */
    public void assertNoBrowserErrors() {
        var errors = driver().manage().logs().get(org.openqa.selenium.logging.LogType.BROWSER).getAll()
                .stream().filter(entry -> entry.getLevel().intValue() >= java.util.logging.Level.SEVERE.intValue())
                .toList();
        assertThat(errors).as("Browser console at %s", driver().getCurrentUrl()).isEmpty();
        assertThat(all(".v-system-error").stream().filter(WebElement::isDisplayed).toList())
                .as("Vaadin system errors at %s", driver().getCurrentUrl()).isEmpty();
    }

    /** Der gerade gerenderte Seitentext — für Fehler-Dumps. */
    public String pageText() {
        return driver().findElement(By.tagName("body")).getText();
    }

    /** Saves the current rendered view for the personal UI acceptance. */
    public void captureUiAcceptance(String name) throws java.io.IOException {
        java.nio.file.Path target = java.nio.file.Path.of("build", "ui-acceptance", name + ".png");
        java.nio.file.Files.createDirectories(target.getParent());
        java.nio.file.Files.write(target, ((org.openqa.selenium.TakesScreenshot) driver())
                .getScreenshotAs(org.openqa.selenium.OutputType.BYTES));
        java.nio.file.Files.writeString(target.resolveSibling(name + ".txt"), pageText());
    }

    private String fullPageText() {
        String text = driver().findElement(By.tagName("body")).getDomProperty("textContent");
        return text == null ? "" : text;
    }
}
