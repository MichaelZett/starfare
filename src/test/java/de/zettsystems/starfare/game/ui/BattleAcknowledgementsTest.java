package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BattleAcknowledgementsTest {

    @Test
    void acknowledgementOnlyClearsTheChosenBattleInTheCurrentRound() {
        GameId gameId = GameId.of("game-1");
        TurnReport report = new TurnReport(4, List.of(), List.of(
                new TurnEvent.BattleWon(1, 10, "Vega", 12, 8, 4, false),
                new TurnEvent.BattleLost(1, 11, "Sirius", 9, 11, 3)));

        Set<String> acknowledgements = Set.of();
        assertThat(BattleAcknowledgements.pending(gameId, report, acknowledgements)).containsExactlyInAnyOrder(10, 11);

        acknowledgements = BattleAcknowledgements.acknowledge(gameId, report, 10, acknowledgements);

        assertThat(BattleAcknowledgements.pending(gameId, report, acknowledgements)).containsExactly(11);
    }

    @Test
    void nonBattleReportEventsDoNotCreateAnAcknowledgement() {
        TurnReport report = new TurnReport(4, List.of(), List.of(
                new TurnEvent.Production(1, 10, "Vega", 2)));

        assertThat(BattleAcknowledgements.pending(GameId.of("game-1"), report, Set.of())).isEmpty();
    }
}
