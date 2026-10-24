package de.zettsystems.starfare.game.values;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PhysicalSystemSizeTest {
    @Test
    void frozenModelSurvivesReloadAndDistinguishesIndividualsWithoutChangingComposition() {
        var body = new SystemComposition.Body(1, SystemComposition.BodyType.ROCKY, 1, false, 0);
        assertThat(PhysicalSystemSize.bodyRadius(GameId.of("radii-fixture"), 17, body)).isEqualTo(7_300);
        assertThat(PhysicalSystemSize.bodyRadius(GameId.of("radii-fixture"), 17, body)).isEqualTo(7_300);
        assertThat(IntStream.range(1, 30).map(id -> PhysicalSystemSize.bodyRadius(GameId.of("radii-fixture"), id, body))
                .distinct().count()).isGreaterThan(1);
        assertThat(body).isEqualTo(new SystemComposition.Body(1, SystemComposition.BodyType.ROCKY, 1, false, 0));
    }

    @Test
    void allWorldsAndMoonsShareOneBoundedMonotoneScale() {
        int previous = 0;
        for (int radius = 100; radius <= 80_000; radius += 100) {
            int pixels = PhysicalSystemSize.bodyDiameter(radius);
            assertThat(pixels).isBetween(8, 40).isGreaterThanOrEqualTo(previous);
            previous = pixels;
        }
        assertThatThrownBy(() -> PhysicalSystemSize.bodyDiameter(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void starsUseTheirSeparateScaleAndBeltsHaveNoFabricatedSingleRadius() {
        var game = GameId.of("radii-fixture");
        assertThat(PhysicalSystemSize.starRadius(game, 17, SystemComposition.StarType.RED, 0)).isBetween(100_000, 400_000);
        assertThat(PhysicalSystemSize.starDiameter(100_000)).isEqualTo(66);
        assertThat(PhysicalSystemSize.starDiameter(1_800_000)).isEqualTo(120);
        assertThat(PhysicalSystemSize.moonRadius(game, 17, 1, 0)).isBetween(300, 3_500);
        var belt = new SystemComposition.Body(2, SystemComposition.BodyType.ASTEROIDS, 0, false, 0);
        assertThatThrownBy(() -> PhysicalSystemSize.bodyRadius(game, 17, belt)).isInstanceOf(IllegalArgumentException.class);
    }
}
