package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.report.values.CosmicNews;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
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
    void battlesLossesCosmicNewsWithASystemAndHyperspaceNewsAreWalkedThroughButRoutineEventsAreNot() {
        assertThat(REPORT.events()).extracting(event -> RoundWalkthrough.siteOf(event).isPresent())
                .containsExactly(false, true, false, true, false, true, false);
        assertThat(REPORT.events()).extracting(RoundWalkthrough::walked)
                .as("the storm has no system but is shown at the own fleets")
                .containsExactly(false, true, false, true, true, true, false);
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
        assertThat(first.total()).isEqualTo(4);

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
        var storm = RoundWalkthrough.current(REPORT, Set.of(1, 3)).orElseThrow();
        assertThat(storm.eventIndex()).isEqualTo(4);
        assertThat(storm.hasSystem()).isFalse();
        assertThat(RoundWalkthrough.current(REPORT, Set.of(1, 3, 4)).orElseThrow().eventIndex()).isEqualTo(5);
    }

    @Test
    void theWalkthroughEndsOnceEveryLocatedEventIsConfirmed() {
        assertThat(RoundWalkthrough.current(REPORT, Set.of(1, 3, 4, 5))).isEmpty();
        assertThat(RoundWalkthrough.current(new TurnReport(2, List.of(), List.of(
                new TurnEvent.Production(1, 10, "Vega", 4))), Set.of())).isEmpty();
    }

    @Test
    void onlyAnUnconfirmedLocatedEventIsOpen() {
        assertThat(RoundWalkthrough.isOpen(REPORT, 3, Set.of())).isTrue();
        assertThat(RoundWalkthrough.isOpen(REPORT, 3, Set.of(3))).isFalse();
        assertThat(RoundWalkthrough.isOpen(REPORT, 0, Set.of())).as("production").isFalse();
        assertThat(RoundWalkthrough.isOpen(REPORT, 4, Set.of())).as("hyperspace news").isTrue();
        assertThat(RoundWalkthrough.isOpen(REPORT, 6, Set.of())).as("victory").isFalse();
        assertThat(RoundWalkthrough.isOpen(REPORT, -1, Set.of())).isFalse();
        assertThat(RoundWalkthrough.isOpen(REPORT, REPORT.events().size(), Set.of())).isFalse();
    }

    private static RoundWalkthrough.Step step(TurnEvent event, int systemId) {
        return new RoundWalkthrough.Step(6, 0, event, systemId, 1, 1);
    }

    private static Fleet flying(int globalId, int localNo, int arrival) {
        return new Fleet(globalId, 1, localNo, 10, 11, 5, 4, arrival);
    }

    @Test
    void aStormMarksEveryOwnFleetInFlightAndCentresOnTheFirstToArrive() {
        var marks = RoundWalkthrough.marks(step(TurnEvent.Cosmic.news(CosmicNews.STORM), RoundWalkthrough.NO_SYSTEM),
                List.of(flying(7, 1, 12), flying(8, 2, 9)));

        assertThat(marks.fleets()).containsOnly(Map.entry(7, "+1"), Map.entry(8, "+1"));
        assertThat(marks.focusFleet()).isEqualTo(8);
        assertThat(marks.systems()).isEmpty();
    }

    @Test
    void aSpacetimeQuakeShowsTheNewArrivalAtEachFleet() {
        var marks = RoundWalkthrough.marks(step(TurnEvent.Cosmic.news(CosmicNews.SPACETIME_QUAKE),
                RoundWalkthrough.NO_SYSTEM), List.of(flying(7, 1, 12)));

        assertThat(marks.fleets()).containsOnly(Map.entry(7, "R12"));
    }

    @Test
    void aVanishedPartOfAFleetShowsAtTheFleetAndAWholeLossAtItsTarget() {
        var partly = new TurnEvent.Cosmic(CosmicNews.DISAPPEARANCE, 11, "Sirius", 2, 3, 4, 0, "");
        var marks = RoundWalkthrough.marks(step(partly, 11), List.of(flying(7, 1, 12), flying(8, 2, 9)));
        assertThat(marks.fleets()).containsOnly(Map.entry(8, "−3"));
        assertThat(marks.focusFleet()).isEqualTo(8);

        var wholly = new TurnEvent.Cosmic(CosmicNews.DISAPPEARANCE, 11, "Sirius", 3, 1, 0, 0, "");
        var gone = RoundWalkthrough.marks(step(wholly, 11), List.of(flying(7, 1, 12)));
        assertThat(gone.fleets()).isEmpty();
        assertThat(gone.focusFleet()).isNull();
        assertThat(gone.systems()).containsOnly(Map.entry(11, "−1"));
    }

    @Test
    void aSpaceportQuakeShowsItsLossAtTheSystemAndBattlesMarkNothing() {
        var quake = new TurnEvent.Cosmic(CosmicNews.SPACEPORT_QUAKE_HEAVY, 10, "Vega", 0, 6, 2, 0, "");
        assertThat(RoundWalkthrough.marks(step(quake, 10), List.of()).systems()).containsOnly(Map.entry(10, "−6"));
        assertThat(RoundWalkthrough.marks(step(REPORT.events().get(1), 11), List.of()))
                .isEqualTo(RoundWalkthrough.MapMarks.NONE);
        assertThat(RoundWalkthrough.marks(null, List.of())).isEqualTo(RoundWalkthrough.MapMarks.NONE);
    }

    @Test
    void refugeesAreHighlightedWithoutALabel() {
        var refugees = new TurnEvent.Cosmic(CosmicNews.REFUGEES, 12, "Deneb", 1, 6, 0, 9, "Vega");
        var marks = RoundWalkthrough.marks(step(refugees, 12), List.of(flying(7, 1, 9)));

        assertThat(marks.fleets()).containsOnly(Map.entry(7, ""));
        assertThat(marks.focusFleet()).isNull();
    }
}
