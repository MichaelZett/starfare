package de.zettsystems.starfare.game.values;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LogisticsSystemSummaryTest {

    @Test
    void separatesPlannedBalanceFromNextDeliveryForCompetingRoutes() {
        VisibleSystem source = system(1, 8, 7, 6, 2);
        VisibleSystem firstTarget = system(2, 0, 4, 0, 0);
        VisibleSystem secondTarget = system(3, 0, 4, 0, 0);
        List<StandingOrderView> orders = List.of(
                new StandingOrderView(1, 1, 2, "A", "B", 7, 8),
                new StandingOrderView(2, 1, 3, "A", "C", 7, 3));

        var summaries = LogisticsSystemSummary.forSystems(List.of(source, firstTarget, secondTarget), orders);

        assertThat(summaries.get(1))
                .returns(-11, LogisticsSystemSummary::netFlow)
                .returns(-4, LogisticsSystemSummary::plannedAccumulation)
                .returns(0, LogisticsSystemSummary::freePlanningCapacity)
                .returns(true, LogisticsSystemSummary::deliveryBottleneck);
        assertThat(summaries.get(2).nextDelivery()).isEqualTo(8);
        assertThat(summaries.get(3).nextDelivery()).isEqualTo(1);
    }

    private static VisibleSystem system(int id, int garrison, int production, int reserve, int available) {
        return new VisibleSystem(id, "System " + id, 0, 0, 1, garrison, production, true, "#ffffff", 1,
                true, 0, reserve, available, List.of());
    }
}
