package de.zettsystems.starfare.economy.domain;

import de.zettsystems.starfare.economy.values.RecyclingRules;

import java.util.HashMap;
import java.util.Map;

/** Fixed-point metal accounting: one ship binds ten tenths and returns seven. */
public record RecyclingEconomy(Map<Integer, Integer> orders, Map<Integer, Integer> fractions,
                               long initialTenths, long combatLossTenths, long recyclingLossTenths) {
    public RecyclingEconomy {
        orders = Map.copyOf(orders);
        fractions = Map.copyOf(fractions);
        if (orders.values().stream().anyMatch(value -> value < 0)
                || fractions.values().stream().anyMatch(value -> value < 0 || value >= RecyclingRules.METAL_SCALE)
                || initialTenths < 0 || combatLossTenths < 0 || recyclingLossTenths < 0) {
            throw new IllegalArgumentException("Invalid recycling economy");
        }
    }
    public RecyclingEconomy plan(int system, int ships) {
        if (!orders.containsKey(system) || ships < 0) { throw new IllegalArgumentException("Invalid recycling order"); }
        var next = new HashMap<>(orders); next.put(system, ships);
        return new RecyclingEconomy(next, fractions, initialTenths, combatLossTenths, recyclingLossTenths);
    }
    public RecyclingEconomy recordCombatLoss(long ships) {
        if (ships < 0) { throw new IllegalArgumentException("Combat cannot create ships"); }
        return new RecyclingEconomy(orders, fractions, initialTenths,
                Math.addExact(combatLossTenths, Math.multiplyExact(ships, RecyclingRules.METAL_SCALE)), recyclingLossTenths);
    }
}
