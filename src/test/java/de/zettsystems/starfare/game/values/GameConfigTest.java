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
        assertThat(GameConfig.START_PRODUCTION).isPositive();
        assertThat(GameConfig.START_GARRISON).isGreaterThanOrEqualTo(GameConfig.START_PRODUCTION);
        assertThat(GameConfig.MAX_PRODUCTION_CHOICES).contains(GameConfig.DEFAULT_MAX_PRODUCTION)
                .allSatisfy(highest -> assertThat(highest).isBetween(GameConfig.MIN_PRODUCTION, GameConfig.MAX_PRODUCTION));
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
