package de.zettsystems.starfare.game.values;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class GameConfigTest {

    @Test
    void defaultsArePositive() {
        assertThat(GameConfig.DEFAULT_SYSTEM_COUNT).isGreaterThan(0);
        assertThat(GameConfig.DEFAULT_HUMAN_PLAYERS).isGreaterThan(0);
        assertThat(GameConfig.DEFAULT_AI_PLAYERS).isGreaterThanOrEqualTo(0);
        assertThat(GameConfig.DEFAULT_START_SYSTEM_PRODUCTION).isGreaterThan(0);
        assertThat(GameConfig.DEFAULT_NEUTRAL_MIN_PRODUCTION).isGreaterThan(0);
        assertThat(GameConfig.DEFAULT_NEUTRAL_MAX_PRODUCTION).isGreaterThanOrEqualTo(GameConfig.DEFAULT_NEUTRAL_MIN_PRODUCTION);
        assertThat(GameConfig.SPACEOUT_ITERATIONS).isGreaterThan(0);
        assertThat(GameConfig.SPACEOUT_MIN_DIST).isGreaterThan(0.0);
    }

    @Test
    void paletteHasNoDuplicates() {
        var colors = GameConfig.PLAYER_PALETTE;
        var unique = new HashSet<>(colors);
        assertThat(unique).hasSameSizeAs(colors);
    }
}
