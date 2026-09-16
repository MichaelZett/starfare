package de.zettsystems.starfare.game.values;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class GameConfigTest {

    @Test
    void defaultsArePositive() {
        assertThat(GameConfig.DEFAULT_SYSTEM_COUNT).isPositive();
        assertThat(GameConfig.DEFAULT_HUMAN_PLAYERS).isPositive();
        assertThat(GameConfig.DEFAULT_AI_PLAYERS).isNotNegative();
        assertThat(GameConfig.DEFAULT_START_SYSTEM_PRODUCTION).isPositive();
        assertThat(GameConfig.DEFAULT_NEUTRAL_MIN_PRODUCTION).isPositive();
        assertThat(GameConfig.DEFAULT_NEUTRAL_MAX_PRODUCTION)
                .isGreaterThanOrEqualTo(GameConfig.DEFAULT_NEUTRAL_MIN_PRODUCTION)
                .isEqualTo(10);
        assertThat(GameConfig.DEFAULT_PRODUCTION_DISTRIBUTION).isEqualTo(ProductionDistribution.GAUSSIAN);
        assertThat(GameConfig.DEFAULT_GALAXY_LAYOUT).isEqualTo(GalaxyLayout.EVEN);
        assertThat(GameConfig.SPACEOUT_ITERATIONS).isPositive();
        assertThat(GameConfig.SPACEOUT_MIN_DIST).isGreaterThan(0.0);
    }

    @Test
    void paletteHasNoDuplicates() {
        var colors = GameConfig.PLAYER_PALETTE;
        var unique = new HashSet<>(colors);
        assertThat(unique).hasSameSizeAs(colors);
    }
}
