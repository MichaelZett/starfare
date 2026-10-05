package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.diplomacy.application.DefaultDiplomacyService;
import de.zettsystems.starfare.diplomacy.values.DiplomacyOrder;
import de.zettsystems.starfare.game.values.RulesetRef;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Command-line entry point. All state is private to this process, all sound/UI is absent. */
public final class StrategyBenchmark {
    private StrategyBenchmark() { }

    public static void main(String[] args) throws IOException {
        var options = BenchmarkOptions.parse(args);
        Files.createDirectories(options.output());
        var results = new ArrayList<MatchResult>();
        var skipped = new ArrayList<String>();
        Files.writeString(options.output().resolve("invocation.txt"), "harness-v1\nsource-sha256=" + SourceFingerprint.current()
                + "\njava=" + System.getProperty("java.version") + "\n" + String.join(" ", args) + "\n" + options + "\n");
        for (var rules : options.rules()) {
            for (var layout : options.layouts()) {
                for (int size : options.sizes()) {
                    for (int seed = 0; seed < options.seeds().count(); seed++) {
                        for (int combat = 0; combat < options.seeds().combatCount(); combat++) {
                            var scenario = new Scenario(rules, layout, size, options.seeds().first() + seed,
                                    options.seeds().combat() + combat, options.limits().rounds(), options.limits().randomness());
                            for (boolean groupVictory : options.allianceVictory().equals("both") ? List.of(false, true)
                                    : List.of(Boolean.parseBoolean(options.allianceVictory()))) {
                                var configured = scenario.withVictoryRules(new de.zettsystems.starfare.game.values.VictoryRules(
                                        options.individualPercent(), scenario.rules().spaceward(), groupVictory, options.alliancePercent()));
                                run(options, configured, results, skipped);
                            }
                            System.out.printf("%s %s systems=%d mapSeed=%d combatSeed=%d: %d matches completed%n",
                                    rules.variant(), layout, size, scenario.mapSeed(), scenario.combatSeed(), results.size());
                        }
                    }
                }
            }
        }
        BenchmarkReport.write(options.output(), results, skipped);
        System.out.println("Reports: " + options.output().toAbsolutePath().normalize());
    }

    private static void run(BenchmarkOptions options, Scenario scenario, List<MatchResult> results, List<String> skipped) {
        var profiles = Strategy.forRules(scenario.rules());
        switch (options.mode()) {
            case "matrix" -> {
                for (int left = 0; left < profiles.size(); left++) {
                    for (int right = left + 1; right < profiles.size(); right++) {
                        results.add(MatchRunner.play(scenario, List.of(profiles.get(left), profiles.get(right))));
                        results.add(MatchRunner.play(scenario, List.of(profiles.get(right), profiles.get(left))));
                    }
                }
            }
            case "multiplayer" -> rotated(scenario, profiles, false, results);
            case "alliance" -> rotated(scenario, List.of(Strategy.RUSH, Strategy.DEFENSE, Strategy.CONCENTRATION, Strategy.INDUSTRY), true, results);
            case "decision" -> decisions(options, scenario, results, skipped);
            default -> throw new IllegalArgumentException("Unknown mode");
        }
    }

    private static void rotated(Scenario scenario, List<Strategy> profiles, boolean alliance, List<MatchResult> results) {
        var seats = new ArrayList<>(profiles);
        for (int rotation = 0; rotation < profiles.size(); rotation++) {
            var state = scenario.create(seats.size());
            if (alliance) {
                var diplomacy = new DefaultDiplomacyService();
                boolean found = diplomacy.act(state, 1, new DiplomacyOrder(DiplomacyOrder.Action.FOUND, 2, 3));
                if (!found) { throw new IllegalStateException("Alliance setup failed"); }
                int proposal = state.diplomacy().proposals().getFirst().id();
                if (!diplomacy.act(state, 2, new DiplomacyOrder(DiplomacyOrder.Action.APPROVE, proposal, 0))) {
                    throw new IllegalStateException("Alliance approval failed");
                }
            }
            results.add(MatchRunner.continueFrom(scenario, List.copyOf(seats), state,
                    alliance ? "alliance-seats-1-2-group-win-" + scenario.victoryRules().allianceVictoryAllowed() : "multiplayer", SimulationEngine.Intervention.NONE));
            Collections.rotate(seats, 1);
        }
    }

    private static void decisions(BenchmarkOptions options, Scenario scenario, List<MatchResult> results, List<String> skipped) {
        var strategies = List.of(scenario.rules().spaceward() ? Strategy.INDUSTRY : Strategy.CONCENTRATION, Strategy.RUSH);
        var checkpoint = MatchRunner.checkpoint(scenario, strategies, options.limits().forkRound());
        if (checkpoint.gameOver()) { skipped.add(scenario + ": finished before fork"); return; }
        var baseline = new SimulationEngine().plan(checkpoint, strategies).getFirst();
        for (var intervention : SimulationEngine.Intervention.values()) {
            if (scenario.rules().equals(RulesetRef.SECTOR_FORCES)
                    && (intervention == SimulationEngine.Intervention.EXPAND_FIRST || intervention == SimulationEngine.Intervention.SHIPS_FIRST)) { continue; }
            if (SimulationEngine.intervene(checkpoint, baseline, intervention).equals(baseline)
                    && intervention != SimulationEngine.Intervention.NONE) {
                skipped.add(scenario + ": " + intervention + " has no effective action at fork"); continue;
            }
            results.add(MatchRunner.continueFrom(scenario, strategies, checkpoint, "fork-" + checkpoint.turn() + "-" + intervention, intervention));
        }
    }
}
