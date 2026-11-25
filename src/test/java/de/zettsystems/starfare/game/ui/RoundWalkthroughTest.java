package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.report.values.CosmicNews;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RoundWalkthroughTest {

    private static final TurnReport REPORT = new TurnReport(6, List.of(), List.of(
            new TurnEvent.Production(1, 10, "Vega", 4),
            new TurnEvent.BattleWon(1, 11, "Sirius", 12, 8, 4, false),
            new TurnEvent.Reinforcement(1, 10, "Vega", 3, 9, "F1"),
            new TurnEvent.Cosmic(CosmicNews.SUPERNOVA, 12, "Deneb", 0, 0, 0, 0, ""),
            TurnEvent.Cosmic.news(CosmicNews.STORM),
            new TurnEvent.SystemLost(1, 2, 13, "Altair"),
            new TurnEvent.Victory(1)));

    @Test
    void battlesLossesAndCosmicNewsWithASystemAreWalkedThroughButRoutineAndPlacelessEventsAreNot() {
        assertThat(REPORT.events()).extracting(event -> RoundWalkthrough.siteOf(event).isPresent())
                .containsExactly(false, true, false, true, false, true, false);
        assertThat(RoundWalkthrough.siteOf(REPORT.events().get(3)).getAsInt()).isEqualTo(12);
        var cancelled = new TurnEvent.JointDeparture(1, 14, "Rigel", 13, "Altair", 8, 0,
                TurnEvent.JointDeparture.Outcome.SOURCE_LOST);
        assertThat(RoundWalkthrough.siteOf(cancelled)).as("a changed joint start is shown at its source").hasValue(14);
    }

    @Test
    void theFirstUnconfirmedEventInReportOrderIsCurrentWithItsProgress() {
        var first = RoundWalkthrough.current(REPORT, Set.of()).orElseThrow();
        assertThat(first.turn()).isEqualTo(6);
        assertThat(first.eventIndex()).isEqualTo(1);
        assertThat(first.systemId()).isEqualTo(11);
        assertThat(first.battle()).isTrue();
        assertThat(first.position()).isEqualTo(1);
        assertThat(first.total()).isEqualTo(3);

        var second = RoundWalkthrough.current(REPORT, Set.of(1)).orElseThrow();
        assertThat(second.eventIndex()).isEqualTo(3);
        assertThat(second.systemId()).isEqualTo(12);
        assertThat(second.battle()).isFalse();
        assertThat(second.position()).isEqualTo(2);
    }

    @Test
    void anEventConfirmedOutOfOrderCountsAsDoneAndIsSkipped() {
        var step = RoundWalkthrough.current(REPORT, Set.of(3)).orElseThrow();
        assertThat(step.eventIndex()).isEqualTo(1);
        assertThat(step.position()).isEqualTo(2);
        assertThat(RoundWalkthrough.current(REPORT, Set.of(1, 3)).orElseThrow().eventIndex()).isEqualTo(5);
    }

    @Test
    void theWalkthroughEndsOnceEveryLocatedEventIsConfirmed() {
        assertThat(RoundWalkthrough.current(REPORT, Set.of(1, 3, 5))).isEmpty();
        assertThat(RoundWalkthrough.current(new TurnReport(2, List.of(), List.of(
                new TurnEvent.Production(1, 10, "Vega", 4))), Set.of())).isEmpty();
    }

    @Test
    void onlyAnUnconfirmedLocatedEventIsOpen() {
        assertThat(RoundWalkthrough.isOpen(REPORT, 3, Set.of())).isTrue();
        assertThat(RoundWalkthrough.isOpen(REPORT, 3, Set.of(3))).isFalse();
        assertThat(RoundWalkthrough.isOpen(REPORT, 0, Set.of())).as("production").isFalse();
        assertThat(RoundWalkthrough.isOpen(REPORT, 4, Set.of())).as("news without a system").isFalse();
        assertThat(RoundWalkthrough.isOpen(REPORT, -1, Set.of())).isFalse();
        assertThat(RoundWalkthrough.isOpen(REPORT, REPORT.events().size(), Set.of())).isFalse();
    }
}
