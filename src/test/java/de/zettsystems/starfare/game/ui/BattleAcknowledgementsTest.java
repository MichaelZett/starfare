package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BattleAcknowledgementsTest {

    @Test
    void acknowledgementOnlyClearsTheChosenBattleInTheCurrentRound() {
        TurnReport report = new TurnReport(4, List.of(), List.of(
                new TurnEvent.BattleWon(1, 10, "Vega", 12, 8, 4, false),
                new TurnEvent.BattleLost(1, 11, "Sirius", 9, 11, 3)));

        assertThat(BattleAcknowledgements.pending(report, Set.of())).containsExactlyInAnyOrder(10, 11);

        int first = BattleAcknowledgements.firstPendingIndexOf(report, report.events().getFirst(), Set.of());

        assertThat(first).isZero();
        assertThat(BattleAcknowledgements.pending(report, Set.of(first))).containsExactly(11);
    }

    @Test
    void nonBattleReportEventsDoNotCreateAnAcknowledgement() {
        TurnReport report = new TurnReport(4, List.of(), List.of(
                new TurnEvent.Production(1, 10, "Vega", 2)));

        assertThat(BattleAcknowledgements.pending(report, Set.of())).isEmpty();
    }

    @Test
    void acknowledgementDoesNotClearAnotherBattleAtTheSameSystem() {
        TurnReport report = new TurnReport(4, List.of(), List.of(
                new TurnEvent.BattleWon(1, 10, "Vega", 12, 8, 4, false),
                new TurnEvent.BattleLost(2, 10, "Vega", 9, 7, 2)));

        assertThat(BattleAcknowledgements.pending(report, Set.of(0))).containsExactly(10);
    }

    @Test
    void identicalBattlesInOneRoundCanBothBeAcknowledged() {
        TurnEvent battle = new TurnEvent.BattleLost(2, 10, "Vega", 9, 7, 2);
        TurnReport report = new TurnReport(4, List.of(), List.of(battle, battle));

        int first = BattleAcknowledgements.firstPendingIndexOf(report, battle, Set.of());
        int second = BattleAcknowledgements.firstPendingIndexOf(report, battle, Set.of(first));

        assertThat(first).isZero();
        assertThat(second).isEqualTo(1);
        assertThat(BattleAcknowledgements.pending(report, Set.of(first))).containsExactly(10);
        assertThat(BattleAcknowledgements.pending(report, Set.of(first, second))).isEmpty();
        assertThat(BattleAcknowledgements.firstPendingIndexOf(report, battle, Set.of(first, second))).isNegative();
    }
}
