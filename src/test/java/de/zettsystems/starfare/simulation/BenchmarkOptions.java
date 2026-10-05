package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.game.values.GalaxyLayout;
import de.zettsystems.starfare.game.values.RulesetRef;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

record BenchmarkOptions(String mode, List<RulesetRef> rules, List<GalaxyLayout> layouts,
                        List<Integer> sizes, Seeds seeds, Limits limits, Path output, String allianceVictory, int individualPercent, int alliancePercent) {
    record Seeds(long first, long combat, int count, int combatCount) { }
    record Limits(int rounds, int forkRound, int randomness) { }
    private static final Set<String> KEYS = Set.of("mode", "rules", "layouts", "systems", "first-seed", "combat-seed",
            "seeds", "combat-seeds", "rounds", "fork-round", "randomness", "output", "alliance-victory", "individual-percent", "alliance-percent");

    static BenchmarkOptions parse(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (String argument : args) {
            String[] pair = argument.split("=", 2);
            if (pair.length != 2 || !pair[0].startsWith("--") || !KEYS.contains(pair[0].substring(2))
                    || options.putIfAbsent(pair[0].substring(2), pair[1]) != null) {
                throw new IllegalArgumentException("Unknown, duplicate or malformed argument: " + argument);
            }
        }
        String mode = options.getOrDefault("mode", "matrix");
        if (!Set.of("matrix", "multiplayer", "decision", "alliance").contains(mode)) {
            throw new IllegalArgumentException("Modes: matrix, multiplayer, decision, alliance");
        }
        var rules = Arrays.stream(options.getOrDefault("rules", "classic,spaceward").split(",")).map(value -> switch (value) {
            case "classic" -> RulesetRef.SECTOR_FORCES;
            case "spaceward" -> RulesetRef.SPACEWARD;
            case "spaceward-1.1" -> RulesetRef.SPACEWARD_ALLIANCE;
            default -> throw new IllegalArgumentException("Unknown rules: " + value);
        }).toList();
        var layouts = Arrays.stream(options.getOrDefault("layouts", "RANDOM,EVEN").split(",")).map(GalaxyLayout::valueOf).toList();
        var sizes = Arrays.stream(options.getOrDefault("systems", "16").split(",")).map(Integer::parseInt).toList();
        var seeds = new Seeds(Long.parseLong(options.getOrDefault("first-seed", "1")),
                Long.parseLong(options.getOrDefault("combat-seed", "314159")), number(options, "seeds", 2), number(options, "combat-seeds", 1));
        var limits = new Limits(number(options, "rounds", 120), number(options, "fork-round", 10), number(options, "randomness", 10));
        if (seeds.count() < 1 || seeds.count() > 1000 || seeds.combatCount() < 1 || seeds.combatCount() > 100
                || limits.forkRound() < 1 || (mode.equals("decision") && limits.forkRound() > limits.rounds())
                || rules.stream().distinct().count() != rules.size() || layouts.stream().distinct().count() != layouts.size()
                || sizes.stream().distinct().count() != sizes.size()) { throw new IllegalArgumentException("Invalid or duplicate experiment dimensions"); }
        for (int size : sizes) { new Scenario(rules.getFirst(), layouts.getFirst(), size, seeds.first(), seeds.combat(), limits.rounds(), limits.randomness()); }
        if (mode.equals("alliance") && (rules.size() != 1 || !rules.getFirst().spaceward())) {
            throw new IllegalArgumentException("Alliance mode requires --rules=spaceward");
        }
        String allianceVictory = options.getOrDefault("alliance-victory", "false");
        if (!Set.of("false", "true", "both").contains(allianceVictory)
                || (!allianceVictory.equals("false") && !rules.equals(List.of(RulesetRef.SPACEWARD_ALLIANCE)))) {
            throw new IllegalArgumentException("Group victory requires --rules=spaceward-1.1 and false, true or both");
        }
        int individual = number(options, "individual-percent", 70);
        int group = number(options, "alliance-percent", 70);
        if (individual < 10 || individual > 100 || group < 10 || group > 100) { throw new IllegalArgumentException("Victory shares: 10..100"); }
        return new BenchmarkOptions(mode, rules, layouts, sizes, seeds, limits,
                Path.of(options.getOrDefault("output", "build/strategy-harness/" + mode)), allianceVictory, individual, group);
    }

    private static int number(Map<String, String> options, String key, int fallback) {
        return Integer.parseInt(options.getOrDefault(key, Integer.toString(fallback)));
    }
}
