package de.zettsystems.starfare.economy.domain;

import de.zettsystems.starfare.economy.values.RecyclingRules;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import java.util.List;
import java.util.Objects;

/** Shared reservation checks for recycling, dispatch and reserve changes. */
public final class RecyclingOrders {
    private RecyclingOrders() { }
    public static boolean plan(GameState state, int player, int systemId, int ships) {
        if (!state.ruleset().usesRecycling() || ships < 0) { return false; }
        var system = state.systems().stream().filter(value -> value.id() == systemId).findFirst().orElse(null);
        if (system == null || system.neutral() || !Objects.equals(system.ownerId(), player)
                || !state.industries().containsKey(systemId)) { return false; }
        long committed = state.pendingOrders().getOrDefault(player, List.of()).stream()
                .filter(order -> order instanceof FleetOrder.Send send && send.fromSystemId() == systemId)
                .mapToLong(order -> ((FleetOrder.Send) order).ships()).sum();
        if (ships + committed > system.availableShips()) { return false; }
        state.planRecycling(systemId, ships); return true;
    }
    public static void validateReference(RulesetRef rules, MetalEconomy metal) {
        var recycle = metal.recycling();
        if (rules.usesRecycling() != (recycle != null)) { throw new IllegalArgumentException("Recycling rules and state must match"); }
        if (recycle != null && (!recycle.orders().keySet().equals(metal.deposits().keySet())
                || !recycle.fractions().keySet().equals(metal.stocks().keySet()))) {
            throw new IllegalArgumentException("Incomplete stored recycling economy");
        }
    }
    public static void validate(GameState state, MetalEconomy metal) {
        validateReference(state.ruleset(), metal);
        var recycling = metal.recycling();
        if (recycling == null) { return; }
        for (var system : state.systems()) {
            int count = recycling.orders().getOrDefault(system.id(), 0);
            long committed = system.ownerId() == null ? 0 : state.pendingOrders().getOrDefault(system.ownerId(), List.of()).stream()
                    .filter(order -> order instanceof FleetOrder.Send send && send.fromSystemId() == system.id())
                    .mapToLong(order -> ((FleetOrder.Send) order).ships()).sum();
            if (count > 0 && (system.neutral() || system.ownerId() == null || count + committed > system.availableShips())) {
                throw new IllegalArgumentException("Invalid stored recycling reservation");
            }
        }
        if (balanceTenths(state) != recycling.initialTenths()) { throw new IllegalArgumentException("Unbalanced stored metal economy"); }
    }
    public static long balanceTenths(GameState state) {
        var metal = state.metals().orElseThrow();
        var recycling = Objects.requireNonNull(metal.recycling());
        long raw = metal.deposits().values().stream().mapToLong(MetalDeposit::remaining).sum()
                + metal.stocks().values().stream().mapToLong(Long::longValue).sum() + state.totalShips();
        return raw * RecyclingRules.METAL_SCALE + recycling.fractions().values().stream().mapToLong(Integer::longValue).sum()
                + recycling.combatLossTenths() + recycling.recyclingLossTenths();
    }
}
