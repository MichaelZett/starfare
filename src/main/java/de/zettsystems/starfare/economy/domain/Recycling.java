package de.zettsystems.starfare.economy.domain;

import de.zettsystems.starfare.economy.values.RecyclingRules;

import de.zettsystems.starfare.game.values.StarSystem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Pure settlement shared by the production forecast and the turn engine. */
public final class Recycling {
    private Recycling() { }
    public record Settlement(MetalEconomy economy, Map<Integer, Integer> ships) {
        public Settlement { ships = Map.copyOf(ships); }
    }
    public static Settlement calculate(List<StarSystem> systems, MetalEconomy metal) {
        var current = metal.recycling();
        if (current == null) { return new Settlement(metal, Map.of()); }
        var stocks = new HashMap<>(metal.stocks());
        var fractions = new HashMap<>(current.fractions());
        var orders = new HashMap<>(current.orders());
        Map<Integer, Integer> ships = new HashMap<>();
        long loss = current.recyclingLossTenths();
        for (StarSystem system : systems) {
            int planned = orders.getOrDefault(system.id(), 0);
            Integer owner = system.ownerId();
            if (planned == 0 || owner == null || system.neutral()) { continue; }
            if (planned > system.availableShips()) { throw new IllegalArgumentException("Recycling exceeds available garrison"); }
            long recovered = (long) planned * RecyclingRules.RETURN_TENTHS + Objects.requireNonNull(fractions.get(owner));
            stocks.merge(owner, recovered / RecyclingRules.METAL_SCALE, Math::addExact);
            fractions.put(owner, (int) (recovered % RecyclingRules.METAL_SCALE));
            orders.put(system.id(), 0); ships.put(system.id(), planned);
            loss = Math.addExact(loss, (long) planned * RecyclingRules.LOSS_TENTHS);
        }
        var next = new RecyclingEconomy(orders, fractions, current.initialTenths(), current.combatLossTenths(), loss);
        return new Settlement(new MetalEconomy(metal.deposits(), stocks, metal.mining(), next, metal.research()), ships);
    }
}
