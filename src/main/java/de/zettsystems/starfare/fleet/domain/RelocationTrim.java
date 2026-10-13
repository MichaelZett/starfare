package de.zettsystems.starfare.fleet.domain;

import de.zettsystems.starfare.game.values.StandingOrder;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shortens a source's outgoing relocations proportionally when they exceed its limit.
 * Pure rule shared by preview and commit: floor(old × limit / total), remaining whole
 * ships by largest remainder, ties by the smaller order id. Never raises a route.
 */
public final class RelocationTrim {
    private RelocationTrim() { }

    /** New sizes keyed by order id for every changed route; empty when the routes still fit. */
    public static Map<Integer, Integer> trim(List<StandingOrder> outgoing, int limit) {
        long total = outgoing.stream().mapToLong(StandingOrder::ships).sum();
        int bound = Math.max(0, limit);
        if (total <= bound) { return Map.of(); }
        Map<Integer, Integer> sizes = new HashMap<>();
        int assigned = 0;
        for (StandingOrder order : outgoing) {
            int size = (int) (order.ships() * (long) bound / total);
            sizes.put(order.id(), size);
            assigned += size;
        }
        var ranked = outgoing.stream().sorted(Comparator
                .comparingLong((StandingOrder order) -> order.ships() * (long) bound % total).reversed()
                .thenComparingInt(StandingOrder::id)).toList();
        for (int index = 0; index < bound - assigned; index++) { sizes.merge(ranked.get(index).id(), 1, Integer::sum); }
        Map<Integer, Integer> changed = new HashMap<>();
        outgoing.forEach(order -> {
            int size = java.util.Objects.requireNonNull(sizes.get(order.id()));
            if (size != order.ships()) { changed.put(order.id(), size); }
        });
        return Map.copyOf(changed);
    }
}
