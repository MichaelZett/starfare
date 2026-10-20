package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.game.values.GalaxyLayout;
import de.zettsystems.starfare.game.values.RulesetRef;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/** Test-only cost and conquest interventions; never registered as playable rules. */
final class InvestmentBenchmark {
    private InvestmentBenchmark() { }
    static void main(String[] args) throws IOException {
        var values = new HashMap<String, String>();
        for (String arg : args) {
            var pair = arg.split("=", 2);
            if (pair.length != 2 || !Set.of("--mode", "--output", "--model", "--first-seed", "--seeds", "--rounds", "--combat-seeds").contains(pair[0])
                    || values.putIfAbsent(pair[0], pair[1]) != null) { throw new IllegalArgumentException("Invalid investment option: " + arg); }
        }
        var model = InvestmentModel.valueOf(values.getOrDefault("--model", "BASELINE"));
        var output = Path.of(values.getOrDefault("--output", "build/investment/" + model));
        long first = Long.parseLong(values.getOrDefault("--first-seed", "7101"));
        int seeds = Integer.parseInt(values.getOrDefault("--seeds", "1"));
        int combats = Integer.parseInt(values.getOrDefault("--combat-seeds", "2"));
        int rounds = Integer.parseInt(values.getOrDefault("--rounds", "150"));
        if (seeds < 1 || seeds > 10 || combats < 1 || combats > 3 || rounds < 1 || rounds > 300) {
            throw new IllegalArgumentException("Invalid investment dimensions");
        }
        Files.createDirectories(output);
        Files.writeString(output.resolve("invocation.txt"), "investment-v1\nsource-sha256=" + SourceFingerprint.current()
                + "\njava=" + System.getProperty("java.version") + "\n" + String.join(" ", args) + "\n");
        var matches = new StringBuilder("id,model,policy,opponent,investor,seed,combat,layout,size,outcome,winner,rounds,invested,grown,acquired,first_investment,first_completion,sha256\n");
        Files.writeString(output.resolve("rounds.csv"), "id,round,player,systems,capacity,ships,expansion,built,stock\n");
        int id = 0;
        for (long seed = first; seed < first + seeds; seed++) {
            for (int combat = 0; combat < combats; combat++) {
                for (var layout : List.of(GalaxyLayout.RANDOM, GalaxyLayout.EVEN)) {
                    for (int size : List.of(16, 32)) {
                        var scenario = new Scenario(RulesetRef.SPACEWARD_EQUAL_METAL, layout, size, seed, 314159 + combat, rounds, 10);
                        var metrics = new StringBuilder();
                        for (var opponent : List.of(AiStrategy.CONCENTRATION, AiStrategy.RUSH)) {
                            for (var policy : InvestmentPolicy.values()) {
                                for (int seat = 1; seat <= 2; seat++) {
                                    var result = InvestmentMatch.play(scenario, model, policy, opponent, seat, ++id, metrics);
                                    matches.append(id).append(',').append(model).append(',').append(policy).append(',').append(opponent)
                                            .append(',').append(seat).append(',').append(seed).append(',').append(combat).append(',').append(layout)
                                            .append(',').append(size).append(',').append(result.outcome()).append(',').append(result.winner())
                                            .append(',').append(result.rounds()).append(',').append(result.invested()).append(',').append(result.grown())
                                            .append(',').append(result.acquired()).append(',').append(result.firstInvestment()).append(',').append(result.firstCompletion())
                                            .append(',').append(result.digest()).append('\n');
                                }
                            }
                        }
                        Files.writeString(output.resolve("rounds.csv"), metrics, StandardOpenOption.APPEND);
                        Files.writeString(output.resolve("matches.csv"), matches);
                        System.out.printf("Investment %s %d %s map=%d combat=%d: %d complete%n", model, size, layout, seed, combat, id);
                    }
                }
            }
        }
        Files.writeString(output.resolve("complete.txt"), "matches=" + id + "\n");
    }
}
