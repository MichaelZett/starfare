package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.game.values.RulesetRef;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/** Bounded, explicit dimensions for reproducible economy experiments. */
record EconomyBenchmarkOptions(Path output, long firstSeed, int seeds, int combatSeeds, int rounds, String suite,
               RulesetRef rules, List<EconomyMaps.Environment> environments) {
    static EconomyBenchmarkOptions parse(String[] args) {
        var values = new HashMap<String, String>();
        for (String arg : args) {
            String[] pair = arg.split("=", 2);
            if (pair.length != 2 || !Set.of("--mode", "--output", "--first-seed", "--seeds", "--combat-seeds", "--rounds", "--suite", "--rules", "--environments").contains(pair[0])
                    || values.putIfAbsent(pair[0], pair[1]) != null) { throw new IllegalArgumentException("Invalid economy option: " + arg); }
        }
        var options = new EconomyBenchmarkOptions(Path.of(values.getOrDefault("--output", "build/economy")),
                Long.parseLong(values.getOrDefault("--first-seed", "1")), Integer.parseInt(values.getOrDefault("--seeds", "2")),
                Integer.parseInt(values.getOrDefault("--combat-seeds", "2")), Integer.parseInt(values.getOrDefault("--rounds", "150")),
                values.getOrDefault("--suite", "comparison"), parseRules(values.getOrDefault("--rules", "w3")),
                Arrays.stream(values.getOrDefault("--environments", "NATURAL,POOR,RICH,HARSH,FERTILE,EXHAUSTED").split(","))
                        .map(EconomyMaps.Environment::valueOf).distinct().toList());
        if (options.seeds < 1 || options.seeds > 100 || options.combatSeeds < 1 || options.combatSeeds > 10
                || options.rounds < 1 || options.rounds > 2000 || !Set.of("comparison", "profiles", "strategies").contains(options.suite)
                || options.environments.isEmpty()) {
            throw new IllegalArgumentException("Invalid economy dimensions");
        }
        return options;
    }
    private static RulesetRef parseRules(String value) {
        return switch (value) {
            case "w3" -> RulesetRef.SPACEWARD_RECYCLING;
            case "w4" -> RulesetRef.SPACEWARD_ECONOMY_PREVIEW;
            case "w5" -> RulesetRef.SPACEWARD_DIRECT_ECONOMY;
            case "r1" -> RulesetRef.SPACEWARD_SHIP_RESEARCH;
            case "w7" -> RulesetRef.SPACEWARD_EARLY_INDUSTRY;
            case "w6" -> RulesetRef.SPACEWARD_EQUAL_METAL;
            default -> throw new IllegalArgumentException("Expected economy rules w3, w4, w5, w6, w7 or r1");
        };
    }
}
