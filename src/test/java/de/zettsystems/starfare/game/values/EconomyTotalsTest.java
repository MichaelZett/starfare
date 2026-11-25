package de.zettsystems.starfare.game.values;

import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EconomyTotalsTest {
    private static TurnReport report(int round, TurnEvent... events) { return new TurnReport(round, List.of(), List.of(events)); }

    @Test void sumsTheRecordedIndustryOfOneEmpire() {
        List<TurnReport> reports = List.of(
                report(1, new TurnEvent.Production(1, 1, "Alpha", 4), new TurnEvent.EconomySummary(1, 1),
                        new TurnEvent.EconomySummary(2, 5)),
                report(2, new TurnEvent.EconomySummary(1, 0)),
                report(3, new TurnEvent.EconomySummary(1, 2)));
        assertThat(EconomyTotals.of(reports, 1)).isEqualTo(new EconomyTotals(3));
        assertThat(EconomyTotals.of(reports, 2)).isEqualTo(new EconomyTotals(5));
    }

    @Test void withoutSummariesNothingWasBuilt() {
        assertThat(EconomyTotals.of(List.of(report(1, new TurnEvent.Production(1, 1, "Alpha", 4))), 1).industryBuilt()).isZero();
        assertThat(EconomyTotals.of(List.of(), 1).industryBuilt()).isZero();
    }
}
