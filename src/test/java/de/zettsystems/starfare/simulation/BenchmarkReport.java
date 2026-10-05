package de.zettsystems.starfare.simulation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/** Raw evidence plus descriptive win rates; no assertion that limited heuristic opponents prove balance. */
final class BenchmarkReport {
    private BenchmarkReport() { }
    private record Result(List<Integer> winners, int seat, String outcome, long mapSeed, long combatSeed) { }

    static void write(Path output, List<MatchResult> results, List<String> skipped) throws IOException {
        Files.createDirectories(output);
        var matches = new StringBuilder("match,rules,version,layout,systems,map_seed,combat_seed,round_limit,randomness,strategies,experiment,outcome,winner,rounds,sha256,winners,alliance_id,alliances_allowed,alliance_victory,individual_percent,alliance_percent\n");
        var metrics = new StringBuilder("match,round,player,systems,ships,capacity,produced_cumulative,lost_cumulative,invested_cumulative\n");
        var orders = new StringBuilder("match,round,player,action,source,target,amount\n");
        Map<String, List<Result>> groups = new TreeMap<>();
        for (int index = 0; index < results.size(); index++) {
            var result = results.get(index);
            int id = index + 1;
            appendMatch(matches, id, result);
            result.metrics().forEach(row -> metrics.append(id).append(',').append(row.round()).append(',').append(row.player())
                    .append(',').append(row.systems()).append(',').append(row.ships()).append(',').append(row.capacity())
                    .append(',').append(row.produced()).append(',').append(row.lost()).append(',').append(row.invested()).append('\n'));
            result.orders().forEach(row -> orders.append(id).append(',').append(row).append('\n'));
            group(groups, result);
        }
        Files.writeString(output.resolve("matches.csv"), matches);
        Files.writeString(output.resolve("rounds.csv"), metrics);
        Files.writeString(output.resolve("orders.csv"), orders);
        Files.writeString(output.resolve("decision-deltas.csv"), DecisionReport.csv(results));
        Files.write(output.resolve("skipped.txt"), skipped);
        Files.writeString(output.resolve("summary.md"), summary(groups, results.size(), skipped.size()));
    }

    private static void appendMatch(StringBuilder out, int id, MatchResult result) {
        var scenario = result.scenario();
        String seats = result.strategies().stream().map(Enum::name).reduce((a, b) -> a + "/" + b).orElseThrow();
        out.append(id).append(',').append(scenario.rules().variant()).append(',').append(scenario.rules().version())
                .append(',').append(scenario.layout()).append(',').append(scenario.systems()).append(',').append(scenario.mapSeed())
                .append(',').append(scenario.combatSeed()).append(',').append(scenario.roundLimit()).append(',').append(scenario.randomness())
                .append(',').append(seats).append(',').append(result.experiment()).append(',').append(result.outcome())
                .append(',').append(result.winner()).append(',').append(result.rounds()).append(',').append(result.digest()).append(',')
                .append(result.winners().stream().map(String::valueOf).collect(java.util.stream.Collectors.joining("/")))
                .append(',').append(result.allianceId() == null ? "" : result.allianceId())
                .append(',').append(scenario.victoryRules().alliancesAllowed()).append(',').append(scenario.victoryRules().allianceVictoryAllowed())
                .append(',').append(scenario.victoryRules().individualSystemPercent()).append(',').append(scenario.victoryRules().allianceSystemPercent()).append('\n');
    }

    private static void group(Map<String, List<Result>> groups, MatchResult match) {
        var scenario = match.scenario();
        for (int seat = 0; seat < match.strategies().size(); seat++) {
            String opponents = match.strategies().size() == 2 ? match.strategies().get(1 - seat).name() : "mixed-" + match.strategies().size();
            String key = scenario.rules().variant() + "/" + scenario.rules().version() + " | " + scenario.layout() + " | " + scenario.systems() + " | "
                    + match.experiment() + " | " + match.strategies().get(seat) + " | " + opponents;
            groups.computeIfAbsent(key, _ -> new ArrayList<>()).add(new Result(match.winners(), seat + 1,
                    match.outcome(), scenario.mapSeed(), scenario.combatSeed()));
        }
    }

