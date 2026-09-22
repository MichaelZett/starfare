package de.zettsystems.starfare.e2e.steps;

import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.starfare.e2e.Browser;
import de.zettsystems.starfare.game.application.Broadcaster;
import de.zettsystems.starfare.game.application.GameEvent;
import de.zettsystems.starfare.game.application.GameRegistry;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import io.cucumber.java.After;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** A deterministic final round exercises the browser journey independently of combat luck. */
public class BattleSteps {
    private static final String PENDING = ".event-battle-pending";
    private final Browser browser;
    private final GameService games;
    private final GameRegistry registry;
    private final Broadcaster broadcaster;
    private final UserAccountService accounts;
    private Dimension originalSize;

    public BattleSteps(Browser browser, GameService games, GameRegistry registry,
                       Broadcaster broadcaster, UserAccountService accounts) {
        this.browser = browser;
        this.games = games;
        this.registry = registry;
        this.broadcaster = broadcaster;
        this.accounts = accounts;
    }

    @Wenn("eine Partie mit zwei abschließenden Schlachten für {string} bereitsteht")
    public void prepareFinalRound(String email) {
        String account = String.valueOf(accounts.findByEmail(email).orElseThrow().id());
        GameId id = games.newGame(GameSetup.defaults(), account, "Finale Schlachten");
        int seat = games.joinGame(id, account, "Battle Tester", "Battle Empire").orElseThrow();
        assertThat(games.startGame(id)).isTrue();
        browser.open("/map/" + id.value());
        browser.awaitCss(".map-content");
        registry.writeState(id, state -> {
            var first = state.systems().get(1);
            var second = state.systems().get(2);
            state.reports().put(seat, new TurnReport(state.turn(), List.of(), List.of(
                    new TurnEvent.BattleWon(seat, first.id(), first.name(), 60, 40, 20, true),
                    new TurnEvent.BattleWon(seat, second.id(), second.name(), 90, 30, 60, true),
                    new TurnEvent.Victory(seat))));
            state.endGame(seat);
            state.nextTurn();
            return null;
        });
        broadcaster.publish(new GameEvent.GameFinished(id, seat));
    }

    @Dann("bleiben Sieg und offene Karte bis zur letzten Schlacht verborgen")
    public void outcomeWaitsForBattles() {
        browser.awaitCss(PENDING);
        assertConcealed();
        browser.driver().navigate().refresh();
        browser.awaitCss(".map-content");
        browser.clickTabWithText("Bericht");
        browser.awaitCss(PENDING);
        assertConcealed();
    }

    private void assertConcealed() {
        assertThat(browser.all(PENDING)).hasSize(2);
        assertThat(browser.all(".event-victory")).isEmpty();
        assertThat(browser.all(".review-controls")).noneMatch(WebElement::isDisplayed);
        assertThat(browser.all("#map .sys-fog")).isNotEmpty();
    }

    @Dann("kann ich die Kartenbreite durch Ziehen ändern")
    public void resizeMap() throws IOException {
        originalSize = browser.driver().manage().window().getSize();
        browser.driver().manage().window().setSize(new Dimension(1024, 768));
        WebElement layout = browser.awaitCss("vaadin-split-layout");
        WebElement splitter = layout.getShadowRoot().findElement(By.cssSelector("[part='splitter']"));
        WebElement map = browser.awaitCss(".map-left");
        int before = map.getSize().getWidth();
        new Actions(browser.driver()).dragAndDropBy(splitter, -120, 0).perform();
        assertThat(map.getSize().getWidth()).isLessThan(before - 50);
        browser.assertNoSidewaysScrolling();
        screenshot("map-1024");
    }

    @Wenn("ich beide Schlachten mit Ton auswerte")
    public void playBattles() throws IOException {
        for (int battle = 0; battle < 2; battle++) {
            browser.awaitCss(PENDING).click();
            browser.awaitCss(".battle-replay");
            browser.awaitCss(".sys-report-event-active");
            new WebDriverWait(browser.driver(), Duration.ofSeconds(10)).until(_ ->
                    "running".equals(js().executeScript("return window.starfareBattleAudio.context?.state")));
            if (battle == 0) {
                verifyAudioOutput();
            }
            browser.awaitCss(".battle-replay-result-visible");
            screenshot("battle-result-" + battle);
            assertThat(browser.textsOf("[data-battle-attacking]")).contains(battle == 0 ? "20" : "60");
            assertThat(browser.textsOf("[data-battle-defending]")).contains("0");
            browser.clickButtonWithText("Ergebnis übernehmen");
            if (battle == 0) {
                new WebDriverWait(browser.driver(), Duration.ofSeconds(10))
                        .until(_ -> browser.all(PENDING).size() == 1);
                assertThat(browser.all(PENDING)).hasSize(1);
                assertThat(browser.all(".event-victory")).isEmpty();
            }
        }
    }

    @Dann("erscheint das Spielergebnis vor der offenen Nachbetrachtung")
    public void outcomeThenReview() {
        browser.awaitText("Sieg:");
        assertThat(browser.all(".review-controls")).noneMatch(WebElement::isDisplayed);
        browser.clickButtonWithText("Zum Endstand");
        browser.awaitCss(".review-controls");
        assertThat(browser.all("#map .sys-fog")).isEmpty();
    }

    private JavascriptExecutor js() {
        return (JavascriptExecutor) browser.driver();
    }

    @After
    public void restoreWindowSize() {
        if (originalSize != null) {
            browser.driver().manage().window().setSize(originalSize);
        }
    }

    private void screenshot(String name) throws IOException {
        Path directory = Path.of("build", "e2e-validation");
        Files.createDirectories(directory);
        Files.write(directory.resolve(name + ".png"),
                ((TakesScreenshot) browser.driver()).getScreenshotAs(OutputType.BYTES));
    }

    private void verifyAudioOutput() {
        js().executeScript("""
                const context = window.starfareBattleAudio.context;
                const analyser = context.createAnalyser();
                window.battleTestAnalyser = analyser;
                const createGain = context.createGain.bind(context);
                context.createGain = () => {
                    const gain = createGain();
                    gain.connect(analyser);
                    return gain;
                };
                """);
        new WebDriverWait(browser.driver(), Duration.ofSeconds(5)).until(_ -> Boolean.TRUE.equals(
                js().executeScript("""
                        const data = new Float32Array(window.battleTestAnalyser.fftSize);
                        window.battleTestAnalyser.getFloatTimeDomainData(data);
                        return data.some(value => Math.abs(value) > 0.001);
                        """)));
        WebElement sound = browser.awaitCss(".battle-replay-dialog vaadin-checkbox");
        sound.click();
        assertThat(js().executeScript("return window.starfareBattleAudio.enabled")).isEqualTo(false);
        sound.click();
        assertThat(js().executeScript("return window.starfareBattleAudio.enabled")).isEqualTo(true);
    }
}
