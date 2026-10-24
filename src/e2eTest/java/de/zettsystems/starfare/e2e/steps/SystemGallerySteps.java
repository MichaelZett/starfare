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
                browser.type(".lang-switcher input", Keys.chord(Keys.CONTROL, "a"), "Englisch");
                browser.clickMatching("vaadin-combo-box-item", option -> option.getText().equals("Englisch"), "Sprache Englisch");
                browser.awaitTextIn(".system-schematic-caption", "Star reduced");
            }
            for (int width : new int[]{1366, 1024}) {
                browser.driver().manage().window().setSize(new Dimension(width, 2300));
                js.executeScript("""
                        const gallery = document.querySelector('#system-gallery');
                        gallery.style.cssText = 'display:grid;grid-template-columns:repeat(3,300px);gap:16px;padding:16px;background:#101923';
                        gallery.querySelectorAll('h2').forEach(title => title.style.cssText = 'font-size:14px;margin:8px 0;height:24px');
                        """);
                assertThat((java.util.Map<?, ?>) js.executeScript("""
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
                        const measured = worlds.filter(world => world.hasAttribute('data-model-radius-km'))
                            .map(world => ({radius: Number(world.dataset.modelRadiusKm),
                                width: parseFloat(getComputedStyle(world.querySelector('.system-body-sphere')).width)}))
                            .sort((a, b) => a.radius - b.radius);
                        return {
                            orbits: fx > nx + 20,
                            sizeOrder: giant.width > small.width,
                            modelScale: measured.length >= 10 && measured.every((body, index) => body.radius > 0
                                && (index === 0 || body.width >= measured[index - 1].width - 0.1)),
                            colors: new Set(colors).size === 5,
                            targets: worlds.every(world => world.getBoundingClientRect().width >= 44),
                            stars: box('#gallery-WHITE .system-star').width > box('#gallery-RED .system-star').width,
                            captions: [...document.querySelectorAll('.system-schematic-caption')].every(label => !label.textContent.includes('//'))
                        };
                        """)).allSatisfy((name, passed) -> assertThat(passed).as(String.valueOf(name)).isEqualTo(true));
                browser.captureUiAcceptance("system-gallery-" + language + "-" + width);
            }
        }
        var star = browser.awaitCss("#gallery-YELLOW .system-star");
        String color = star.getCssValue("background-image");
        new org.openqa.selenium.interactions.Actions(browser.driver()).moveToElement(star, 30, 0).click().perform();
        assertThat(star.getCssValue("background-image")).isEqualTo(color);
        browser.type("#gallery-moon .system-colony-moon", Keys.ENTER);
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
                Individuelle Größen stammen aus fiktiven Modellradien; Sterne und Planeten verwenden getrennte Maßstäbe.</p>
                <p><a href="system-gallery-de-1024.png">1024 Pixel</a> · <a href="system-gallery-en-1366.png">Englische Aufnahme</a></p>
                <img src="system-gallery-de-1366.png" alt="Vergleich bei gleichen Kartenbreiten">
                </html>
                """);
        browser.driver().manage().window().setSize(new Dimension(1366, 768));
    }
}
