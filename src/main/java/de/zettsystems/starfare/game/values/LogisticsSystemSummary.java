package de.zettsystems.starfare.game.values;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable per-system logistics view. Planned rates stay separate from what a
 * source can actually send in the next production step.
 */
public record LogisticsSystemSummary(
        int systemId,
        int production,
        int plannedIncoming,
        int plannedOutgoing,
        int currentShips,
        int reserve,
        int availableShips,
        int nextDelivery,
        boolean deliveryBottleneck
) {
    public int netFlow() {
        return plannedIncoming - plannedOutgoing;
    }

    public int plannedAccumulation() {
        return production + netFlow();
    }

    public int freePlanningCapacity() {
        return Math.max(0, production + plannedIncoming - plannedOutgoing);
    }

    public static Map<Integer, LogisticsSystemSummary> forSystems(List<VisibleSystem> systems,
                                                                    List<StandingOrderView> orders) {
        Map<Integer, Integer> remainingBySource = new HashMap<>();
        for (VisibleSystem system : systems) {
            Integer available = system.availableShips();
            if (system.fullyVisible() && available != null) {
                remainingBySource.put(system.id(), available + valueOrZero(system.productionPerTurn()));
            }
        }
        Map<Integer, Integer> deliveredByTarget = new HashMap<>();
        Map<Integer, Boolean> bottleneckBySource = new HashMap<>();
        for (StandingOrderView order : orders) {
            int remaining = remainingBySource.getOrDefault(order.fromSystemId(), 0);
            int delivered = Math.clamp(remaining, 0, order.ships());
            remainingBySource.put(order.fromSystemId(), remaining - delivered);
            deliveredByTarget.merge(order.toSystemId(), delivered, Integer::sum);
            if (delivered < order.ships()) {
                bottleneckBySource.put(order.fromSystemId(), true);
            }
        }
        Map<Integer, LogisticsSystemSummary> summaries = new HashMap<>();
        for (VisibleSystem system : systems) {
            if (!system.fullyVisible() || system.routedProduction() == null) {
                continue;
            }
            ProductionFlow flow = ProductionFlow.at(system.id(), orders);
            summaries.put(system.id(), new LogisticsSystemSummary(system.id(), valueOrZero(system.productionPerTurn()),
                    flow.incoming(), flow.outgoing(), valueOrZero(system.garrison()),
                    valueOrZero(system.garrisonReserve()), valueOrZero(system.availableShips()),
                    deliveredByTarget.getOrDefault(system.id(), 0),
                    bottleneckBySource.getOrDefault(system.id(), false)));
        }
        return Map.copyOf(summaries);
    }

    private static int valueOrZero(@Nullable Integer value) {
        return value == null ? 0 : value;
    }
}
