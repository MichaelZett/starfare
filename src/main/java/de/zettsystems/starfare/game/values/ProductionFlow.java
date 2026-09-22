package de.zettsystems.starfare.game.values;

import java.util.List;

/** Scheduled transfers per round; incoming ships still need their normal travel time. */
public record ProductionFlow(int incoming, int outgoing) {
    public static ProductionFlow at(int systemId, List<StandingOrderView> orders) {
        int incoming = orders.stream().filter(order -> order.toSystemId() == systemId)
                .mapToInt(StandingOrderView::ships).sum();
        int outgoing = orders.stream().filter(order -> order.fromSystemId() == systemId)
                .mapToInt(StandingOrderView::ships).sum();
        return new ProductionFlow(incoming, outgoing);
    }
}
