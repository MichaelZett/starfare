package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.starfare.e2e.Browser;
import io.cucumber.java.de.Dann;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

/** Browser geometry and selection checks against the production renderer. */
public class SystemGallerySteps {
    private final Browser browser;
    public SystemGallerySteps(Browser browser) { this.browser = browser; }

    @Dann("prüfe ich die Systemvergleichsgalerie in beiden Sprachen und Bildschirmgrößen")
    public void gallery() throws IOException {
        browser.open("/acceptance/system-gallery");
        browser.awaitCss("#system-gallery");
        var js = (JavascriptExecutor) browser.driver();
        for (String language : new String[]{"de", "en"}) {
            if (language.equals("en")) {
                browser.awaitCss(".lang-switcher input").sendKeys(Keys.chord(Keys.CONTROL, "a"), "Englisch");
                new org.openqa.selenium.support.ui.WebDriverWait(browser.driver(), java.time.Duration.ofSeconds(10))
                        .ignoring(org.openqa.selenium.StaleElementReferenceException.class).until(_ -> {
                            var item = browser.all("vaadin-combo-box-item").stream()
                                    .filter(option -> option.isDisplayed() && option.getText().equals("Englisch")).findFirst();
                            if (item.isEmpty()) { return false; }
                            item.get().click(); return true;
                        });
                browser.awaitTextIn(".system-schematic-caption", "Star reduced");
            }
            for (int width : new int[]{1366, 1024}) {
                browser.driver().manage().window().setSize(new Dimension(width, 2300));
                js.executeScript("""
                        const gallery = document.querySelector('#system-gallery');
                        gallery.style.cssText = 'display:grid;grid-template-columns:repeat(3,300px);gap:16px;padding:16px;background:#101923';
                        gallery.querySelectorAll('h2').forEach(title => title.style.cssText = 'font-size:14px;margin:8px 0;height:24px');
                        """);
                assertThat(js.executeScript("""
                        const q = selector => document.querySelector(selector);
                        const box = selector => q(selector).getBoundingClientRect();
                        const near = box('#gallery-near .system-world');
                        const far = box('#gallery-far .system-world');
                        const nx = near.x - box('#gallery-near .system-orrery').x;
                        const fx = far.x - box('#gallery-far .system-orrery').x;
                        const small = box('#gallery-DWARF .system-body-sphere');
                        const giant = box('#gallery-GAS_GIANT .system-body-sphere');
                        const stars = [...document.querySelectorAll('#system-gallery .system-star')];
                        const colors = stars.slice(0,5).map(star => getComputedStyle(star).backgroundImage);
                        const worlds = [...document.querySelectorAll('#system-gallery .system-world')];
                        return fx > nx + 20 && giant.width > small.width * 2
                            && new Set(colors).size === 5
                            && worlds.every(world => world.getBoundingClientRect().width >= 44)
                            && box('#gallery-WHITE .system-star').width > box('#gallery-RED .system-star').width
                            && [...document.querySelectorAll('.system-schematic-caption')].every(label => !label.textContent.includes('//'));
                        """)).isEqualTo(true);
                browser.captureUiAcceptance("system-gallery-" + language + "-" + width);
            }
        }
        var star = browser.awaitCss("#gallery-YELLOW .system-star");
        String color = star.getCssValue("background-image");
        new org.openqa.selenium.interactions.Actions(browser.driver()).moveToElement(star, 30, 0).click().perform();
        assertThat(star.getCssValue("background-image")).isEqualTo(color);
        var moon = browser.awaitCss("#gallery-moon .system-colony-moon");
        moon.sendKeys(Keys.ENTER);
        browser.awaitTextIn("#gallery-moon .gallery-colony-inspected", "Kolonie geöffnet");
        assertThat(browser.awaitCss("#gallery-moon .system-colony-moon").getDomAttribute("aria-pressed")).isEqualTo("true");
        String position = (String) js.executeScript("return document.querySelector('#gallery-near .system-world').style.left");
        browser.driver().navigate().refresh();
        browser.awaitCss("#gallery-near .system-world");
        assertThat(js.executeScript("return document.querySelector('#gallery-near .system-world').style.left"))
                .isEqualTo(position);
        Path output = Path.of("build/ui-acceptance/system-gallery.html");
        Files.writeString(output, """
                <!doctype html><html lang="de"><meta charset="utf-8"><title>Systemvergleich N6</title>
                <style>body{background:#101923;color:#edf3fa;font:16px system-ui;margin:24px}img{max-width:100%;height:auto}a{color:#8bc7ff}</style>
                <h1>Systemvergleich N6</h1><p>Produktiver Renderer: Sternarten, Körpertypen, feste Bahnskala, Koloniemond und Atmosphären.
                Größen sind Typklassen; Sterne und Planeten verwenden getrennte Maßstäbe.</p>
                <p><a href="system-gallery-de-1024.png">1024 Pixel</a> · <a href="system-gallery-en-1366.png">Englische Aufnahme</a></p>
                <img src="system-gallery-de-1366.png" alt="Vergleich bei gleichen Kartenbreiten">
                </html>
                """);
        browser.driver().manage().window().setSize(new Dimension(1366, 768));
    }
}
