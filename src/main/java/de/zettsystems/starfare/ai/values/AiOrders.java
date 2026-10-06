package de.zettsystems.starfare.ai.values;

import de.zettsystems.starfare.fleet.values.FleetOrder;
import java.util.List;
import java.util.Map;

public record AiOrders(List<FleetOrder.Send> sends, Map<Integer, Integer> expansion) {
    public AiOrders {
        sends = List.copyOf(sends);
        expansion = Map.copyOf(expansion);
    }
}
