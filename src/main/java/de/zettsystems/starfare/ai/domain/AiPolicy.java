package de.zettsystems.starfare.ai.domain;

import de.zettsystems.starfare.ai.values.AiObservation;
import de.zettsystems.starfare.ai.values.AiOrders;
import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.values.VisibleSystem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Deliberately simple contrasting policies, not claims of optimal play. All ties use system IDs. */
public final class AiPolicy {
    private AiPolicy() { }

    public static AiOrders plan(AiStrategy strategy, AiObservation observation) {
        if (strategy == AiStrategy.BASELINE) { throw new IllegalArgumentException("Use the production baseline adapter"); }
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
                int ships = strategy == AiStrategy.RUSH ? available
                        : Math.min(available, required(observation, source, target, committed));
                sends.add(new FleetOrder.Send(observation.player(), source.id(), target.id(), ships));
                committed.merge(target.id(), ships, Integer::sum);
            } else {
                reinforce(strategy, observation, source, owned, available, sends);
            }
        }
        return new AiOrders(sends, expansion);
    }

    private static int allocate(AiStrategy strategy, AiObservation observation, VisibleSystem source,
                                Map<Integer, Integer> expansion) {
        var industry = source.industry();
        Integer production = source.productionPerTurn();
        if (industry == null) { return production == null ? 0 : production; }
        boolean threatened = observation.view().systems().stream().anyMatch(target -> target.ownerId() != null
                && observation.hostile(target) && observation.rounds(source.id(), target.id()) <= 2);
        int points = 0;
        if (strategy == AiStrategy.INDUSTRY && !industry.atMaximum()) {
            points = threatened ? industry.capacity() / 5 : industry.capacity() * 2 / 3;
        }
        if (strategy == AiStrategy.INDUSTRY_LIGHT || strategy == AiStrategy.INDUSTRY_ADAPTIVE) {
            int nearestEnemy = observation.view().systems().stream()
                    .filter(target -> target.ownerId() != null && observation.hostile(target))
                    .mapToInt(target -> observation.rounds(source.id(), target.id())).min().orElse(Integer.MAX_VALUE);
            points = IndustryPolicy.allocation(strategy, industry, nearestEnemy, available(source));
        }
        expansion.put(source.id(), points);
        return industry.capacity() - points;
    }

    private static int reserve(AiStrategy strategy, int output) {
        return switch (strategy) {
            case RUSH -> 1;
            case DEFENSE -> Math.max(5, output * 3);
            default -> Math.max(1, output);
        };
    }

    private static @Nullable VisibleSystem chooseTarget(AiStrategy strategy, AiObservation view, VisibleSystem source,
                                              int available, Map<Integer, Integer> committed) {
        return view.view().systems().stream().filter(view::hostile)
                .filter(target -> view.rounds(source.id(), target.id()) != Integer.MAX_VALUE)
                .filter(target -> strategy == AiStrategy.RUSH || required(view, source, target, committed) <= available)
                .filter(target -> committed.getOrDefault(target.id(), 0) == 0)
                .min(Comparator.comparingDouble((VisibleSystem target) -> score(strategy, view, source, target))
                        .thenComparingInt(VisibleSystem::id)).orElse(null);
    }

    private static double score(AiStrategy strategy, AiObservation view, VisibleSystem source, VisibleSystem target) {
        int distance = view.rounds(source.id(), target.id());
        boolean enemy = target.ownerId() != null;
        return switch (strategy) {
            case RUSH -> distance - (enemy ? 3 : 0);
            case EXPANSION -> distance + knownDefense(target) / 5.0 + (enemy ? 8 : 0);
            case DEFENSE -> distance + knownDefense(target) / 3.0 + (enemy ? 12 : 0);
            default -> distance + knownDefense(target) / 8.0;
        };
    }

    private static int required(AiObservation view, VisibleSystem source, VisibleSystem target, Map<Integer, Integer> committed) {
        int rounds = view.rounds(source.id(), target.id());
        Integer production = target.productionPerTurn();
        int growth = target.ownerId() != null && production != null ? production * rounds : 0;
        int inbound = view.view().ownFleets().stream()
                .filter(fleet -> fleet.toSystemId() == target.id() && fleet.arrivalTurn() <= view.view().turn() + rounds)
                .mapToInt(fleet -> fleet.ships()).sum();
        return Math.max(1, (int) Math.ceil((knownDefense(target) + growth) * 1.35) + 1
                - inbound - committed.getOrDefault(target.id(), 0));
    }

    private static void reinforce(AiStrategy strategy, AiObservation view, VisibleSystem source,
                                  List<VisibleSystem> owned, int available, List<FleetOrder.Send> sends) {
        if (strategy != AiStrategy.CONCENTRATION && strategy != AiStrategy.DEFENSE && !strategy.requiresIndustry()) { return; }
        var front = owned.stream().filter(system -> view.rounds(source.id(), system.id()) != Integer.MAX_VALUE)
                .filter(system -> frontDistance(view, system) < frontDistance(view, source))
                .min(Comparator.comparingInt((VisibleSystem system) -> frontDistance(view, system))
                        .thenComparingInt(VisibleSystem::id)).orElse(null);
        if (front != null) { sends.add(new FleetOrder.Send(view.player(), source.id(), front.id(), available)); }
    }

    private static int frontDistance(AiObservation view, VisibleSystem source) {
        return view.view().systems().stream().filter(view::hostile)
                .mapToInt(target -> view.rounds(source.id(), target.id())).min().orElse(Integer.MAX_VALUE);
    }

    private static int knownDefense(VisibleSystem system) {
        Integer garrison = system.garrison();
        return garrison == null ? 5 : garrison;
    }

    private static int available(VisibleSystem system) {
        Integer ships = system.availableShips();
        return ships == null ? 0 : ships;
    }
}
