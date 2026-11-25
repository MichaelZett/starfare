package de.zettsystems.starfare.simulation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Win rates of the reference against each computer strength, game lengths and the effect of luck. */
final class StrengthReport {
    private StrengthReport() { }

    static void write(Path output, List<StrengthBenchmark.Match> matches, List<StrengthBenchmark.MapFacts> maps) throws IOException {
        Files.createDirectories(output);
        var csv = new StringBuilder("strength,randomness,events,systems,players,map_seed,combat_seed,computers,outcome,winner,rounds,sha256,"
                + "computer_rounds,sends,attacks,attack_ships,reference_attacks,first_reference_attack\n");
        matches.forEach(match -> csv.append(match.strength()).append(',').append(match.level()).append(',').append(match.events())
                .append(',').append(match.systems()).append(',').append(match.players()).append(',').append(match.mapSeed())
                .append(',').append(match.combatSeed()).append(',').append(profiles(match)).append(',').append(match.outcome())
                .append(',').append(match.winner()).append(',').append(match.rounds()).append(',').append(match.digest())
                .append(',').append(sum(match, MatchResult.SeatActivity::activeRounds)).append(',').append(sum(match, MatchResult.SeatActivity::sends))
                .append(',').append(sum(match, MatchResult.SeatActivity::attacks)).append(',').append(sum(match, MatchResult.SeatActivity::attackShips))
                .append(',').append(sum(match, MatchResult.SeatActivity::referenceAttacks)).append(',').append(firstReferenceAttack(match))
                .append('\n'));
        Files.writeString(output.resolve("matches.csv"), csv);
        Files.writeString(output.resolve("strength.md"), summary(matches, maps));
    }

    static String summary(List<StrengthBenchmark.Match> matches, List<StrengthBenchmark.MapFacts> maps) {
        var out = new StringBuilder("# Computerstaerke\n\n").append(matches.size())
                .append(" Partien. Sitz 1 ist die Referenz fuer den Menschen. Quote = Siege der Referenz / alle Partien,")
                .append(" offene Partien (Rundenlimit) zaehlen nicht als Sieg. Intervall: Wilson 95 %, Partien als unabhaengig gezaehlt.\n\n")
                .append("| Staerke | Zufall | Ereignisse | Systeme | Spieler | Partien | Referenz | Computer | offen | Quote | 95 % | Runden Median | Runden Mittel | Runden P90 |\n")
                .append("|---|---|---|---:|---:|---:|---:|---:|---:|---:|---|---:|---:|---:|\n");
        group(matches, match -> match.strength() + " | " + match.level() + " | " + (match.events() ? "an" : "aus") + " | "
                + match.systems() + " | " + match.players()).forEach((key, rows) -> {
            long wins = rows.stream().filter(StrengthBenchmark.Match::referenceWon).count();
            long computers = rows.stream().filter(row -> row.decided() && !row.referenceWon()).count();
            var rounds = rows.stream().filter(StrengthBenchmark.Match::decided).mapToInt(StrengthBenchmark.Match::rounds).sorted().toArray();
            out.append("| ").append(key).append(" | ").append(rows.size()).append(" | ").append(wins).append(" | ").append(computers)
                    .append(" | ").append(rows.size() - wins - computers).append(" | ").append(percent(wins / (double) rows.size()))
                    .append(" | ").append(wilson(wins, rows.size())).append(" | ").append(quantile(rounds, 0.5)).append(" | ")
                    .append(rounds.length == 0 ? "-" : String.format(Locale.ROOT, "%.1f", java.util.Arrays.stream(rounds).average().orElseThrow()))
                    .append(" | ").append(quantile(rounds, 0.9)).append(" |\n");
        });
        appendProfiles(out, matches);
        appendActivity(out, matches);
        appendCombatSeeds(out, matches);
        appendEvents(out, matches);
        appendMaps(out, maps);
        return out.toString();
    }

    /** Duels split by the drawn computer profile: which profile makes a strength hard or easy. */
    private static void appendProfiles(StringBuilder out, List<StrengthBenchmark.Match> matches) {
        out.append("\n## Nach Computerprofil\n\n| Staerke | Zufall | Systeme | Spieler | Profile | Partien | Referenz | Quote |\n|---|---|---:|---:|---|---:|---:|---:|\n");
        group(matches.stream().filter(StrengthBenchmark.Match::events).toList(), match -> match.strength() + " | " + match.level() + " | "
                + match.systems() + " | " + match.players() + " | " + profiles(match)).forEach((key, rows) -> {
            long wins = rows.stream().filter(StrengthBenchmark.Match::referenceWon).count();
            out.append("| ").append(key).append(" | ").append(rows.size()).append(" | ").append(wins).append(" | ")
                    .append(percent(wins / (double) rows.size())).append(" |\n");
        });
    }

