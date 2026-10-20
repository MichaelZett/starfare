package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.ai.application.DefaultSpacewardPlanning;
import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.combat.application.DefaultCoalitionCombatService;
import de.zettsystems.starfare.combat.application.DefaultCombatService;
import de.zettsystems.starfare.diplomacy.application.DefaultDiplomacyService;
import de.zettsystems.starfare.economy.domain.RecyclingOrders;
import de.zettsystems.starfare.game.application.DefaultPlayerViewBuilder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GalaxyLayout;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.navigation.application.DefaultNavigationService;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.turn.application.RoundPipeline;
import de.zettsystems.starfare.turn.application.ShipResearchSpacewardTurnEngine;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Random;

/** Opt-in paired research budgets through the actual AI, production, combat and save boundary. */
public final class ResearchBenchmark {
    record Result(int winner, int rounds, int level, int firstTechnology, long research, long ships,
                  double strength, int systems, String digest) { }
    private ResearchBenchmark() { }
    public static void main(String[] args) throws Exception {
        String family = args.length > 0 ? args[0] : "discovery";
        if (args.length > 1 || !java.util.Set.of("discovery", "control").contains(family)) {
            throw new IllegalArgumentException("Expected discovery or control");
        }
        boolean control = family.equals("control");
        Path output = Path.of("build/research/" + family + ".csv");
        Files.createDirectories(output.getParent());
        Files.writeString(output.resolveSibling(family + "-invocation.txt"), "research-v1\nfamily=" + family
                + "\nsource-sha256=" + SourceFingerprint.current() + "\njava=" + System.getProperty("java.version") + "\n");
        try (var writer = Files.newBufferedWriter(output)) {
            writer.write("family,map,combat,layout,size,opponent,seat,budget,winner,rounds,level,firstTechnology,research,ships,strength,systems,digest\n");
            int matches = 0;
            for (long map : control ? new long[]{19001, 29003} : new long[]{42, 137}) {
                for (long combat : new long[]{314159, 271828}) {
                    for (var layout : new GalaxyLayout[]{GalaxyLayout.EVEN, GalaxyLayout.RANDOM}) {
                        for (int size : new int[]{16, 32}) {
                            for (var opponent : new AiStrategy[]{AiStrategy.CONCENTRATION, AiStrategy.RUSH}) {
                                for (int seat : new int[]{1, 2}) {
                                    for (int budget : new int[]{0, 5, 10, 15, 25, 40}) {
                                        var scenario = new Scenario(RulesetRef.SPACEWARD_SHIP_RESEARCH, layout, size, map,
                                                combat, control ? 300 : 150, 10);
                                        var result = play(scenario, opponent, seat, budget);
                                        // Repeat complete trajectories, including technology and cohort composition.
                                        if (matches % 32 == 0 && !result.equals(play(scenario, opponent, seat, budget))) {
                                            throw new IllegalStateException("Non-deterministic research match");
                                        }
                                        writer.write(String.join(",", family, "" + map, "" + combat, layout.name(), "" + size,
                                                opponent.name(), "" + seat, "" + budget, "" + result.winner(), "" + result.rounds(),
                                                "" + result.level(), "" + result.firstTechnology(), "" + result.research(), "" + result.ships(),
                                                "" + result.strength(), "" + result.systems(), result.digest()) + "\n");
                                        writer.flush();
                                        if (++matches % 24 == 0) { System.out.println(family + ": " + matches + " matches"); }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    static Result play(Scenario scenario, AiStrategy opponent, int seat, int budget) throws Exception {
        var state = scenario.create(2);
        state.players().replaceAll(player -> new Player(player.id(), player.name(), true, player.colorHex(), player.empireName(),
                player.id() == seat ? AiStrategy.CONCENTRATION : opponent));
        var economy = new EconomyProfiles.CheckedEconomy();
        var fleets = new EconomyProfiles.CheckedFleets();
        var views = new DefaultPlayerViewBuilder();
        var planning = new DefaultSpacewardPlanning(economy, views) {
            @Override protected int researchPercent(Player player) { return player.id() == seat ? budget : 0; }
        };
        var reports = new DefaultReportService();
        var navigation = new DefaultNavigationService();
        var combat = new Random();
        var order = new Random();
        var pipeline = new RoundPipeline(new DefaultCombatService(reports, combat::nextDouble), reports, fleets, navigation,
                new DefaultDiplomacyService(), new DefaultCoalitionCombatService(reports, navigation, _ -> combat.nextDouble()), () -> order);
        var engine = new ShipResearchSpacewardTurnEngine(pipeline, economy, planning, fleets, reports);
        var digest = MessageDigest.getInstance("SHA-256");
        long invested = 0;
        int firstTechnology = 0;
        int resolved = 0;
        while (!state.gameOver() && resolved < scenario.roundLimit()) {
            // Observe the actual post-planning reservation without planning the AI twice.
            int beforeLevel = state.shipTechnology(seat);
            long beforeProgress = state.metals().orElseThrow().research().empires().get(seat).progress();
            combat.setSeed(SimulationEngine.mix(scenario.combatSeed(), state.turn()));
            order.setSeed(SimulationEngine.mix(scenario.combatSeed() ^ 0x6a09e667f3bcc909L, state.turn()));
            engine.advanceTurn(state);
            var research = state.metals().orElseThrow().research().empires().get(seat);
            long spent = research.progress() - beforeProgress;
            for (int level = beforeLevel; level < research.level(); level++) {
                spent += de.zettsystems.starfare.research.values.ShipResearch.cost(level);
            }
            invested += spent;
            resolved++;
            if (research.level() > 0 && firstTechnology == 0) { firstTechnology = resolved; }
            EconomyProfiles.validate(state);
            if (RecyclingOrders.balanceTenths(state) != Objects.requireNonNull(state.metals().orElseThrow().recycling()).initialTenths()) {
                throw new IllegalStateException("Research match violated metal conservation");
            }
            var canonical = new StringBuilder(EconomyBenchmark.canonical(state));
            state.metals().orElseThrow().research().empires().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                    .forEach(entry -> canonical.append('|').append(entry));
            state.systems().stream().sorted(java.util.Comparator.comparingInt(de.zettsystems.starfare.game.values.StarSystem::id))
                    .forEach(system -> canonical.append('|').append(system.id()).append(new java.util.TreeMap<>(system.composition().levels())));
            state.fleets().stream().sorted(java.util.Comparator.comparingInt(de.zettsystems.starfare.game.values.Fleet::globalId))
                    .forEach(fleet -> canonical.append('|').append(fleet.globalId()).append(new java.util.TreeMap<>(fleet.composition().levels())));
            digest.update(canonical.toString().getBytes(StandardCharsets.UTF_8));
            if (resolved % 25 == 0) { state = GameState.fromSnapshot(GameState.toSnapshot(state)); }
        }
        var owned = state.systems().stream().filter(system -> Integer.valueOf(seat).equals(system.ownerId())).toList();
        var flying = state.fleets().stream().filter(fleet -> fleet.ownerId() == seat).toList();
        long ships = owned.stream().mapToLong(system -> system.garrison()).sum() + flying.stream().mapToLong(fleet -> fleet.ships()).sum();
        double strength = owned.stream().mapToDouble(system -> system.composition().strength()).sum()
                + flying.stream().mapToDouble(fleet -> fleet.composition().strength()).sum();
        return new Result(state.winnerId() == null ? 0 : state.winnerId(), resolved, state.shipTechnology(seat), firstTechnology,
                invested, ships, strength, owned.size(), HexFormat.of().formatHex(digest.digest()));
    }
}
