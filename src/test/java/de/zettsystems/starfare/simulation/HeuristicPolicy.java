package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.ai.domain.AiPolicy;
import de.zettsystems.starfare.ai.values.AiObservation;
import de.zettsystems.starfare.ai.values.AiStrategy;
import java.util.HashMap;
import java.util.Map;

/** Runs the same policies as selectable game AI against captured observations. */
final class HeuristicPolicy {
    private HeuristicPolicy() { }
    static StrategyOrders plan(Strategy strategy, Observation observation) {
        Map<AiObservation.Route, Integer> travel = new HashMap<>();
        observation.travel().forEach((route, rounds) -> travel.put(new AiObservation.Route(route.from(), route.to()), rounds));
        var orders = AiPolicy.plan(AiStrategy.valueOf(strategy.name()),
                new AiObservation(observation.player(), observation.view(), travel, observation.allies()));
        return new StrategyOrders(orders.sends(), orders.expansion());
    }
}
