package de.zettsystems.starfare.simulation;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Compares the first player's outcomes at identical resolved rounds, never mismatched end dates. */
final class DecisionReport {
    private DecisionReport() { }

    static String csv(List<MatchResult> matches) {
        var out = new StringBuilder("rules,layout,systems,map_seed,combat_seed,experiment,round,systems_delta,ships_delta,capacity_delta,produced_delta,lost_delta,invested_delta\n");
        for (var reference : matches) {
            if (!reference.experiment().endsWith("-NONE")) { continue; }
            Map<Integer, MatchResult.RoundMetric> rounds = reference.metrics().stream().filter(row -> row.player() == 1)
                    .collect(Collectors.toMap(MatchResult.RoundMetric::round, Function.identity()));
            matches.stream().filter(match -> match.scenario().equals(reference.scenario())
                    && match.strategies().equals(reference.strategies()) && !match.experiment().equals(reference.experiment()))
                    .forEach(match -> append(out, match, rounds));
        }
        return out.toString();
    }

    private static void append(StringBuilder out, MatchResult match, Map<Integer, MatchResult.RoundMetric> reference) {
        for (var row : match.metrics()) {
            var original = reference.get(row.round());
            if (row.player() != 1 || original == null) { continue; }
            var scenario = match.scenario();
            out.append(scenario.rules().variant()).append(',').append(scenario.layout()).append(',').append(scenario.systems())
                    .append(',').append(scenario.mapSeed()).append(',').append(scenario.combatSeed()).append(',').append(match.experiment())
                    .append(',').append(row.round()).append(',').append(row.systems() - original.systems())
                    .append(',').append(row.ships() - original.ships()).append(',').append(row.capacity() - original.capacity())
                    .append(',').append(row.produced() - original.produced()).append(',').append(row.lost() - original.lost())
                    .append(',').append(row.invested() - original.invested()).append('\n');
        }
    }
}
