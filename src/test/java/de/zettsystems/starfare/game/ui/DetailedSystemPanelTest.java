package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.SystemComposition;
import de.zettsystems.starfare.game.values.SystemComposition.Body;
import de.zettsystems.starfare.game.values.SystemComposition.BodyType;
import de.zettsystems.starfare.game.values.SystemComposition.StarType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DetailedSystemPanelTest {
    private static final String PRESSED = "aria-pressed";
    private static final SystemComposition COMPOSITION = new SystemComposition(List.of(StarType.YELLOW, StarType.RED),
            List.of(new Body(1, BodyType.ROCKY, 1, false, 0),
                    new Body(2, BodyType.ASTEROIDS, 0, false, 0),
                    new Body(3, BodyType.SUPER_EARTH, 0, false, 1),
                    new Body(4, BodyType.GAS_GIANT, 2, true, 2),
                    new Body(5, BodyType.ICE_GIANT, 1, false, 3),
                    new Body(6, BodyType.DWARF, 0, false, 0)));

    private final DetailedSystemPanel panel = new DetailedSystemPanel(COMPOSITION, false, GameId.of("system-panel"), 3);

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            1 | Gesteinsplanet    | Feste Oberfläche aus Gestein und Metall.
            2 | Asteroidengürtel  | Viele kleine Körper auf ähnlichen Bahnen.
            3 | Supererde         | Größer beziehungsweise massereicher als die Erde
            4 | Gasriese          | mächtigen Hülle aus Wasserstoff und Helium
            5 | Eisriese          | Neptunähnlicher Planet mit tiefer Atmosphäre
            6 | Zwergplanet       | hat er seine Bahnumgebung nicht freigeräumt
            """)
    void theCatalogDescribesEachKindOfBody(int orbit, String name, String description) {
        catalogButton(orbit).click();

        assertThat(description()).startsWith("Umlaufbahn " + orbit + " · " + name).contains(description)
                .doesNotContain("Gelber Stern");
    }

    @Test
    void selectingABodyHighlightsOnlyItsWorldOnTheDiagram() {
        catalogButton(4).click();

        List<Button> worlds = descendants().filter(button -> button.hasClassName("system-world")).toList();
        assertThat(worlds).hasSize(6);
        assertThat(worlds).extracting(world -> world.getElement().getAttribute(PRESSED))
                .containsExactly("false", "false", "false", "true", "false", "false");
        assertThat(descendants().filter(button -> button.hasClassName("system-star")))
                .extracting(star -> star.getElement().getAttribute(PRESSED)).containsOnly("false");
    }

    @Test
    void planetsListRadiusAndEachDrawnMoon() {
        catalogButton(4).click();

        assertThat(description()).contains("Radius ", "Dargestellte Hauptmonde: 2", "Mond 1: Radius", "Mond 2: Radius")
                .doesNotContain("Mond 3:");
    }

    @Test
    void anAsteroidBeltHasNeitherRadiusNorMoons() {
        catalogButton(2).click();

        assertThat(description()).doesNotContain("Radius", "Hauptmonde");
    }

    @Test
    void clickingAWorldOnTheDiagramSelectsItAndReportsTheInspection() {
        AtomicInteger inspections = new AtomicInteger();
        panel.onInspect(inspections::incrementAndGet);

        descendants().filter(button -> button.hasClassName("system-world")).toList().get(2).click();

        assertThat(description()).startsWith("Umlaufbahn 3 · Supererde").contains("Dargestellte Hauptmonde: 0");
        assertThat(inspections).hasValue(1);
    }

    private Button catalogButton(int orbit) {
        return descendants(panel.background()).filter(button -> button.hasClassName("system-catalog-button"))
                .filter(button -> button.getText().startsWith("Umlaufbahn " + orbit + " ")).findFirst().orElseThrow();
    }

    private String description() {
        Component description = panel.background().getChildren().findFirst().orElseThrow();
        return description.getElement().getTextRecursively();
    }

    private Stream<Button> descendants() {
        return descendants(panel);
    }

    private static Stream<Button> descendants(Component root) {
        return all(root).filter(Button.class::isInstance).map(Button.class::cast);
    }

    private static Stream<Component> all(Component component) {
        return component.getChildren().flatMap(child -> Stream.concat(Stream.of(child), all(child)));
    }
}
