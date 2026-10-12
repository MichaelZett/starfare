package de.zettsystems.starfare.report.values;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class CombatReadinessTest {
    @Test void thresholdsUseOpeningStrengthWithInclusiveNeutralAndFivePercentBoundaries() {
        assertThat(CombatReadiness.from(100, 94, 1).level()).isZero();
        assertThat(CombatReadiness.from(100, 95, 1).level()).isEqualTo(1);
        assertThat(CombatReadiness.from(100, 99, 1).level()).isEqualTo(2);
        assertThat(CombatReadiness.from(100, 101, 1).level()).isEqualTo(2);
        assertThat(CombatReadiness.from(100, 105, 1).level()).isEqualTo(3);
        assertThat(CombatReadiness.from(100, 106, 1).level()).isEqualTo(4);
        assertThat(CombatReadiness.from(0, 0, 1).level()).isEqualTo(2);
        assertThat(CombatReadiness.from(100, 100, 1)).isEqualTo(CombatReadiness.from(100, 100, 1));
    }
    @Test void coalitionDefenderUsesOriginalOwnerEvenWhenAttackerHasSmallerIdAndWins() {
        var attacker = new CoalitionSide(1, 10, List.of(new CoalitionSide.Member(1, "A", 10, 5, 2)));
        var defender = new CoalitionSide(4, 2, List.of(new CoalitionSide.Member(4, "D", 2, 0, 5)));
        var replay = BattleReplay.from(new TurnEvent.CoalitionBattle(3, "Vega", 4, 1, "A", List.of(defender, attacker))).orElseThrow();
        assertThat(replay.defenderSideId()).isEqualTo(4);
        assertThat(replay.coalitions()).containsExactly(defender, attacker);
        var neutral = new CoalitionSide(0, 2, List.of(new CoalitionSide.Member(0, "", 2, 0, 5)));
        assertThat(BattleReplay.from(new TurnEvent.CoalitionBattle(3, "Vega", null, 1, "A", List.of(attacker, neutral)))
                .orElseThrow().defenderSideId()).isZero();
        assertThat(BattleReplay.from(new TurnEvent.CoalitionBattle(3, "Vega", 9, 1, "A", List.of(attacker)))
                .orElseThrow().defenderSideId()).isNull();
    }
}