    /**
     * How restless the computers play: sends and attacks on other empires per computer and round it held a system,
     * ships per attack, and when a computer first attacked the reference.
     */
    private static void appendActivity(StringBuilder out, List<StrengthBenchmark.Match> matches) {
        out.append("\n## Angriffe der Computer\n\nJe Computer und Runde mit eigenem System. Angriff = Sendung an ein System eines")
                .append(" anderen Reichs (beim Planen). Erster Angriff auf die Referenz: Median der Computer, die sie angriffen.\n\n")
                .append("| Staerke | Zufall | Ereignisse | Systeme | Spieler | Sendungen | Angriffe | Schiffe je Angriff | Angriffe auf Referenz ")
                .append("| erster Angriff auf Referenz | ohne Angriff auf Referenz |\n|---|---|---|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        group(matches, match -> match.strength() + " | " + match.level() + " | " + (match.events() ? "an" : "aus") + " | "
                + match.systems() + " | " + match.players()).forEach((key, rows) -> {
            var seats = rows.stream().flatMap(match -> match.activity().stream()).toList();
            long rounds = seats.stream().mapToLong(MatchResult.SeatActivity::activeRounds).sum();
            long attacks = seats.stream().mapToLong(MatchResult.SeatActivity::attacks).sum();
            var first = seats.stream().mapToInt(MatchResult.SeatActivity::firstReferenceAttack).filter(round -> round > 0).sorted().toArray();
            out.append("| ").append(key).append(" | ").append(rate(seats.stream().mapToLong(MatchResult.SeatActivity::sends).sum(), rounds))
                    .append(" | ").append(rate(attacks, rounds)).append(" | ")
                    .append(attacks == 0 ? "-" : String.format(Locale.ROOT, "%.1f",
                            seats.stream().mapToLong(MatchResult.SeatActivity::attackShips).sum() / (double) attacks))
                    .append(" | ").append(rate(seats.stream().mapToLong(MatchResult.SeatActivity::referenceAttacks).sum(), rounds))
                    .append(" | ").append(quantile(first, 0.5)).append(" | ")
                    .append(percent(seats.isEmpty() ? 0 : (seats.size() - first.length) / (double) seats.size())).append(" |\n");
        });
    }

    private static String rate(long count, long rounds) {
        return rounds == 0 ? "-" : String.format(Locale.ROOT, "%.2f", count / (double) rounds);
    }

    private static long sum(StrengthBenchmark.Match match, java.util.function.ToLongFunction<MatchResult.SeatActivity> value) {
        return match.activity().stream().mapToLong(value).sum();
    }

    private static int firstReferenceAttack(StrengthBenchmark.Match match) {
        return match.activity().stream().mapToInt(MatchResult.SeatActivity::firstReferenceAttack).filter(round -> round > 0)
                .min().orElse(0);
    }

    /** Maps whose winner changes with the combat seed alone. */
    private static void appendCombatSeeds(StringBuilder out, List<StrengthBenchmark.Match> matches) {
        if (matches.stream().map(StrengthBenchmark.Match::combatSeed).distinct().count() < 2) { return; }
        out.append("\n## Kampfstartwert\n\nKarten, deren Sieger sich allein mit dem Kampfstartwert aendert.\n\n")
                .append("| Staerke | Zufall | Ereignisse | Systeme | Spieler | Karten | Sieger wechselt | Anteil |\n|---|---|---|---:|---:|---:|---:|---:|\n");
        group(matches, match -> match.strength() + " | " + match.level() + " | " + (match.events() ? "an" : "aus") + " | "
                + match.systems() + " | " + match.players()).forEach((key, rows) -> {
            var byMap = rows.stream().collect(Collectors.groupingBy(StrengthBenchmark.Match::mapSeed, TreeMap::new, Collectors.toList()));
            long changed = byMap.values().stream().filter(seeds -> seeds.stream().map(StrengthBenchmark.Match::winner).distinct().count() > 1).count();
            out.append("| ").append(key).append(" | ").append(byMap.size()).append(" | ").append(changed).append(" | ")
                    .append(percent(changed / (double) byMap.size())).append(" |\n");
        });
    }

