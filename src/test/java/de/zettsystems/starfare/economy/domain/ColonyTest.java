package de.zettsystems.starfare.economy.domain;

import de.zettsystems.starfare.economy.values.SystemQuality;
import de.zettsystems.starfare.game.values.SystemComposition;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ColonyTest {
    private static SystemQuality quality(double light, double orbit, SystemQuality.Atmosphere atmosphere, int richness) {
        return new SystemQuality(SystemComposition.StarType.YELLOW, light, orbit,
                SystemComposition.BodyType.ROCKY, false, atmosphere, richness);
    }
    @Test void suitabilityUsesLightDistanceAndAtmosphereRatherThanStarColor() {
        var earth = quality(1, 1, SystemQuality.Atmosphere.TEMPERATE, 1);
        var sameFlux = quality(4, 2, SystemQuality.Atmosphere.TEMPERATE, 5);
        assertThat(earth.stellarFlux()).isEqualTo(sameFlux.stellarFlux());
        assertThat(earth.suitability()).isEqualTo(SystemQuality.Suitability.FERTILE).isEqualTo(sameFlux.suitability());
        assertThat(quality(1, 1, SystemQuality.Atmosphere.THIN, 1).suitability()).isEqualTo(SystemQuality.Suitability.HABITABLE);
        assertThat(quality(1, 1, SystemQuality.Atmosphere.NONE, 1).suitability()).isEqualTo(SystemQuality.Suitability.HARSH);
        assertThat(quality(1, 0.1, SystemQuality.Atmosphere.TEMPERATE, 1).suitability()).isEqualTo(SystemQuality.Suitability.HARSH);
        assertThat(quality(1, 10, SystemQuality.Atmosphere.TEMPERATE, 1).suitability()).isEqualTo(SystemQuality.Suitability.HARSH);
    }
    @Test void fractionalGrowthRemainsSavedAndStopsExactlyAtTheEnvironmentLimit() {
        var quality = quality(1, 1, SystemQuality.Atmosphere.TEMPERATE, 3);
        assertThat(new Colony(690).grow(quality)).isEqualTo(new Colony(711));
        assertThat(new Colony(100).grow(quality).population()).isEqualTo(103);
        assertThat(new Colony(4999).grow(quality)).isEqualTo(new Colony(5000));
        assertThat(new Colony(5000).grow(quality)).isEqualTo(new Colony(5000));
        assertThatThrownBy(() -> new Colony(99)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ColonyEconomy(quality(1, 1, SystemQuality.Atmosphere.NONE, 3), new Colony(2501)))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void invalidPhysicalInputsAreRejectedAndMoonsCanHostColonies() {
        assertThatThrownBy(() -> quality(Double.NaN, 1, SystemQuality.Atmosphere.NONE, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> quality(1, 0, SystemQuality.Atmosphere.NONE, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> quality(1, 1, SystemQuality.Atmosphere.NONE, 6)).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> new SystemQuality(SystemComposition.StarType.RED, 0.04, 0.2,
                SystemComposition.BodyType.GAS_GIANT, true, SystemQuality.Atmosphere.TEMPERATE, 2)).doesNotThrowAnyException();
        assertThatThrownBy(() -> new SystemQuality(SystemComposition.StarType.RED, 0.04, 0.2,
                SystemComposition.BodyType.GAS_GIANT, false, SystemQuality.Atmosphere.TEMPERATE, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
