package de.zettsystems.starfare.game.values;

import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EconomyTotalsTest {
    private static TurnReport report(int round, TurnEvent... events) { return new TurnReport(round, List.of(), List.of(events)); }
    private static TreeSet<Integer> rounds(Integer... values) { return new TreeSet<>(List.of(values)); }

    @Test void completeHistorySumsRecordedProductionOnly() {
        Map<Integer, TurnReport> reports = Map.of(
                1, report(1, new TurnEvent.Production(1, 1, "Alpha", 4), new TurnEvent.EconomySummary(1, 6, 4, 1),
                        new TurnEvent.EconomySummary(2, 50, 50, 5), new TurnEvent.Recycling(1, 1, "Alpha", 9, 63)),
                2, report(2, new TurnEvent.EconomySummary(1, 3, 2, 0)));
        var totals = EconomyTotals.of(reports, rounds(1, 2), 1, true);
        assertThat(totals).isEqualTo(new EconomyTotals(9, 6, 1, EconomyTotals.Coverage.COMPLETE, 1, true));
    }

    @Test void zeroIsRecordedButMissingHistoryIsNot() {
        var zero = EconomyTotals.of(Map.of(1, report(1, new TurnEvent.EconomySummary(1, 0, 0, 0))), rounds(1), 1, true);
        assertThat(zero.coverage()).isEqualTo(EconomyTotals.Coverage.COMPLETE);
        assertThat(zero.recorded()).isTrue();
        var legacy = EconomyTotals.of(Map.of(1, report(1, new TurnEvent.Production(1, 1, "Alpha", 4))), rounds(1, 2), 1, false);
        assertThat(legacy.coverage()).isEqualTo(EconomyTotals.Coverage.NOT_RECORDED);
        assertThat(legacy.recorded()).isFalse();
        assertThat(EconomyTotals.of(Map.of(), rounds(), 1, true).recorded()).isFalse();
    }

    @Test void laterStartAndGapsAreLabelledHonestly() {
        Map<Integer, TurnReport> later = new HashMap<>();
        later.put(1, report(1));
        later.put(2, report(2, new TurnEvent.EconomySummary(1, 2, 1, 0)));
        later.put(3, report(3, new TurnEvent.EconomySummary(1, 2, 1, 1)));
        var since = EconomyTotals.of(later, rounds(1, 2, 3), 1, true);
        assertThat(since.coverage()).isEqualTo(EconomyTotals.Coverage.SINCE);
        assertThat(since.firstRound()).isEqualTo(2);
        assertThat(since.metalMined()).isEqualTo(4);
        later.remove(2);
        var gap = EconomyTotals.of(Map.of(1, report(1, new TurnEvent.EconomySummary(1, 1, 1, 0)),
                3, report(3, new TurnEvent.EconomySummary(1, 1, 1, 0))), rounds(1, 2, 3), 1, true);
        assertThat(gap.coverage()).isEqualTo(EconomyTotals.Coverage.PARTIAL);
        assertThat(gap.metalMined()).isEqualTo(2);
    }
}
