package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.values.VisibleSystem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Deliberately simple contrasting policies, not claims of optimal play. All ties use system IDs. */
final class HeuristicPolicy {
    private HeuristicPolicy() { }

    static StrategyOrders plan(Strategy strategy, Observation observation) {
        if (strategy == Strategy.BASELINE) { throw new IllegalArgumentException("Use the production baseline adapter"); }
        var owned = observation.view().systems().stream().filter(observation::owned)
                .sorted(Comparator.comparingInt(VisibleSystem::id)).toList();
        var sends = new ArrayList<FleetOrder.Send>();
        Map<Integer, Integer> expansion = new HashMap<>();
        Map<Integer, Integer> committed = new HashMap<>();
        for (var source : owned) {
            int output = allocate(strategy, observation, source, expansion);
            int available = available(source) - reserve(strategy, output);
            if (available <= 0) { continue; }
            var target = chooseTarget(strategy, observation, source, available, committed);
            if (target != null) {
                int ships = strategy == Strategy.RUSH ? available
                        : Math.min(available, required(observation, source, target, committed));
                sends.add(new FleetOrder.Send(observation.player(), source.id(), target.id(), ships));
                committed.merge(target.id(), ships, Integer::sum);
            } else {
                reinforce(strategy, observation, source, owned, available, sends);
            }
        }
        return new StrategyOrders(sends, expansion);
    }

    private static int allocate(Strategy strategy, Observation observation, VisibleSystem source,
                                Map<Integer, Integer> expansion) {
        var industry = source.industry();
        if (industry == null) { return source.productionPerTurn() == null ? 0 : source.productionPerTurn(); }
        boolean threatened = observation.view().systems().stream().anyMatch(target -> target.ownerId() != null
                && observation.hostile(target) && observation.rounds(source.id(), target.id()) <= 2);
        int points = 0;
        if (strategy == Strategy.INDUSTRY && !industry.atMaximum()) {
            points = threatened ? industry.capacity() / 5 : industry.capacity() * 2 / 3;
        }
        expansion.put(source.id(), points);
        return industry.capacity() - points;
    }

    private static int reserve(Strategy strategy, int output) {
        return switch (strategy) {
            case RUSH -> 1;
            case DEFENSE -> Math.max(5, output * 3);
            default -> Math.max(1, output);
        };
    }

    private static VisibleSystem chooseTarget(Strategy strategy, Observation view, VisibleSystem source,
                                              int available, Map<Integer, Integer> committed) {
        return view.view().systems().stream().filter(view::hostile)
                .filter(target -> view.rounds(source.id(), target.id()) != Integer.MAX_VALUE)
                .filter(target -> strategy == Strategy.RUSH || required(view, source, target, committed) <= available)
                .filter(target -> committed.getOrDefault(target.id(), 0) == 0)
                .min(Comparator.comparingDouble((VisibleSystem target) -> score(strategy, view, source, target))
                        .thenComparingInt(VisibleSystem::id)).orElse(null);
    }

    private static double score(Strategy strategy, Observation view, VisibleSystem source, VisibleSystem target) {
        int distance = view.rounds(source.id(), target.id());
        boolean enemy = target.ownerId() != null;
        return switch (strategy) {
            case RUSH -> distance - (enemy ? 3 : 0);
            case EXPANSION -> distance + knownDefense(target) / 5.0 + (enemy ? 8 : 0);
            case DEFENSE -> distance + knownDefense(target) / 3.0 + (enemy ? 12 : 0);
            default -> distance + knownDefense(target) / 8.0;
        };
    }

    private static int required(Observation view, VisibleSystem source, VisibleSystem target, Map<Integer, Integer> committed) {
        int rounds = view.rounds(source.id(), target.id());
        int growth = target.ownerId() != null && target.productionPerTurn() != null ? target.productionPerTurn() * rounds : 0;
        int inbound = view.view().ownFleets().stream()
                .filter(fleet -> fleet.toSystemId() == target.id() && fleet.arrivalTurn() <= view.view().turn() + rounds)
                .mapToInt(fleet -> fleet.ships()).sum();
        return Math.max(1, (int) Math.ceil((knownDefense(target) + growth) * 1.35) + 1
                - inbound - committed.getOrDefault(target.id(), 0));
    }

    private static void reinforce(Strategy strategy, Observation view, VisibleSystem source,
                                  List<VisibleSystem> owned, int available, List<FleetOrder.Send> sends) {
        if (strategy != Strategy.CONCENTRATION && strategy != Strategy.DEFENSE && strategy != Strategy.INDUSTRY) { return; }
        var front = owned.stream().filter(system -> view.rounds(source.id(), system.id()) != Integer.MAX_VALUE)
                .filter(system -> frontDistance(view, system) < frontDistance(view, source))
                .min(Comparator.comparingInt((VisibleSystem system) -> frontDistance(view, system))
                        .thenComparingInt(VisibleSystem::id)).orElse(null);
        if (front != null) { sends.add(new FleetOrder.Send(view.player(), source.id(), front.id(), available)); }
    }

    private static int frontDistance(Observation view, VisibleSystem source) {
        return view.view().systems().stream().filter(view::hostile)
                .mapToInt(target -> view.rounds(source.id(), target.id())).min().orElse(Integer.MAX_VALUE);
    }

    private static int knownDefense(VisibleSystem system) { return system.garrison() == null ? 5 : system.garrison(); }
    private static int available(VisibleSystem system) { return system.availableShips() == null ? 0 : system.availableShips(); }
}
