package de.zettsystems.starfare.economy.domain;

import de.zettsystems.starfare.economy.values.SystemQuality;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ColonyTest {
    private static SystemQuality quality(double light, double orbit, SystemQuality.Atmosphere atmosphere, int richness) {
        return new SystemQuality(SystemQuality.StarType.YELLOW, light, orbit,
                SystemQuality.BodyType.ROCKY, false, atmosphere, richness);
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
        var input1 = quality(1, 1, SystemQuality.Atmosphere.NONE, 3);
        var input2 = new Colony(2501);
        assertThatThrownBy(() -> new ColonyEconomy(input1, input2)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void invalidPhysicalInputsAreRejectedAndMoonsCanHostColonies() {
        assertThatThrownBy(() -> quality(Double.NaN, 1, SystemQuality.Atmosphere.NONE, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> quality(1, 0, SystemQuality.Atmosphere.NONE, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> quality(1, 1, SystemQuality.Atmosphere.NONE, 6)).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> new SystemQuality(SystemQuality.StarType.RED, 0.04, 0.2,
                SystemQuality.BodyType.GAS_GIANT, true, SystemQuality.Atmosphere.TEMPERATE, 2)).doesNotThrowAnyException();
        assertThatThrownBy(() -> new SystemQuality(SystemQuality.StarType.RED, 0.04, 0.2,
                SystemQuality.BodyType.GAS_GIANT, false, SystemQuality.Atmosphere.TEMPERATE, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void populationMayReachButNotExceedTheEnvironmentLimit() {
        var harsh = quality(0.04, 1, SystemQuality.Atmosphere.NONE, 3);
        assertThat(harsh.suitability()).isEqualTo(SystemQuality.Suitability.HARSH);
        assertThat(new ColonyEconomy(harsh, new Colony(2500)).colony().labor()).isEqualTo(25);
        var crowded = new Colony(2501);
        assertThatThrownBy(() -> new ColonyEconomy(harsh, crowded)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void suitabilityThresholdsIncludeTheirBounds() {
        assertThat(quality(0.75, 1, SystemQuality.Atmosphere.TEMPERATE, 1).suitability()).isEqualTo(SystemQuality.Suitability.FERTILE);
        assertThat(quality(1.5, 1, SystemQuality.Atmosphere.TEMPERATE, 1).suitability()).isEqualTo(SystemQuality.Suitability.FERTILE);
        assertThat(quality(0.4, 1, SystemQuality.Atmosphere.THIN, 1).suitability()).isEqualTo(SystemQuality.Suitability.HABITABLE);
        assertThat(quality(2.0, 1, SystemQuality.Atmosphere.THIN, 1).suitability()).isEqualTo(SystemQuality.Suitability.HABITABLE);
        assertThat(quality(0.39, 1, SystemQuality.Atmosphere.THIN, 1).suitability()).isEqualTo(SystemQuality.Suitability.HARSH);
        assertThatThrownBy(() -> quality(0, 1, SystemQuality.Atmosphere.THIN, 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
