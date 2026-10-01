package de.zettsystems.starfare.report.values;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BattleReplayTest {

    @Test
    void eventTypeDeterminesWinnerEvenWhenBothArmiesHaveZeroSurvivors() {
        for (TurnEvent event : java.util.List.of(
                new TurnEvent.BattleWon(1, 3, "Vega", 1, 1, 0, false, 1.1, 0.9),
                new TurnEvent.SystemLost(2, 1, 3, "Vega", 1, 1, 0, 1.1, 0.9))) {
            BattleReplay replay = BattleReplay.from(event).orElseThrow();
            assertThat(replay.attackerWon()).isTrue();
            assertThat(replay.attackingRemaining()).isZero();
            assertThat(replay.defendingRemaining()).isZero();
        }
        for (TurnEvent event : java.util.List.of(
                new TurnEvent.BattleLost(1, 3, "Vega", 1, 1, 0, 0.9, 1.1),
                new TurnEvent.DefenseHeld(2, 3, "Vega", 1, 1, 0, 0.9, 1.1))) {
            assertThat(BattleReplay.from(event)).isPresent().get().satisfies(replay ->
                    assertThat(replay.attackerWon()).isFalse());
        }
    }

    @Test
    void winningBattleContainsOnlyCombatNumbers() {
        BattleReplay replay = BattleReplay.from(new TurnEvent.BattleWon(1, 3, "Vega", 20, 12, 7, false))
                .orElseThrow();

        assertThat(replay).isEqualTo(new BattleReplay("Vega", 20, 12, 7, 0));
    }

    @Test
    void losingBattleContainsOnlyCombatNumbers() {
        BattleReplay replay = BattleReplay.from(new TurnEvent.BattleLost(1, 3, "Vega", 20, 12, 4))
                .orElseThrow();

        assertThat(replay).isEqualTo(new BattleReplay("Vega", 20, 12, 0, 4));
    }

    @Test
    void nonCombatEventsDoNotCreateAReplay() {
        assertThat(BattleReplay.from(new TurnEvent.SystemLost(1, 2, 3, "Vega"))).isEmpty();
    }

    @Test
    void systemLossContainsTheDefenderBattleReplay() {
        BattleReplay replay = BattleReplay.from(new TurnEvent.SystemLost(1, 2, 3, "Vega", 20, 12, 7))
                .orElseThrow();

        assertThat(replay).isEqualTo(new BattleReplay("Vega", 20, 12, 7, 0));
    }

    @Test
    void heldDefenseContainsTheDefenderBattleReplay() {
        BattleReplay replay = BattleReplay.from(new TurnEvent.DefenseHeld(1, 3, "Vega", 20, 12, 4))
                .orElseThrow();

        assertThat(replay).isEqualTo(new BattleReplay("Vega", 20, 12, 0, 4));
    }
}
