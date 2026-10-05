package de.zettsystems.starfare.simulation;

import java.util.List;

record MatchResult(Scenario scenario, List<Strategy> strategies, String experiment, String outcome,
                   int winner, List<RoundMetric> metrics, List<String> orders, String digest, List<Integer> winners, Integer allianceId) {
    MatchResult(Scenario scenario, List<Strategy> strategies, String experiment, String outcome,
                int winner, List<RoundMetric> metrics, List<String> orders, String digest) {
        this(scenario, strategies, experiment, outcome, winner, metrics, orders, digest,
                winner == 0 ? List.of() : List.of(winner), null);
    }
    record RoundMetric(int round, int player, int systems, long ships, long capacity,
                       long produced, long lost, long invested) { }
    MatchResult {
        strategies = List.copyOf(strategies);
        winners = List.copyOf(winners);
        metrics = List.copyOf(metrics);
        orders = List.copyOf(orders);
    }
    int rounds() { return metrics.getLast().round(); }
}
