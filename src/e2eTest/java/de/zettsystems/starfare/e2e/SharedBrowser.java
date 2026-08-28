package de.zettsystems.starfare.e2e;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.Map;

/**
 * Ein Browser für die ganze Suite — der Start kostet Sekunden. Szenarien
 * trennt das Löschen der Cookies im Vorher-Hook.
 *
 * <p>HD-Viewport (1366×768), weil die Oberfläche dafür getunt ist. Die
 * Oberflächensprache folgt dem {@code Accept-Language}-Header des Browsers:
 * {@code --lang} allein reicht in Headless-Chrome auf Linux nicht (der
 * CI-Lauf vom 2026-08-28 bekam eine englische Lobby), deshalb zusätzlich die
 * Pref {@code intl.accept_languages}.
 */
final class SharedBrowser {

    static final int WIDTH = 1366;
    static final int HEIGHT = 768;

    private static WebDriver driver;

    private SharedBrowser() {
    }

    static synchronized WebDriver driver() {
        if (driver == null) {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            if (Boolean.parseBoolean(System.getProperty("e2e.headless", "true"))) {
                options.addArguments("--headless=new");
            }
            String language = System.getProperty("e2e.lang", "de-DE");
            options.addArguments("--window-size=" + WIDTH + "," + HEIGHT,
                    "--lang=" + language, "--accept-lang=" + language);
            options.setExperimentalOption("prefs", Map.of("intl.accept_languages", language));
            driver = new ChromeDriver(options);
            WebDriver started = driver;
            Runtime.getRuntime().addShutdownHook(new Thread(started::quit));
        }
        return driver;
    }
}
