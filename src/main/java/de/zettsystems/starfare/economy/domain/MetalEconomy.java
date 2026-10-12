package de.zettsystems.starfare.economy.domain;

import de.zettsystems.starfare.economy.values.RecyclingRules;

import java.util.HashMap;
import java.util.Map;

/** Immutable resource state, including the next round's local mining assignments. */
public record MetalEconomy(Map<Integer, MetalDeposit> deposits, Map<Integer, Long> stocks,
                           Map<Integer, Integer> mining, @org.jspecify.annotations.Nullable RecyclingEconomy recycling) {
    public MetalEconomy(Map<Integer, MetalDeposit> deposits, Map<Integer, Long> stocks, Map<Integer, Integer> mining) {
        this(deposits, stocks, mining, null);
    }
    public MetalEconomy {
        deposits = Map.copyOf(deposits);
        stocks = Map.copyOf(stocks);
        mining = Map.copyOf(mining);
        if (!deposits.keySet().equals(mining.keySet()) || stocks.values().stream().anyMatch(value -> value < 0)
                || mining.values().stream().anyMatch(value -> value < 0)) {
            throw new IllegalArgumentException("Invalid metal economy");
        }
    }
    public MetalEconomy allocateMining(int system, int points) {
        if (!deposits.containsKey(system) || points < 0) { throw new IllegalArgumentException("Invalid mining assignment"); }
        var next = new HashMap<>(mining);
        next.put(system, points);
        return new MetalEconomy(deposits, stocks, next, recycling);
    }
    public MetalEconomy establishRecycling(long ships) {
        if (recycling != null) { throw new IllegalArgumentException("Recycling is fixed at creation"); }
        long total = deposits.values().stream().mapToLong(MetalDeposit::remaining).sum()
                + stocks.values().stream().mapToLong(Long::longValue).sum() + ships;
        var orders = new HashMap<Integer, Integer>(); deposits.keySet().forEach(id -> orders.put(id, 0));
        var fractions = new HashMap<Integer, Integer>(); stocks.keySet().forEach(id -> fractions.put(id, 0));
        return new MetalEconomy(deposits, stocks, mining, new RecyclingEconomy(orders, fractions,
                Math.multiplyExact(total, RecyclingRules.METAL_SCALE), 0, 0));
    }
    public MetalEconomy planRecycling(int system, int ships) {
        return new MetalEconomy(deposits, stocks, mining, java.util.Objects.requireNonNull(recycling).plan(system, ships));
    }
    public MetalEconomy recordCombatLoss(long ships) {
        return new MetalEconomy(deposits, stocks, mining, java.util.Objects.requireNonNull(recycling).recordCombatLoss(ships));
    }
}
