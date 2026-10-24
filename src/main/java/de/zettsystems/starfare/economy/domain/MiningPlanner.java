package de.zettsystems.starfare.economy.domain;

import de.zettsystems.starfare.economy.values.MiningAutomation;
import de.zettsystems.starfare.economy.values.MiningPlan;
import de.zettsystems.starfare.economy.values.MetalRules;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.StarSystem;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Deterministic empire-wide order assistance using the existing exact production forecast. */
public final class MiningPlanner {
    private MiningPlanner() { }

    public static MiningPlan plan(GameState state, int player, MiningAutomation policy) {
        GameState draft = GameState.copyOf(state);
        var original = state.metals().orElseThrow();
        Set<Integer> owned = state.systems().stream().filter(system -> Objects.equals(system.ownerId(), player))
                .map(StarSystem::id).collect(Collectors.toSet());
        Set<Integer> routed = state.standingOrders().getOrDefault(player, List.of()).stream()
                .filter(order -> order.ships() > 0).map(order -> order.fromSystemId()).collect(Collectors.toSet());
        List<Integer> selected = policy.systems().stream().filter(owned::contains).sorted().toList();
        if (!policy.enabled()) { return summarize(draft, player, policy, selected, original.mining()); }
        List<Integer> adjustable = selected.stream().filter(id -> !policy.protectedSystems().contains(id) && !routed.contains(id))
                .sorted(Comparator.<Integer>comparingInt(id -> Objects.requireNonNull(original.deposits().get(id)).yieldPerPoint())
                        .reversed().thenComparingInt(Integer::intValue)).toList();
        adjustable.forEach(id -> draft.planMining(id, 0));
        for (int id : adjustable) {
            if (summarize(draft, player, policy, selected, original.mining()).shortfall() == 0) { break; }
            MetalDeposit deposit = Objects.requireNonNull(original.deposits().get(id));
            int capacity = draft.usableIndustrialCapacity(id) - Objects.requireNonNull(draft.industries().get(id)).expansionAllocation();
            int upper = (int) Math.min(capacity, Math.ceilDiv(deposit.remaining(), deposit.yieldPerPoint()));
            if (upper == 0) { continue; }
            draft.planMining(id, upper);
            if (summarize(draft, player, policy, selected, original.mining()).shortfall() > 0) { continue; }
            int lower = 0;
            while (lower < upper) {
                int middle = lower + (upper - lower) / 2;
                draft.planMining(id, middle);
                if (summarize(draft, player, policy, selected, original.mining()).shortfall() == 0) { upper = middle; }
                else { lower = middle + 1; }
            }
            draft.planMining(id, lower);
        }
        // Even an unreachable reserve must not silently starve an existing route's production.
        var before = state.metalPreview().ships();
        var after = draft.metalPreview().ships();
        if (routed.stream().anyMatch(id -> after.getOrDefault(id, 0) < before.getOrDefault(id, 0))) {
            return summarize(state, player, policy, selected, original.mining());
        }
        return summarize(draft, player, policy, selected, original.mining());
    }

    private static MiningPlan summarize(GameState state, int player, MiningAutomation policy,
                                        List<Integer> selected, Map<Integer, Integer> original) {
        MetalProduction forecast = state.metalPreview();
        long demand = state.systems().stream().filter(system -> Objects.equals(system.ownerId(), player))
                .mapToLong(system -> (long) system.productionPerTurn() * MetalRules.SHIP_COST).sum();
        long consumed = state.systems().stream().filter(system -> Objects.equals(system.ownerId(), player))
                .mapToLong(system -> (long) forecast.ships().getOrDefault(system.id(), 0) * MetalRules.SHIP_COST).sum();
        long target = demand * policy.reserveRounds();
        long stock = forecast.economy().stocks().getOrDefault(player, 0L);
        long shortfall = Math.max(0, target + demand - stock - consumed);
        Map<Integer, Integer> assignments = new HashMap<>();
        Map<Integer, MiningPlan.Change> changes = new HashMap<>();
        var mining = state.metals().orElseThrow().mining();
        for (int id : selected) {
            int points = Objects.requireNonNull(mining.get(id));
            assignments.put(id, points);
            int old = Objects.requireNonNull(original.get(id));
            if (points != old) { changes.put(id, new MiningPlan.Change(old, points)); }
        }
        return new MiningPlan(assignments, changes, demand, target, stock, shortfall);
    }

    public static void applyNewRound(GameState state) {
        for (int player : state.miningAutomation().keySet().stream().sorted().toList()) { applyFor(state, player, true); }
    }

    public static void applyFor(GameState state, int player) { applyFor(state, player, false); }

    private static void applyFor(GameState state, int player, boolean newRound) {
        if (!state.active() || !state.started() || state.gameOver() || !state.ruleset().usesMetals()) { return; }
        MiningAutomation policy = state.miningAutomation().get(player);
        if (policy == null) { return; }
        if (!Objects.equals(state.seatByUser().get(policy.account()), player)
                || !state.joinedHumanPlayerIds().contains(player)) {
            state.miningAutomation().remove(player); return;
        }
        if (!policy.enabled() || (!newRound && state.submittedThisTurn().contains(player))) { return; }
        Set<Integer> owned = state.systems().stream().filter(system -> Objects.equals(system.ownerId(), player))
                .map(StarSystem::id).collect(Collectors.toSet());
        Set<Integer> selected = policy.systems().stream().filter(owned::contains).collect(Collectors.toSet());
        Set<Integer> protectedIds = policy.protectedSystems().stream().filter(selected::contains).collect(Collectors.toSet());
        if (!selected.equals(policy.systems())) { policy = policy.configure(true, policy.reserveRounds(), selected, protectedIds); }
        MiningPlan plan = plan(state, player, policy);
        plan.allocations().forEach(state::planMining);
        state.miningAutomation().put(player, policy.planned(state.turn(), plan));
    }
}
