package de.zettsystems.starfare.game.values;

import java.util.List;

/** Scheduled transfers per round; incoming ships still need their normal travel time. */
public record ProductionFlow(int incoming, int outgoing) {
    public int retainedShips(int production) {
        return Math.toIntExact(Math.max(0L, (long) production + incoming - outgoing));
    }

    public int sendExceptProduction(int maximum, int production) {
        return Math.max(0, maximum - retainedShips(production));
    }

    public static ProductionFlow at(int systemId, List<StandingOrderView> orders) {
        int incoming = orders.stream().filter(order -> order.toSystemId() == systemId)
                .mapToInt(StandingOrderView::ships).sum();
        int outgoing = orders.stream().filter(order -> order.fromSystemId() == systemId)
                .mapToInt(StandingOrderView::ships).sum();
        return new ProductionFlow(incoming, outgoing);
    }
}
