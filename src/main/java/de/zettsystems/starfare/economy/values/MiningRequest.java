package de.zettsystems.starfare.economy.values;

import java.util.Map;
import java.util.Set;

/** Settings change with optimistic checks against both policy and the complete own production plan. */
public record MiningRequest(int turn, MiningAutomation expected, Map<Integer, AllocationSplit> plans,
                            boolean enabled, int reserveRounds, Set<Integer> systems, Set<Integer> protectedSystems,
                            @org.jspecify.annotations.Nullable MiningPlan reviewed) {
    public MiningRequest(int turn, MiningAutomation expected, Map<Integer, AllocationSplit> plans,
                         boolean enabled, int reserveRounds, Set<Integer> systems, Set<Integer> protectedSystems) {
        this(turn, expected, plans, enabled, reserveRounds, systems, protectedSystems, null);
    }
    public MiningRequest {
        plans = Map.copyOf(plans); systems = Set.copyOf(systems); protectedSystems = Set.copyOf(protectedSystems);
    }
    public MiningRequest withReviewed(MiningPlan plan) {
        return new MiningRequest(turn, expected, plans, enabled, reserveRounds, systems, protectedSystems, plan);
    }
}
