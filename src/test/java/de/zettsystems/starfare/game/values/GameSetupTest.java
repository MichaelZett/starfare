package de.zettsystems.starfare.game.values;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameSetupTest {

    private static GameSetup setup(int humans, int ai, boolean observersAllowed) {
        return new GameSetup(
                24, humans, ai,
                List.of(),
                2, 6, 8,
                observersAllowed, true, List.of()
        );
    }

    @Test
    void multipleHumansSurviveNormalization() {
        GameSetup setup = new GameSetup(
                24, 3, 1,
                List.of(4, 4, 4, 4),
                2, 6, 8,
                true, true, List.of()
        ).normalized();

        assertThat(setup.humanPlayers()).isEqualTo(3);
        assertThat(setup.aiPlayers()).isOne();
        assertThat(setup.totalPlayers()).isEqualTo(4);
    }

    @Test
    void humansClampedToMinimumWhenObserversNotAllowed() {
        GameSetup setup = setup(0, 2, false).normalized();

        assertThat(setup.humanPlayers()).isEqualTo(GameConfig.MIN_HUMAN_PLAYERS);
    }

    @Test
    void humansMayBeZeroWhenObserversAllowed() {
        GameSetup setup = setup(0, 2, true).normalized();

        assertThat(setup.humanPlayers()).isZero();
        assertThat(setup.observersAllowed()).isTrue();
    }

    @Test
    void humansClampedToMaximum() {
        GameSetup setup = setup(99, 0, true).normalized();

        assertThat(setup.humanPlayers()).isEqualTo(GameConfig.MAX_HUMAN_PLAYERS);
    }

    @Test
    void totalPlayersCannotExceedMaxTotal() {
        GameSetup setup = setup(GameConfig.MAX_HUMAN_PLAYERS, GameConfig.MAX_AI_PLAYERS, true).normalized();

        assertThat(setup.totalPlayers()).isLessThanOrEqualTo(GameConfig.MAX_TOTAL_PLAYERS);
    }

    @Test
    void startProductionListMatchesTotalPlayers() {
        GameSetup setup = new GameSetup(
                24, 2, 1,
                List.of(5),
                2, 6, 8,
                true, true, List.of()
        ).normalized();

        assertThat(setup.startProductionPerPlayer()).hasSize(setup.totalPlayers());
        assertThat(setup.startProductionForSeat(0)).isEqualTo(5);
        assertThat(setup.startProductionForSeat(1)).isEqualTo(GameConfig.DEFAULT_START_SYSTEM_PRODUCTION);
        assertThat(setup.startProductionForSeat(2)).isEqualTo(GameConfig.DEFAULT_START_SYSTEM_PRODUCTION);
    }

    @Test
    void defaultsHaveObserversAllowedAndReentryAllowed() {
        GameSetup defaults = GameSetup.defaults();

        assertThat(defaults.observersAllowed()).isTrue();
        assertThat(defaults.reentryAllowed()).isTrue();
    }

    @Test
    void flagsSurviveNormalization() {
        GameSetup normalized = new GameSetup(
                24, 2, 1,
                List.of(4, 4, 4),
                2, 6, 8,
                false, false, List.of()
        ).normalized();

        assertThat(normalized.observersAllowed()).isFalse();
        assertThat(normalized.reentryAllowed()).isFalse();
    }

    @Test
    void seatColorsDefaultToPaletteOrder() {
        GameSetup setup = new GameSetup(
                24, 2, 1,
                List.of(4, 4, 4),
                2, 6, 8,
                true, true, List.of()
        ).normalized();

        assertThat(setup.seatColorHexes()).hasSize(3);
        assertThat(setup.colorForSeat(0)).isEqualTo(GameConfig.PLAYER_PALETTE.get(0));
        assertThat(setup.colorForSeat(1)).isEqualTo(GameConfig.PLAYER_PALETTE.get(1));
        assertThat(setup.colorForSeat(2)).isEqualTo(GameConfig.PLAYER_PALETTE.get(2));
    }

    @Test
    void chosenSeatColorsSurviveNormalization() {
        String third = GameConfig.PLAYER_PALETTE.get(2);
        String first = GameConfig.PLAYER_PALETTE.get(0);

        GameSetup setup = new GameSetup(
                24, 2, 0,
                List.of(4, 4),
                2, 6, 8,
                true, true, List.of(third, first)
        ).normalized();

        assertThat(setup.colorForSeat(0)).isEqualTo(third);
        assertThat(setup.colorForSeat(1)).isEqualTo(first);
    }

    @Test
    void duplicateSeatColorsAreReplacedByFreePaletteEntries() {
        String same = GameConfig.PLAYER_PALETTE.get(1);

        GameSetup setup = new GameSetup(
                24, 3, 0,
                List.of(4, 4, 4),
                2, 6, 8,
                true, true, List.of(same, same, same)
        ).normalized();

        assertThat(setup.seatColorHexes()).doesNotHaveDuplicates();
        assertThat(setup.colorForSeat(0)).isEqualTo(same);
    }

    @Test
    void unknownSeatColorFallsBackToPalette() {
        GameSetup setup = new GameSetup(
                24, 2, 0,
                List.of(4, 4),
                2, 6, 8,
                true, true, List.of("not-a-color", "#123456")
        ).normalized();

        assertThat(setup.seatColorHexes()).allMatch(GameConfig.PLAYER_PALETTE::contains);
        assertThat(setup.seatColorHexes()).doesNotHaveDuplicates();
    }

    @Test
    void seatColorsCoverEveryPlayerAfterClamping() {
        GameSetup setup = setup(GameConfig.MAX_HUMAN_PLAYERS, GameConfig.MAX_AI_PLAYERS, true).normalized();

        assertThat(setup.seatColorHexes()).hasSize(setup.totalPlayers());
    }
}
