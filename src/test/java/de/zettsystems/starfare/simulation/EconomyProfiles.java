package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.ai.application.DefaultSpacewardPlanning;
import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.economy.application.DefaultEconomyService;
import de.zettsystems.starfare.economy.domain.RecyclingOrders;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.application.DefaultPlayerViewBuilder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Player;
import java.util.List;
import java.util.Objects;

/** Experimental economics shares CONCENTRATION fleet decisions; no extra selectable game profiles. */
final class EconomyProfiles {
    enum Policy { STANDARD, MINING, RECYCLING, ADAPTIVE }
    record Profile(AiStrategy strategy, Policy policy) {
        @Override public String toString() { return policy == Policy.STANDARD ? strategy.name() : policy.name(); }
    }
    private final DefaultEconomyService economy = new CheckedEconomy();
    private final DefaultFleetService fleets = new CheckedFleets();
    private final DefaultPlayerViewBuilder views = new DefaultPlayerViewBuilder();
    private final DefaultSpacewardPlanning standard = new DefaultSpacewardPlanning(economy, views);

    void plan(GameState state, List<Profile> profiles) {
        for (int index = 0; index < profiles.size(); index++) {
            var player = state.players().get(index);
            state.players().set(index, new Player(player.id(), player.name(), true, player.colorHex(), player.empireName(), profiles.get(index).strategy()));
        }
        standard.plan(state, fleets);
        for (int index = 0; index < profiles.size(); index++) {
            if (profiles.get(index).policy() != Policy.STANDARD) { adjust(state, index + 1, profiles.get(index).policy()); }
        }
    }

    private void adjust(GameState state, int player, Policy policy) {
        var view = views.forPlayer(state, player);
        // Decide from a frozen view before applying allocations. Foreign resources never enter the policy.
        for (var system : view.systems()) {
            if (!Objects.equals(system.ownerId(), player) || !system.fullyVisible()) { continue; }
            var industry = Objects.requireNonNull(system.industry());
            var metal = Objects.requireNonNull(industry.metal());
            int capacity = industry.usableCapacity();
            int expansion = industry.expansionAllocation();
            int mining = mining(policy, capacity - expansion, metal.yieldPerPoint(), metal.stock(), metal.remaining());
            require(economy.allocateProduction(state, player, system.id(), expansion, mining), "Invalid economic allocation");
            require(RecyclingOrders.plan(state, player, system.id(), 0), "Invalid cancellation");
        }
        if (policy == Policy.MINING) { return; }
        var updated = views.forPlayer(state, player);
        for (var system : updated.systems()) {
            if (!Objects.equals(system.ownerId(), player) || !system.fullyVisible()) { continue; }
            var metal = Objects.requireNonNull(Objects.requireNonNull(system.industry()).metal());
            int shortage = metal.plannedShips() - metal.actualShips();
            boolean threatened = updated.systems().stream().anyMatch(contact -> contact.ownerId() != null
                    && !Objects.equals(contact.ownerId(), player) && !state.allied(player, contact.ownerId())
                    && state.travelRounds(system.id(), contact.id()) <= 2);
            int retained = policy == Policy.RECYCLING ? 4 : 8;
            int surplus = Math.max(0, Objects.requireNonNull(system.availableShips()) - retained);
            int count = threatened || shortage <= 0 ? 0 : Math.min(surplus, Math.ceilDiv(shortage * 10, 7));
            require(RecyclingOrders.plan(state, player, system.id(), count), "Invalid recycling order");
        }
    }

    private static int mining(Policy policy, int capacity, int yieldPerPoint, long stock, long remaining) {
        if (remaining == 0) { return 0; }
        if (policy == Policy.MINING) { return Math.max(1, capacity / 2); }
        if (policy == Policy.RECYCLING) { return capacity / 4; }
        int demand = stock >= capacity ? 0 : (int) (capacity - stock);
        return Math.min(capacity, Math.ceilDiv(demand, yieldPerPoint + 1));
    }

    static void validate(GameState state) {
        state.metals().orElseThrow().mining().forEach((id, mining) -> {
            require(mining + state.industries().get(id).expansionAllocation() <= state.usableIndustrialCapacity(id), "Overallocated capacity");
        });
        state.pendingOrders().forEach((player, orders) -> {
            for (var order : orders) {
                if (order instanceof FleetOrder.Send send) {
                    require(send.ownerId() == player && send.ships() > 0, "Invalid fleet owner or quantity");
                }
            }
        });
        var metal = state.metals().orElseThrow();
        RecyclingOrders.validate(state, metal);
    }

    private static void require(boolean valid, String message) { if (!valid) { throw new IllegalStateException(message); } }

    private static final class CheckedEconomy extends DefaultEconomyService {
        @Override public boolean allocateExpansion(GameState state, int player, int system, int points) {
            require(super.allocateExpansion(state, player, system, points), "AI submitted invalid expansion");
            return true;
        }
        @Override public boolean allocateProduction(GameState state, int player, int system, int expansion, int mining) {
            require(super.allocateProduction(state, player, system, expansion, mining), "AI submitted invalid production allocation");
            return true;
        }
    }

    private static final class CheckedFleets extends DefaultFleetService {
        @Override public boolean queueSend(GameState state, int player, int from, int to, int ships,
                                           @org.jspecify.annotations.Nullable Integer beneficiary) {
            require(super.queueSend(state, player, from, to, ships, beneficiary), "AI submitted invalid fleet order");
            return true;
        }
    }
}
