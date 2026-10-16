package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.starfare.e2e.Browser;
import io.cucumber.java.de.Dann;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.io.IOException;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;

public class RouteAcceptanceSteps {
    private final Browser browser;
    public RouteAcceptanceSteps(Browser browser) { this.browser = browser; }

    @Dann("prüfe ich lange und unbekannte Routen in beiden Sprachen und Bildschirmgrößen")
    public void inspect() throws IOException {
        browser.open("/acceptance/routes"); browser.awaitCss("#route-acceptance");
        assertThat(browser.all("#route-blocked .route-stop").stream()
                .map(point -> point.getDomAttribute("data-route-system")).toList()).containsExactly("1", "5", "6", "7");
        browser.type("#route-preview .route-more", Keys.ENTER);
        new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> browser.all("#route-preview .route-stop").size() == 7);
        assertThat(browser.awaitCss("#route-preview [data-route-system='2']").getText()).startsWith("◇");
        assertThat(browser.awaitCss("#route-unknown [data-route-system='99']").getText()).isEqualTo("?");
        assertThat(browser.awaitCss("#route-unknown [data-route-system='99']").getDomAttribute("disabled")).isNotNull();
        browser.type("#route-direct [data-route-system='7']", Keys.ENTER);
        browser.awaitCss("#route-direct .route-selected");
        for (String language : new String[]{"de", "en"}) {
            if (language.equals("en")) {
                browser.type(".lang-switcher input", Keys.chord(Keys.CONTROL, "a"), "Englisch");
                browser.clickMatching("vaadin-combo-box-item", option -> option.getText().equals("Englisch"), "Sprache Englisch");
                browser.type("#route-preview .route-more", Keys.ENTER);
                new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ -> browser.all("#route-preview .route-stop").size() == 7);
            }
            for (int width : new int[]{1366, 1024}) {
                browser.driver().manage().window().setSize(new Dimension(width, 1000));
                var js = (JavascriptExecutor) browser.driver();
                js.executeScript("document.querySelector('#route-acceptance').style.cssText='width:360px;padding:16px;margin:24px;background:#202a36;color:white'");
                assertThat(js.executeScript("""
                        return [...document.querySelectorAll('.route-stops')].every(strip => strip.scrollWidth <= strip.clientWidth + 1)
                            && document.querySelector('#route-blocked .route-blocked').getAttribute('data-route-system') === '5'
                            && document.querySelector('#route-blocked .route-next').getAttribute('data-route-system') === '6';
                        """)).isEqualTo(true);
                browser.captureUiAcceptance("routes-" + language + "-" + width);
            }
        }
        browser.driver().manage().window().setSize(new Dimension(1366, 768));
    }
}