    /** The same map, dice and profiles with and without events. */
    private static void appendEvents(StringBuilder out, List<StrengthBenchmark.Match> matches) {
        if (matches.stream().map(StrengthBenchmark.Match::events).distinct().count() < 2) { return; }
        out.append("\n## Ereignisse\n\nGleiche Karte, Kampfstartwerte und Profile, einmal mit und einmal ohne Ereignisse.\n\n")
                .append("| Staerke | Zufall | Systeme | Spieler | Paare | anderer Sieger | Anteil | Runden mit | Runden ohne |\n|---|---|---:|---:|---:|---:|---:|---:|---:|\n");
        group(matches, match -> match.strength() + " | " + match.level() + " | " + match.systems() + " | " + match.players()).forEach((key, rows) -> {
            Map<String, List<StrengthBenchmark.Match>> pairs = rows.stream().collect(Collectors.groupingBy(
                    match -> match.mapSeed() + "/" + match.combatSeed(), TreeMap::new, Collectors.toList()));
            var complete = pairs.values().stream().filter(pair -> pair.size() == 2).toList();
            long changed = complete.stream().filter(pair -> pair.get(0).winner() != pair.get(1).winner()).count();
            out.append("| ").append(key).append(" | ").append(complete.size()).append(" | ").append(changed).append(" | ")
                    .append(percent(complete.isEmpty() ? 0 : changed / (double) complete.size())).append(" | ")
                    .append(meanRounds(rows, true)).append(" | ").append(meanRounds(rows, false)).append(" |\n");
        });
    }

    private static void appendMaps(StringBuilder out, List<StrengthBenchmark.MapFacts> maps) {
        out.append("\n## Karten\n\nReiserunden zum naechsten Nachbarn je System und von der Heimat der Referenz zur naechsten gegnerischen Heimat.\n\n")
                .append("| Systeme | Spieler | Karten | Nachbar 2 | Nachbar 3 | Nachbar 4 | Nachbar 5+ | Nachbar Median | Gegner Median | Gegner min | Gegner max |\n")
                .append("|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        var grouped = new TreeMap<Integer, List<StrengthBenchmark.MapFacts>>();
        maps.forEach(map -> grouped.computeIfAbsent(map.systems() * 100 + map.players(), _ -> new ArrayList<>()).add(map));
        grouped.forEach((key, rows) -> {
            var neighbours = rows.stream().flatMap(map -> map.neighbourRounds().stream()).mapToInt(Integer::intValue).sorted().toArray();
            var enemies = rows.stream().mapToInt(StrengthBenchmark.MapFacts::nearestEnemyRounds).sorted().toArray();
            out.append("| ").append(key / 100).append(" | ").append(key % 100).append(" | ").append(rows.size());
            for (int rounds = 2; rounds <= 5; rounds++) {
                int bucket = rounds;
                long count = java.util.Arrays.stream(neighbours).filter(value -> bucket == 5 ? value >= 5 : value == bucket).count();
                out.append(" | ").append(percent(count / (double) neighbours.length));
            }
            out.append(" | ").append(quantile(neighbours, 0.5)).append(" | ").append(quantile(enemies, 0.5)).append(" | ")
                    .append(enemies[0]).append(" | ").append(enemies[enemies.length - 1]).append(" |\n");
        });
    }

    private static <K extends Comparable<K>> Map<K, List<StrengthBenchmark.Match>> group(List<StrengthBenchmark.Match> matches,
                                                                                       Function<StrengthBenchmark.Match, K> key) {
        var groups = new TreeMap<K, List<StrengthBenchmark.Match>>();
        matches.forEach(match -> groups.computeIfAbsent(key.apply(match), _ -> new ArrayList<>()).add(match));
        return groups;
    }

    private static String meanRounds(List<StrengthBenchmark.Match> rows, boolean events) {
        var decided = rows.stream().filter(row -> row.events() == events && row.decided()).mapToInt(StrengthBenchmark.Match::rounds);
        var mean = decided.average();
        return mean.isEmpty() ? "-" : String.format(Locale.ROOT, "%.1f", mean.getAsDouble());
    }

    private static String profiles(StrengthBenchmark.Match match) {
        return match.computers().stream().map(Enum::name).sorted().collect(Collectors.joining("/"));
    }

    static String quantile(int[] sorted, double share) {
        if (sorted.length == 0) { return "-"; }
        return Integer.toString(sorted[Math.min(sorted.length - 1, (int) Math.floor(share * sorted.length))]);
    }

    /** Wilson score interval for a share at 95 %. */
    static String wilson(long successes, long trials) {
        if (trials == 0) { return "-"; }
        double z = 1.96;
        double p = successes / (double) trials;
        double denominator = 1 + z * z / trials;
        double centre = (p + z * z / (2 * trials)) / denominator;
        double half = z * Math.sqrt(p * (1 - p) / trials + z * z / (4.0 * trials * trials)) / denominator;
        return percent(Math.max(0, centre - half)) + " .. " + percent(Math.min(1, centre + half));
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value * 100);
    }
}