    private static String summary(Map<String, List<Result>> groups, int matches, int skipped) {
        var out = new StringBuilder("# Strategiepruefstand\n\n");
        out.append(matches).append(" Partien; ").append(skipped).append(" wirkungslose oder nicht erreichbare Versuche ausgelassen.\n\n")
                .append("Siegquoten beziehen sich auf alle Laeufe, einschliesslich Rundenlimit. Ein Limit ist kein Unentschieden. ")
                .append("Sitze derselben Karte bilden einen gemeinsamen Stichprobenblock. Das 95%-Intervall wird durch 2.000 ")
                .append("deterministische Bootstrap-Ziehungen ganzer Kartenbloecke geschaetzt; bei weniger als zwei Karten ist es nicht bestimmbar. ")
                .append("Wenige Karten liefern keine belastbare Balanceaussage. Strategien und Startwirtschaft sind feste Versuchsvorgaben.\n\n")
                .append("| Variante | Karte | Systeme | Versuch | Strategie | Gegner | Laeufe | Siege | Niederlagen | Remis | Limit | Siegquote | 95%-Intervall | Karten |\n")
                .append("|---|---|---:|---|---|---|---:|---:|---:|---:|---:|---:|---|---:|\n");
        groups.forEach((key, rows) -> {
            long wins = rows.stream().filter(row -> row.winners().contains(row.seat())).count();
            long losses = rows.stream().filter(row -> !row.winners().isEmpty() && !row.winners().contains(row.seat())).count();
            long draws = rows.stream().filter(row -> row.outcome().equals("DRAW")).count();
            long limits = rows.stream().filter(row -> row.outcome().equals("ROUND_LIMIT")).count();
            Map<Long, List<Result>> blocks = new TreeMap<>();
            rows.forEach(row -> blocks.computeIfAbsent(row.mapSeed(), _ -> new ArrayList<>()).add(row));
            var rates = blocks.values().stream().mapToDouble(block -> block.stream()
                    .filter(row -> row.winners().contains(row.seat())).count() / (double) block.size()).toArray();
            out.append("| ").append(key).append(" | ").append(rows.size()).append(" | ").append(wins)
                    .append(" | ").append(losses).append(" | ").append(draws).append(" | ").append(limits)
                    .append(" | ").append(percent(wins / (double) rows.size())).append(" | ").append(interval(rates))
                    .append(" | ").append(blocks.size()).append(" |\n");
        });
        out.append("\nEinzelverlaeufe: rounds.csv. Befehle: orders.csv. Parameter und Fingerabdruck: matches.csv. ")
                .append("Neue Befehle und Folgeentscheidungen nach einem Eingriff duerfen auseinanderlaufen. Gleiche Rundenzufallswerte ")
                .append("garantieren bei veraenderten Schlachten keine identischen Wuerfe je Kampf. ")
                .append("Buendnisversuche verwenden einen festen Vertrag der Sitze 1 und 2, keine diplomatische KI. ")
                .append("matches.csv haelt erlaubten Gruppensieg, getrennte Schwellen und saemtliche Sieger fest.\n");
        return out.toString();
    }

    private static String interval(double[] blocks) {
        if (blocks.length < 2) { return "nicht bestimmbar"; }
        var random = new Random(20261005);
        double[] samples = new double[2000];
        for (int draw = 0; draw < samples.length; draw++) {
            for (int index = 0; index < blocks.length; index++) { samples[draw] += blocks[random.nextInt(blocks.length)] / blocks.length; }
        }
        Arrays.sort(samples);
        return percent(samples[49]) + " .. " + percent(samples[1949]);
    }

    private static String percent(double value) { return String.format(Locale.ROOT, "%.1f%%", value * 100); }
}
