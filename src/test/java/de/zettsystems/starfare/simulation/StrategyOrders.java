package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.fleet.values.FleetOrder;
import java.util.List;
import java.util.Map;

record StrategyOrders(List<FleetOrder.Send> sends, Map<Integer, Integer> expansion) {
    StrategyOrders {
        sends = List.copyOf(sends);
        expansion = Map.copyOf(expansion);
    }
}
