package de.zettsystems.starfare.e2e;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Ein Browser für die ganze Suite — der Start kostet Sekunden. Szenarien
 * trennt das Löschen der Cookies im Vorher-Hook.
 *
 * <p>HD-Viewport (1366×768), weil die Oberfläche dafür getunt ist;
 * {@code --lang=de-DE} für die deutsche Oberfläche.
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
            options.addArguments("--window-size=" + WIDTH + "," + HEIGHT, "--lang=de-DE");
            driver = new ChromeDriver(options);
            WebDriver started = driver;
            Runtime.getRuntime().addShutdownHook(new Thread(started::quit));
        }
        return driver;
    }
}
