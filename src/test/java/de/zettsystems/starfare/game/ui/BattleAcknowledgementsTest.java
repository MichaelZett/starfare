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

        assertThat(BattleAcknowledgements.isPending(report, 0, Set.of())).isTrue();
        assertThat(BattleAcknowledgements.pending(report, Set.of(0))).containsExactly(11);
    }

    @Test
    void nonBattleReportEventsDoNotCreateAnAcknowledgement() {
        TurnReport report = new TurnReport(4, List.of(), List.of(
                new TurnEvent.Production(1, 10, "Vega", 2)));

        assertThat(BattleAcknowledgements.pending(report, Set.of())).isEmpty();
        assertThat(BattleAcknowledgements.isPending(report, 0, Set.of())).isFalse();
    }

    @Test
    void acknowledgementDoesNotClearAnotherBattleAtTheSameSystem() {
        TurnReport report = new TurnReport(4, List.of(), List.of(
                new TurnEvent.BattleWon(1, 10, "Vega", 12, 8, 4, false),
                new TurnEvent.BattleLost(2, 10, "Vega", 9, 7, 2)));

        assertThat(BattleAcknowledgements.pending(report, Set.of(0))).containsExactly(10);
        assertThat(BattleAcknowledgements.isPending(report, 0, Set.of(0))).isFalse();
        assertThat(BattleAcknowledgements.isPending(report, 1, Set.of(0))).isTrue();
    }

    @Test
    void identicalBattlesInOneRoundCanBothBeAcknowledged() {
        TurnEvent battle = new TurnEvent.BattleLost(2, 10, "Vega", 9, 7, 2);
        TurnReport report = new TurnReport(4, List.of(), List.of(battle, battle));

        assertThat(BattleAcknowledgements.isPending(report, 0, Set.of(1))).isTrue();
        assertThat(BattleAcknowledgements.isPending(report, 1, Set.of(1))).isFalse();
        assertThat(BattleAcknowledgements.pending(report, Set.of(1))).containsExactly(10);
        assertThat(BattleAcknowledgements.pending(report, Set.of(0, 1))).isEmpty();
    }
}
