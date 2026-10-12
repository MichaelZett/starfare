package de.zettsystems.starfare.game.values;

import de.zettsystems.starfare.economy.values.SystemQuality;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SystemDiagramLayoutTest {
    @Test
    void physicalDistanceIsComparableAcrossSinglePlanetSystems() {
        var near = colony(0.8);
        var far = colony(2.4);
        assertThat(SystemDiagramLayout.orbitPosition(far, far.bodies().getFirst()))
                .isGreaterThan(SystemDiagramLayout.orbitPosition(near, near.bodies().getFirst()));
        double previous = 0;
        for (double distance : new double[]{0.1, 0.2, 0.8, 2.4, 6.4, 20, 100}) {
            double next = SystemDiagramLayout.distancePosition(distance);
            assertThat(next).isGreaterThan(previous).isBetween(25.0, 87.0);
            previous = next;
        }
        assertThatThrownBy(() -> SystemDiagramLayout.distancePosition(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decorativeOrbitsKeepTheirPositionWhenOtherBodiesArePresent() {
        var body = new SystemComposition.Body(1, SystemComposition.BodyType.ROCKY, 0, false, 0);
        var single = new SystemComposition(List.of(SystemComposition.StarType.YELLOW), List.of(body));
        var multiple = new SystemComposition(single.stars(), List.of(body,
                new SystemComposition.Body(2, SystemComposition.BodyType.GAS_GIANT, 2, true, 0)));
        assertThat(SystemDiagramLayout.orbitPosition(single, body)).isEqualTo(SystemDiagramLayout.orbitPosition(multiple, body));
        assertThat(SystemDiagramLayout.bodyDiameter(SystemComposition.BodyType.DWARF))
                .isLessThan(SystemDiagramLayout.bodyDiameter(SystemComposition.BodyType.ROCKY));
        assertThat(SystemDiagramLayout.bodyDiameter(SystemComposition.BodyType.ROCKY))
                .isLessThan(SystemDiagramLayout.bodyDiameter(SystemComposition.BodyType.SUPER_EARTH));
        assertThat(SystemDiagramLayout.bodyDiameter(SystemComposition.BodyType.SUPER_EARTH))
                .isLessThan(SystemDiagramLayout.bodyDiameter(SystemComposition.BodyType.GAS_GIANT));
        assertThat(SystemDiagramLayout.starDiameter(SystemComposition.StarType.WHITE))
                .isGreaterThan(SystemDiagramLayout.starDiameter(SystemComposition.StarType.RED));
    }

    private static SystemComposition colony(double orbit) {
        var quality = new SystemQuality(SystemComposition.StarType.YELLOW, 1, orbit,
                SystemComposition.BodyType.ROCKY, false, SystemQuality.Atmosphere.TEMPERATE, 3);
        return new SystemComposition(List.of(quality.star()),
                List.of(new SystemComposition.Body(1, quality.body(), 0, false, 0)), quality);
    }
}
