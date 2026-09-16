package de.zettsystems.starfare.report.values;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BattleReplayTest {

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
}
