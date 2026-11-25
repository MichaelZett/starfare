package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.game.values.ComputerStrength;
import de.zettsystems.starfare.game.values.GalaxyLayout;
import de.zettsystems.starfare.game.values.GalaxySize;
import de.zettsystems.starfare.game.values.RandomnessLevel;
import de.zettsystems.starfare.game.values.RulesetRef;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Function;

/**
 * Dimensions of a computer-strength calibration on lobby galaxies: one reference profile stands in for the person on
 * seat 1, the computers play as the chosen strength would let them.
 *
 * @param rules            classic or extended lobby games
 * @param startShare       replaces the start share of every strength when present, to try other values
 * @param computerProfiles replaces the profiles of the computers when not empty; each draws one per map
 * @param events           with {@code false}, all cosmic events are switched off while the randomness level keeps
 *                         its combat deviation and attack order; both values pair events against none
 */
record StrengthOptions(Path output, RulesetRef rules, List<ComputerStrength> strengths, List<RandomnessLevel> levels, List<Boolean> events,
                       List<Integer> sizes, List<Integer> players, GalaxyLayout layout, long firstSeed, int seeds,
                       long combatSeed, int combatSeeds, int rounds, Strategy reference, OptionalDouble startShare,
                       List<Strategy> computerProfiles, int threads) {
    private static final Set<String> KEYS = Set.of("mode", "output", "strengths", "randomness", "events", "systems",
            "players", "layout", "first-seed", "seeds", "combat-seed", "combat-seeds", "rounds", "reference",
            "start-share", "computer-profiles", "threads", "rules");

    StrengthOptions {
        strengths = List.copyOf(strengths);
        levels = List.copyOf(levels);
        events = List.copyOf(events);
        sizes = List.copyOf(sizes);
        players = List.copyOf(players);
        computerProfiles = List.copyOf(computerProfiles);
    }

    static StrengthOptions parse(String[] args) {
        Map<String, String> values = new HashMap<>();
        for (String argument : args) {
            String[] pair = argument.split("=", 2);
            if (pair.length != 2 || !pair[0].startsWith("--") || !KEYS.contains(pair[0].substring(2))
                    || values.putIfAbsent(pair[0].substring(2), pair[1]) != null) {
                throw new IllegalArgumentException("Unknown, duplicate or malformed strength option: " + argument);
            }
        }
        var options = new StrengthOptions(Path.of(values.getOrDefault("output", "build/strategy-harness/strength")),
                rules(values.getOrDefault("rules", "classic")),
                list(values, "strengths", "EASY,NORMAL,HARD", ComputerStrength::valueOf),
                list(values, "randomness", RandomnessLevel.DEFAULT.name(), RandomnessLevel::valueOf),
                list(values, "events", "on", StrengthOptions::switchedOn),
                list(values, "systems", Integer.toString(GalaxySize.DEFAULT.systems()), Integer::parseInt),
                list(values, "players", "2", Integer::parseInt),
                GalaxyLayout.valueOf(values.getOrDefault("layout", GalaxyLayout.EVEN.name())),
                Long.parseLong(values.getOrDefault("first-seed", "1")), number(values, "seeds", 10),
                Long.parseLong(values.getOrDefault("combat-seed", "314159")), number(values, "combat-seeds", 1),
                number(values, "rounds", 300), Strategy.valueOf(values.getOrDefault("reference", Strategy.BASELINE.name())),
                values.containsKey("start-share") ? OptionalDouble.of(Double.parseDouble(values.get("start-share"))) : OptionalDouble.empty(),
                values.containsKey("computer-profiles") ? list(values, "computer-profiles", "", Strategy::valueOf) : List.of(),
                number(values, "threads", 1));
        options.validate();
        return options;
    }

    private void validate() {
        // Extended games also offer the industry profiles the computers of the game play.
        var offered = rules.isSpaceward() ? List.of(Strategy.values()) : Strategy.forRules(rules);
        boolean profilesFit = offered.containsAll(computerProfiles) && offered.contains(reference);
        boolean shareValid = startShare.isEmpty() || (startShare.getAsDouble() >= 0.25 && startShare.getAsDouble() <= 3);
        if (seeds < 1 || seeds > 1000 || combatSeeds < 1 || combatSeeds > 20 || rounds < 1 || rounds > 2000
                || threads < 1 || threads > 8 || !profilesFit || !shareValid
                || sizes.stream().anyMatch(size -> GalaxySize.of(size).isEmpty())
                || players.stream().anyMatch(count -> count < 2 || count > 10)
                || sizes.stream().anyMatch(size -> players.stream().anyMatch(count -> !GalaxySize.of(size).orElseThrow().fits(count)))) {
            throw new IllegalArgumentException("Strength options: profiles of the rules, offered galaxy sizes that fit 2..10 players, "
                    + "1..1000 seeds, 1..20 combat seeds, start share 0.25..3, 1..8 threads");
        }
    }

    private static RulesetRef rules(String value) {
        return switch (value) {
            case "classic" -> RulesetRef.SECTOR_FORCES;
            case "spaceward" -> RulesetRef.EXTENDED;
            default -> throw new IllegalArgumentException("Rules: classic, spaceward");
        };
    }

    private static boolean switchedOn(String value) {
        return switch (value) {
            case "on" -> true;
            case "off" -> false;
            default -> throw new IllegalArgumentException("Events: on, off");
        };
    }

    private static <T> List<T> list(Map<String, String> values, String key, String fallback, Function<String, T> parse) {
        var parsed = Arrays.stream(values.getOrDefault(key, fallback).split(",")).map(parse).toList();
        if (parsed.isEmpty() || parsed.stream().distinct().count() != parsed.size()) {
            throw new IllegalArgumentException("Empty or duplicate values for --" + key);
        }
        return parsed;
    }

    private static int number(Map<String, String> values, String key, int fallback) {
        return Integer.parseInt(values.getOrDefault(key, Integer.toString(fallback)));
    }
}
