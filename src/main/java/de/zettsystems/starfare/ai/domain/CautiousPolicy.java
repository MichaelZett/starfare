package de.zettsystems.starfare.ai.domain;

import de.zettsystems.starfare.ai.values.AiObservation;
import de.zettsystems.starfare.ai.values.AiOrders;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.VisibleSystem;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * Calm planning of normal classic computers (strategy bench K7d, docs/STRATEGY-EXPERIMENTS.md): few, large attacks a
 * person can see coming instead of a fleet every round. Every system keeps a home guard; at most one fleet leaves per
 * round, and only when a system's spare ships clearly beat the estimated defence at arrival. It then takes all of them.
 * Near, weak and neutral targets come first, and no second fleet heads for a target one is already flying to. Like the
 * other profiles it sees only what a person would; ties use system IDs.
 */
final class CautiousPolicy {
    /** Ships per estimated defender at arrival, the defender's production until then included. */
    static final double SUPERIORITY = 1.5;
    /**
     * Rounds of its own production a system keeps at home against a single rival. With more rivals each one spreads
     * its strength over more fronts; the guard shrinks with the square root of their number.
     */
    static final double GUARD_ROUNDS = 3;
    /** Rounds of travel a system of another empire counts more than a neutral one. */
    static final int EMPIRE_PENALTY = 2;
    /** Assumed garrison of a system without any known value. */
    static final int UNKNOWN_DEFENCE = 6;
    /** Assumed production of another empire's system without a known value. */
    static final int UNKNOWN_PRODUCTION = 5;

    private CautiousPolicy() { }

    static AiOrders plan(AiObservation observation) {
        var owned = observation.view().systems().stream().filter(observation::owned)
                .sorted(Comparator.comparingInt(VisibleSystem::id)).toList();
        double guardRounds = guardRounds(observation);
        Set<Integer> ownIds = owned.stream().map(VisibleSystem::id).collect(Collectors.toSet());
        Set<Integer> engaged = observation.view().ownFleets().stream().map(Fleet::toSystemId)
                .filter(target -> !ownIds.contains(target)).collect(Collectors.toSet());
        var operation = bestOperation(observation, owned, guardRounds, engaged);
        Map<Integer, Integer> expansion = new HashMap<>();
        owned.stream().filter(system -> system.industry() != null).forEach(system -> expansion.put(system.id(), 0));
        List<FleetOrder.Send> sends = operation == null ? List.of()
                : List.of(new FleetOrder.Send(observation.player(), operation.source(), operation.target(), operation.ships()));
        return new AiOrders(sends, expansion);
    }

    private record Operation(int source, int target, int ships, double score) {
        boolean betterThan(Operation other) {
            if (score != other.score) { return score < other.score; }
            if (target != other.target) { return target < other.target; }
            return source < other.source;
        }
    }

    private static @Nullable Operation bestOperation(AiObservation observation, List<VisibleSystem> owned,
                                                     double guardRounds, Set<Integer> engaged) {
        Operation best = null;
        for (var target : observation.view().systems()) {
            if (!observation.hostile(target) || engaged.contains(target.id())) { continue; }
            for (var source : owned) {
                int rounds = observation.rounds(source.id(), target.id());
                int spare = spare(source, guardRounds);
                if (rounds == Integer.MAX_VALUE || required(target, rounds) > spare) { continue; }
                double score = rounds + defence(target) / 8.0 + (target.ownerId() != null ? EMPIRE_PENALTY : 0);
                var candidate = new Operation(source.id(), target.id(), spare, score);
                if (best == null || candidate.betterThan(best)) { best = candidate; }
            }
        }
        return best;
    }

    /** Ships for a clear superiority over the defence the target will have when the fleet arrives. */
    static int required(VisibleSystem target, int rounds) {
        int growth = target.ownerId() == null ? 0
                : Objects.requireNonNullElse(target.productionPerTurn(), UNKNOWN_PRODUCTION) * rounds;
        return (int) Math.ceil((defence(target) + growth) * SUPERIORITY) + 1;
    }

    /** Rounds of production every system keeps: three against one rival, fewer the more rivals share the galaxy. */
    static double guardRounds(AiObservation observation) {
        int rivals = (int) observation.view().players().stream()
                .filter(player -> player.id() != observation.player() && !observation.allies().contains(player.id()))
                .count();
        return GUARD_ROUNDS / Math.sqrt(Math.max(1, rivals));
    }

    /** Sendable ships above the home guard. */
    static int spare(VisibleSystem system, double guardRounds) {
        int production = Math.max(1, Objects.requireNonNullElse(system.productionPerTurn(), 0));
        return Objects.requireNonNullElse(system.availableShips(), 0) - (int) Math.round(production * guardRounds);
    }

    private static int defence(VisibleSystem system) {
        return Objects.requireNonNullElse(system.garrison(), UNKNOWN_DEFENCE);
    }
}
