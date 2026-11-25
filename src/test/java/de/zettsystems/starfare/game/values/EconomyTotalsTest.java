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

    @Test void completeHistorySumsTheRecordedIndustryOfOneEmpire() {
        Map<Integer, TurnReport> reports = Map.of(
                1, report(1, new TurnEvent.Production(1, 1, "Alpha", 4), new TurnEvent.EconomySummary(1, 1),
                        new TurnEvent.EconomySummary(2, 5)),
                2, report(2, new TurnEvent.EconomySummary(1, 0)));
        assertThat(EconomyTotals.of(reports, rounds(1, 2), 1))
                .isEqualTo(new EconomyTotals(1, EconomyTotals.Coverage.COMPLETE, 1));
    }

    @Test void zeroIsRecordedButMissingHistoryIsNot() {
        var zero = EconomyTotals.of(Map.of(1, report(1, new TurnEvent.EconomySummary(1, 0))), rounds(1), 1);
        assertThat(zero.coverage()).isEqualTo(EconomyTotals.Coverage.COMPLETE);
        assertThat(zero.recorded()).isTrue();
        var legacy = EconomyTotals.of(Map.of(1, report(1, new TurnEvent.Production(1, 1, "Alpha", 4))), rounds(1, 2), 1);
        assertThat(legacy.coverage()).isEqualTo(EconomyTotals.Coverage.NOT_RECORDED);
        assertThat(legacy.recorded()).isFalse();
        assertThat(EconomyTotals.of(Map.of(), rounds(), 1).recorded()).isFalse();
    }

    @Test void laterStartAndGapsAreLabelledHonestly() {
        Map<Integer, TurnReport> later = new HashMap<>();
        later.put(1, report(1));
        later.put(2, report(2, new TurnEvent.EconomySummary(1, 0)));
        later.put(3, report(3, new TurnEvent.EconomySummary(1, 1)));
        var since = EconomyTotals.of(later, rounds(1, 2, 3), 1);
        assertThat(since.coverage()).isEqualTo(EconomyTotals.Coverage.SINCE);
        assertThat(since.firstRound()).isEqualTo(2);
        assertThat(since.industryBuilt()).isOne();
        var gap = EconomyTotals.of(Map.of(1, report(1, new TurnEvent.EconomySummary(1, 1)),
                3, report(3, new TurnEvent.EconomySummary(1, 1))), rounds(1, 2, 3), 1);
        assertThat(gap.coverage()).isEqualTo(EconomyTotals.Coverage.PARTIAL);
        assertThat(gap.industryBuilt()).isEqualTo(2);
    }
}
