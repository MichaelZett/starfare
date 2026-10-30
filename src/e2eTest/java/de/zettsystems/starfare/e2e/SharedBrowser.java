package de.zettsystems.starfare.e2e;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;

import java.util.Map;
import java.util.logging.Level;

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
    static final int PHONE_WIDTH = 390;
    static final int PHONE_HEIGHT = 844;
    static final String PHONE_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) "
            + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1";

    private static WebDriver driver;

    private SharedBrowser() {
    }

    static synchronized WebDriver driver() {
        if (driver == null) {
            driver = createDriver();
            WebDriver started = driver;
            Runtime.getRuntime().addShutdownHook(new Thread(started::quit));
        }
        return driver;
    }

    static WebDriver createDriver() {
        return new ChromeDriver(options(WIDTH, HEIGHT));
    }

    /**
     * Ein Handy: iPhone-Kennung und Touch-Bildschirm über Chromes Geräteemulation. Die Kennung
     * entscheidet serverseitig, welche Oberfläche gebaut wird.
     */
    static WebDriver createPhoneDriver() {
        ChromeOptions options = options(PHONE_WIDTH, PHONE_HEIGHT);
        options.setExperimentalOption("mobileEmulation", Map.of(
                "deviceMetrics", Map.of("width", PHONE_WIDTH, "height", PHONE_HEIGHT, "pixelRatio", 3.0, "touch", true),
                "userAgent", PHONE_USER_AGENT));
        return new ChromeDriver(options);
    }

    private static ChromeOptions options(int width, int height) {
        var manager = WebDriverManager.chromedriver();
        String requestedBinary = System.getProperty("e2e.chrome.binary");
        if (requestedBinary != null) { manager.browserBinary(requestedBinary); }
        manager.setup();
        ChromeOptions options = new ChromeOptions();
        LoggingPreferences logging = new LoggingPreferences();
        logging.enable(LogType.BROWSER, Level.ALL);
        options.setCapability("goog:loggingPrefs", logging);
        String binary = System.getProperty("e2e.chrome.binary");
        if (binary != null) { options.setBinary(binary); }
        if (Boolean.parseBoolean(System.getProperty("e2e.headless", "true"))) {
            options.addArguments("--headless=new");
        }
        String language = System.getProperty("e2e.lang", "de-DE");
        options.addArguments("--window-size=" + width + "," + height,
                "--lang=" + language, "--accept-lang=" + language, "--mute-audio");
        options.setExperimentalOption("prefs", Map.of("intl.accept_languages", language));
        return options;
    }
}
