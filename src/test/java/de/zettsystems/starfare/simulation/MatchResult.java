package de.zettsystems.starfare.simulation;

import java.util.List;

record MatchResult(Scenario scenario, List<Strategy> strategies, String experiment, String outcome,
                   int winner, List<RoundMetric> metrics, List<String> orders, String digest, List<Integer> winners, Integer allianceId,
                   List<SeatActivity> activity) {
    MatchResult(Scenario scenario, List<Strategy> strategies, String experiment, String outcome,
                int winner, List<RoundMetric> metrics, List<String> orders, String digest) {
        this(scenario, strategies, experiment, outcome, winner, metrics, orders, digest,
                winner == 0 ? List.of() : List.of(winner), null, List.of());
    }
    record RoundMetric(int round, int player, int systems, long ships, long capacity,
                       long produced, long lost, long invested) { }

    /**
     * How often one seat sent fleets, counted when it planned: attacks go to a system another empire held at that
     * moment, reference attacks to one of seat 1.
     *
     * @param activeRounds        rounds in which the seat held a system and could plan
     * @param firstReferenceAttack round of the first attack on seat 1, 0 without one
     */
    record SeatActivity(int player, int activeRounds, int sends, int attacks, long attackShips, int referenceAttacks,
                        int firstReferenceAttack) {
        SeatActivity record(int round, boolean attack, boolean onReference, int ships) {
            return new SeatActivity(player, activeRounds, sends + 1, attacks + (attack ? 1 : 0),
                    attackShips + (attack ? ships : 0), referenceAttacks + (onReference ? 1 : 0),
                    firstReferenceAttack == 0 && onReference ? round : firstReferenceAttack);
        }

        SeatActivity active() {
            return new SeatActivity(player, activeRounds + 1, sends, attacks, attackShips, referenceAttacks, firstReferenceAttack);
        }
    }

    MatchResult {
        strategies = List.copyOf(strategies);
        winners = List.copyOf(winners);
        metrics = List.copyOf(metrics);
        orders = List.copyOf(orders);
        activity = List.copyOf(activity);
    }
    int rounds() { return metrics.getLast().round(); }
}
