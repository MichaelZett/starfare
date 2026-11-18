package de.zettsystems.starfare.e2e;

import org.jspecify.annotations.Nullable;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Die Browser-Handgriffe der Steps an einer Stelle, damit die
 * Vaadin-Eigenheiten (Wert steht im inneren {@code input}, Knöpfe über
 * ihren Text, Dialoge im Overlay) nicht in jede
 * Step-Klasse wandern.
 */
@Component
public class Browser {

    private static final Duration LOAD = Duration.ofSeconds(30);

    public static final String LOBBY = "/starfare";

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
        driver().get(baseUrl() + LOBBY);
        driver().manage().deleteAllCookies();
    }

    /**
     * Meldet das Testkonto in dieser Browsersitzung an (E2eSignInView). Starfares eigener Start hat
     * keine Anmeldung; Konten mehrerer Spieler kennt nur die Test-Umsetzung der Plattform-Konten.
     */
    public void logIn(String email) {
        open("/" + E2eSignInView.PATH + "?email=" + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8));
        awaitUrl(url -> !url.contains(E2eSignInView.PATH), "Anmeldung als " + email + " blieb hängen.");
        awaitLobby();
    }

    /**
     * Waits for the lobby after sign-in or after opening {@link #LOBBY}: Starfare's own lobby in its
     * own start, the platform lobby on /games in a host that lists Starfare's matches (M12.1).
     *
     * @return whether it is Starfare's own lobby
     */
    public boolean awaitLobby() {
        new WebDriverWait(driver(), LOAD).withMessage("Weder Starfares Lobby noch die Plattform-Lobby erschien.")
                .until(driver -> !driver.findElements(By.cssSelector(".lobby-root, #gp-lobby-mine")).isEmpty());
        return !driver().findElements(By.cssSelector(".lobby-root")).isEmpty();
    }

    /**
     * Opens the wizard from the lobby the page shows: the button of Starfare's own lobby, in a host
     * Starfare's page for a new match (the platform lobby links there).
     */
    public void openNewGameWizard() {
        if (awaitLobby()) {
            clickButtonWithText("Neues Spiel");
        } else {
            open(LOBBY + "/new");
        }
        awaitCss("vaadin-dialog[opened] .wizard-section");
    }

    /** Whether a platform host frames the page: its game bar carries Starfare's navigation (M9). */
    public boolean hosted() {
        return !driver().findElements(By.cssSelector(".gp-game-nav")).isEmpty();
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

    public List<WebElement> all(String css) {
        return driver().findElements(By.cssSelector(css));
    }

    /** Whether the page shows Starfare's own language box (own start) or a platform frame's menu (host). */
    public boolean hasOwnLanguageSwitch() {
        return !all(".lang-switcher input").isEmpty();
    }

    /**
     * Switches the language by its label in Starfare's language box ("Englisch", "German", ...).
     * Inside a platform host the account menu of the header carries the languages instead (M9).
     */
    public void switchLanguage(String label) {
        if (hasOwnLanguageSwitch()) {
            type(".lang-switcher input", Keys.chord(Keys.CONTROL, "a"), label);
            clickMatching("vaadin-combo-box-item", item -> item.getText().equals(label), "Sprache " + label);
            return;
        }
        String target = label.startsWith("Engl") ? "English" : "Deutsch";
        var js = (JavascriptExecutor) driver();
        js.executeScript("window.e2eBeforeLanguageSwitch = true;");
        clickCss(".gp-account vaadin-menu-bar-button");
        clickMatching("vaadin-menu-bar-item", item -> List.of("Sprache", "Language").contains(item.getText().trim()),
                "Language menu");
        clickMatching("vaadin-menu-bar-item", item -> item.getText().trim().equals(target), "Language " + target);
        // The platform reloads the page after a language change; the reload drops the marker. Wait for the new page
        // before the next step looks up elements. During the navigation Chrome may reject the script.
        new WebDriverWait(driver(), LOAD).ignoring(WebDriverException.class).until(_ -> Boolean.TRUE.equals(
                js.executeScript("return window.e2eBeforeLanguageSwitch !== true && document.readyState === 'complete';")));
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
        } catch (WebDriverException e) {
            if (detached(e)) { return null; }
            throw e;
        }
    }

    /**
     * Chrome reports an element removed by a re-render as "Node with given id does not belong to
     * the document" instead of as stale. Such a condition counts as not yet met, so the wait asks
     * again; StaleElementReferenceException is a subclass and is covered as well.
     */
    static <T> Function<WebDriver, @Nullable T> retryingDetached(Function<WebDriver, @Nullable T> condition) {
        return webDriver -> {
            try {
                return condition.apply(webDriver);
            } catch (WebDriverException e) {
                if (e instanceof StaleElementReferenceException || detached(e)) { return null; }
                throw e;
            }
        };
    }

    static boolean detached(WebDriverException e) {
        return String.valueOf(e.getMessage()).contains("does not belong to the document");
    }

    public void awaitText(String snippet) {
        new WebDriverWait(driver(), LOAD).ignoring(StaleElementReferenceException.class)
                .withMessage("Erwarteter Text fehlt: " + snippet)
                .until(retryingDetached(_ -> fullPageText().contains(snippet) ? Boolean.TRUE : null));
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
                .until(retryingDetached(_ -> all(css).stream()
                        .map(element -> String.valueOf(element.getDomProperty("textContent")))
                        .anyMatch(text -> text.contains(snippet)) ? Boolean.TRUE : null));
    }

    /** Momentaufnahme der Texte; ein Neu-Rendern während des Auslesens liest erneut. */
    public List<String> textsOf(String css) {
        return new WebDriverWait(driver(), LOAD).ignoring(StaleElementReferenceException.class)
                .until(retryingDetached(_ -> all(css).stream()
                        .map(element -> String.valueOf(element.getDomProperty("textContent")).strip())
                        .toList()));
    }

    /** Wartet, bis die Texte zum Selektor die Bedingung erfüllen, statt eine Momentaufnahme zu prüfen. */
    public List<String> awaitTexts(String css, Predicate<List<String>> condition, String message) {
        return new WebDriverWait(driver(), LOAD).withMessage(message)
                .ignoring(StaleElementReferenceException.class)
                .until(retryingDetached(_ -> {
                    List<String> texts = all(css).stream()
                            .map(element -> String.valueOf(element.getDomProperty("textContent")).strip())
                            .toList();
                    return condition.test(texts) ? texts : null;
                }));
    }

    /** Wartet, bis kein Element zum Selektor mehr sichtbar ist (entfernte Elemente zählen als unsichtbar). */
    public void awaitNoneDisplayed(String css) {
        new WebDriverWait(driver(), LOAD).withMessage("Noch sichtbar: " + css)
                .ignoring(StaleElementReferenceException.class)
                .until(retryingDetached(_ -> all(css).stream().noneMatch(WebElement::isDisplayed) ? Boolean.TRUE : null));
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
        // Layout settles after a navigation (a scroll bar appears, observers resize): only a lasting overflow counts.
        try {
            new WebDriverWait(driver(), Duration.ofSeconds(3)).until(_ -> (Boolean) js.executeScript(
                    "const root = document.documentElement; return root.scrollWidth <= root.clientWidth + 1;"));
        } catch (org.openqa.selenium.TimeoutException _) {
            // reported below with the overflowing elements
        }
        // One script reads both widths and judges them. Read in two calls, a vertical scroll bar appearing in
        // between (the page grows after sign-in) compared the width without the bar to the width with it: a
        // fitting page looked exactly one scroll bar too wide and no element stuck out (CI, 2026-11-16).
        // Also inside shadow roots, and the containers whose content is wider than they are.
        Object overflow = js.executeScript("""
                const limit = document.documentElement.clientWidth;
                if (document.documentElement.scrollWidth <= limit + 1) return '';
                const widths = document.documentElement.scrollWidth + ' > ' + limit + ' px)';
                const found = [];
                const visit = root => root.querySelectorAll('*').forEach(element => {
                    const right = element.getBoundingClientRect().right;
                    if (right > limit || (element.scrollWidth > element.clientWidth + 1
                            && getComputedStyle(element).overflowX === 'visible' && element.clientWidth > 0)) {
                        found.push(element.tagName + '.' + element.className + ': right ' + Math.round(right)
                            + ', scroll ' + element.scrollWidth + '/' + element.clientWidth);
                    }
                    if (element.shadowRoot) visit(element.shadowRoot);
                });
                visit(document);
                // Hide subtrees one at a time: the path names the culprit even if it is a pseudo-element.
                const root = document.documentElement;
                const fits = () => root.scrollWidth <= root.clientWidth + 1;
                const path = [];
                let element = root;
                for (let depth = 0; depth < 25; depth++) {
                    const culprit = [...element.children].find(child => {
                        const display = child.style.display;
                        child.style.display = 'none';
                        const ok = fits();
                        child.style.display = display;
                        return ok;
                    });
                    if (!culprit) break;
                    path.push(culprit.tagName + (culprit.id ? '#' + culprit.id : '')
                        + (culprit.className && typeof culprit.className === 'string' ? '.' + culprit.className.trim().replace(/\\s+/g, '.') : '')
                        + '[' + Math.round(culprit.getBoundingClientRect().width) + ']');
                    element = culprit;
                }
                const style = getComputedStyle(element);
                return widths + ': body ' + document.body.scrollWidth + '/' + document.body.clientWidth
                    + '; path ' + path.join(' > ') + ' (width ' + style.width + ', min ' + style.minWidth
                    + ', position ' + style.position + ', before ' + getComputedStyle(element, '::before').width
                    + ', after ' + getComputedStyle(element, '::after').width + '); '
                    + found.slice(0, 15).join('; ');
                """);
        assertThat(String.valueOf(overflow))
                .withFailMessage("Seite %s scrollt seitwärts (%s", driver().getCurrentUrl(), overflow)
                .isEmpty();
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
