package de.zettsystems.starfare.economy.values;

import java.util.HashSet;
import java.util.Set;

/** Opt-in player orders, not economic rules. Immutable so snapshots and optimistic checks share it. */
public record MiningAutomation(String account, boolean enabled, int reserveRounds, Set<Integer> systems,
                               Set<Integer> protectedSystems, int revision, int plannedTurn, MiningPlan lastPlan) {
    public MiningAutomation {
        java.util.Objects.requireNonNull(account);
        java.util.Objects.requireNonNull(lastPlan);
        systems = Set.copyOf(systems);
        protectedSystems = Set.copyOf(protectedSystems);
        if (reserveRounds < 0 || reserveRounds > 10 || revision < 0 || !systems.containsAll(protectedSystems)) {
            throw new IllegalArgumentException("Invalid mining automation settings");
        }
    }
    public static MiningAutomation off(String account) {
        return new MiningAutomation(account, false, 1, Set.of(), Set.of(), 0, 0, MiningPlan.EMPTY);
    }
    public MiningAutomation configure(boolean active, int rounds, Set<Integer> selected, Set<Integer> protectedIds) {
        return new MiningAutomation(account, active, rounds, selected, protectedIds, Math.incrementExact(revision), 0, MiningPlan.EMPTY);
    }
    public MiningAutomation planned(int turn, MiningPlan plan) {
        return new MiningAutomation(account, enabled, reserveRounds, systems, protectedSystems,
                Math.incrementExact(revision), turn, plan);
    }
    public MiningAutomation manual(int system) {
        if (!systems.contains(system)) { return this; }
        Set<Integer> selected = new HashSet<>(systems);
        Set<Integer> locked = new HashSet<>(protectedSystems);
        selected.remove(system); locked.remove(system);
        return configure(enabled, reserveRounds, selected, locked);
    }
    public MiningAutomation protect(int system, boolean protect) {
        if (!systems.contains(system) || protectedSystems.contains(system) == protect) { return this; }
        Set<Integer> locked = new HashSet<>(protectedSystems);
        if (protect) { locked.add(system); } else { locked.remove(system); }
        return configure(enabled, reserveRounds, systems, locked);
    }
}
