package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.combat.application.DefaultCoalitionCombatService;
import de.zettsystems.starfare.combat.application.DefaultCombatService;
import de.zettsystems.starfare.diplomacy.application.DefaultDiplomacyService;
import de.zettsystems.starfare.economy.application.DefaultEconomyService;
import de.zettsystems.starfare.economy.domain.RecyclingOrders;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.application.DefaultPlayerViewBuilder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GalaxyLayout;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.navigation.application.DefaultNavigationService;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.turn.application.RecyclingSpacewardTurnEngine;
import de.zettsystems.starfare.turn.application.DirectEconomySpacewardTurnEngine;
import de.zettsystems.starfare.turn.application.EconomyPreviewSpacewardTurnEngine;
import de.zettsystems.starfare.turn.application.EqualMetalSpacewardTurnEngine;
import de.zettsystems.starfare.turn.application.SpacewardTurnEngine;
import de.zettsystems.starfare.turn.application.RoundPipeline;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Silent versioned experiments using real AI, commands, resolution and conservation checks. */
final class EconomyBenchmark {
    record Result(String outcome, String winners, int rounds, String digest, long combatLoss, long recyclingLoss, long initialMetal) { }
    private EconomyBenchmark() { }

    public static void main(String[] args) throws IOException {
        var options = EconomyBenchmarkOptions.parse(args);
        Files.createDirectories(options.output());
        Files.writeString(options.output().resolve("invocation.txt"), "economy-harness-v1\nsource-sha256=" + SourceFingerprint.current()
                + "\njava=" + System.getProperty("java.version") + "\n" + String.join(" ", args) + "\nrules=" + options.rules() + "\n");
        var matches = new StringBuilder("match,suite,map_seed,combat_seed,layout,systems,environment,profiles,outcome,winners,rounds,sha256,combat_loss_tenths,recycling_loss_tenths,initial_metal_tenths\n");
        var metrics = new StringBuilder("match,round,player,profile,systems,ships,capacity,unused_labor_capacity,stock,deposit_remaining,mining,extracted,planned,built,shortage,recycled,combat_loss_tenths,recycling_loss_tenths,balance_tenths\n");
        var orders = new StringBuilder("match,round,player,action,source,target,amount\n");
        Files.writeString(options.output().resolve("rounds.csv"), "");
        Files.writeString(options.output().resolve("orders.csv"), "");
        int match = 0;
        for (long seed = options.firstSeed(); seed < options.firstSeed() + options.seeds(); seed++) {
            for (int combat = 0; combat < options.combatSeeds(); combat++) {
                for (var layout : List.of(GalaxyLayout.RANDOM, GalaxyLayout.EVEN)) {
                    for (int size : List.of(16, 32)) {
                        for (var environment : options.environments()) {
                            for (var profiles : lineups(options.suite())) {
                                var scenario = new Scenario(options.rules(), layout, size, seed, 314159L + combat, options.rounds(), 10);
                                int id = ++match;
                                var result = play(scenario, environment, profiles, id, metrics, orders);
                                matches.append(id).append(',').append(options.suite()).append(',').append(seed).append(',').append(scenario.combatSeed())
                                        .append(',').append(layout).append(',').append(size).append(',').append(environment).append(',')
                                        .append(profiles.stream().map(Object::toString).collect(java.util.stream.Collectors.joining("/")))
                                        .append(',').append(result.outcome()).append(',').append(result.winners()).append(',').append(result.rounds())
                                        .append(',').append(result.digest()).append(',').append(result.combatLoss()).append(',').append(result.recyclingLoss())
                                        .append(',').append(result.initialMetal()).append('\n');
                            }
                            Files.writeString(options.output().resolve("rounds.csv"), metrics, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                            Files.writeString(options.output().resolve("orders.csv"), orders, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                            metrics.setLength(0);
                            orders.setLength(0);
                            System.out.printf("Economy %s %d %s: %d matches completed%n", layout, size, environment, match);
                        }
                    }
                }
                System.out.printf("Economy map=%d combat=%d: %d matches completed%n", seed, combat, match);
            }
        }
        Files.writeString(options.output().resolve("matches.csv"), matches);
        System.out.println("Economy reports: " + options.output().toAbsolutePath());
    }

    static List<List<EconomyProfiles.Profile>> lineups(String suite) {
        var result = new ArrayList<List<EconomyProfiles.Profile>>();
        var standard = new EconomyProfiles.Profile(AiStrategy.CONCENTRATION, EconomyProfiles.Policy.STANDARD);
        if (suite.equals("strategies")) { return strategyLineups(); }
        if (suite.equals("profiles")) {
            for (var strategy : AiStrategy.values()) {
                var profile = new EconomyProfiles.Profile(strategy, EconomyProfiles.Policy.STANDARD);
                result.add(List.of(profile, standard));
                result.add(List.of(standard, profile));
            }
        } else {
            var profiles = new ArrayList<EconomyProfiles.Profile>();
            profiles.add(standard);
            for (var policy : List.of(EconomyProfiles.Policy.MINING, EconomyProfiles.Policy.RECYCLING, EconomyProfiles.Policy.ADAPTIVE)) {
                profiles.add(new EconomyProfiles.Profile(AiStrategy.CONCENTRATION, policy));
            }
            for (int left = 0; left < profiles.size(); left++) {
                for (int right = left + 1; right < profiles.size(); right++) {
                    result.add(List.of(profiles.get(left), profiles.get(right)));
                    result.add(List.of(profiles.get(right), profiles.get(left)));
                }
            }
            for (int rotation = 0; rotation < profiles.size(); rotation++) {
                result.add(List.copyOf(profiles));
                Collections.rotate(profiles, 1);
            }
        }
        return List.copyOf(result);
    }

    /** Every selectable pair in both seats, plus two rotating four-player groups. */
    private static List<List<EconomyProfiles.Profile>> strategyLineups() {
        var profiles = Arrays.stream(AiStrategy.values()).map(strategy ->
                new EconomyProfiles.Profile(strategy, EconomyProfiles.Policy.STANDARD)).toList();
        var result = new ArrayList<List<EconomyProfiles.Profile>>();
        for (int left = 0; left < profiles.size(); left++) {
            for (int right = left + 1; right < profiles.size(); right++) {
                result.add(List.of(profiles.get(left), profiles.get(right)));
                result.add(List.of(profiles.get(right), profiles.get(left)));
            }
        }
        for (var group : List.of(List.of(AiStrategy.RUSH, AiStrategy.CONCENTRATION, AiStrategy.INDUSTRY, AiStrategy.INDUSTRY_ADAPTIVE),
                List.of(AiStrategy.BASELINE, AiStrategy.EXPANSION, AiStrategy.DEFENSE, AiStrategy.INDUSTRY_LIGHT))) {
            var seats = new ArrayList<>(group.stream().map(strategy -> new EconomyProfiles.Profile(strategy, EconomyProfiles.Policy.STANDARD)).toList());
            for (int rotation = 0; rotation < seats.size(); rotation++) {
                result.add(List.copyOf(seats));
                Collections.rotate(seats, 1);
            }
        }
        return List.copyOf(result);
    }

    static Result play(Scenario scenario, EconomyMaps.Environment environment, List<EconomyProfiles.Profile> profiles,
                       int match, StringBuilder metrics, StringBuilder orders) {
        var state = scenario.create(profiles.size(), environment);
        var combat = new Random();
        var ordering = new Random();
        var reports = new DefaultReportService();
        var fleets = new DefaultFleetService();
        var navigation = new DefaultNavigationService();
        var pipeline = new RoundPipeline(new DefaultCombatService(reports, combat::nextDouble), reports, fleets, navigation,
                new DefaultDiplomacyService(), new DefaultCoalitionCombatService(reports, navigation, _ -> combat.nextDouble()), () -> ordering);
        var engine = engine(scenario.rules(), pipeline, fleets, reports);
        var planning = new EconomyProfiles();
        var digest = digest();
        int resolved = 0;
        while (!state.gameOver() && resolved < scenario.roundLimit()) {
            planning.plan(state, profiles);
            EconomyProfiles.validate(state);
            recordMetrics(state, profiles, match, metrics, orders);
            combat.setSeed(SimulationEngine.mix(scenario.combatSeed(), state.turn()));
            ordering.setSeed(SimulationEngine.mix(scenario.combatSeed() ^ 0x6a09e667f3bcc909L, state.turn()));
            engine.advanceTurn(state);
            long balance = RecyclingOrders.balanceTenths(state);
            if (balance != Objects.requireNonNull(state.metals().orElseThrow().recycling()).initialTenths()) {
                throw new IllegalStateException("Metal conservation failed in round " + state.turn());
            }
            digest.update(canonical(state).getBytes(StandardCharsets.UTF_8));
            state.replayFrames().clear();
            resolved++;
            // Restoration is an executable check, including reservations and exact fractions.
            if (resolved % 25 == 0) { state = GameState.fromSnapshot(GameState.toSnapshot(state)); }
        }
        String winners = state.outcome().winnerIds().stream().sorted().map(String::valueOf).collect(java.util.stream.Collectors.joining("/"));
        String outcome = "ROUND_LIMIT";
        if (state.gameOver()) { outcome = winners.isEmpty() ? "DRAW" : "WIN"; }
        var recycle = Objects.requireNonNull(state.metals().orElseThrow().recycling());
        return new Result(outcome, winners, resolved, HexFormat.of().formatHex(digest.digest()),
                recycle.combatLossTenths(), recycle.recyclingLossTenths(), recycle.initialTenths());
    }

    private static SpacewardTurnEngine engine(RulesetRef rules, RoundPipeline pipeline, DefaultFleetService fleets, DefaultReportService reports) {
        var economy = new DefaultEconomyService();
        if (rules.equals(RulesetRef.SPACEWARD_SHIP_RESEARCH)) { return new de.zettsystems.starfare.turn.application.ShipResearchSpacewardTurnEngine(pipeline, economy, (_, _) -> { }, fleets, reports); }
        if (rules.equals(RulesetRef.SPACEWARD_EARLY_INDUSTRY)) { return new de.zettsystems.starfare.turn.application.EarlyIndustrySpacewardTurnEngine(pipeline, economy, (_, _) -> { }, fleets, reports); }
        if (rules.equals(RulesetRef.SPACEWARD_EQUAL_METAL)) { return new EqualMetalSpacewardTurnEngine(pipeline, economy, (_, _) -> { }, fleets, reports); }
        if (rules.equals(RulesetRef.SPACEWARD_DIRECT_ECONOMY)) { return new DirectEconomySpacewardTurnEngine(pipeline, economy, (_, _) -> { }, fleets, reports); }
        if (rules.equals(RulesetRef.SPACEWARD_ECONOMY_PREVIEW)) { return new EconomyPreviewSpacewardTurnEngine(pipeline, economy, (_, _) -> { }, fleets, reports); }
        return new RecyclingSpacewardTurnEngine(pipeline, economy, (_, _) -> { }, fleets, reports);
    }

    private static void recordMetrics(GameState state, List<EconomyProfiles.Profile> profiles, int match, StringBuilder metrics, StringBuilder orders) {
        var metal = state.metals().orElseThrow();
        var recycling = Objects.requireNonNull(metal.recycling());
        var views = new DefaultPlayerViewBuilder();
        for (int seat = 1; seat <= profiles.size(); seat++) {
            int player = seat;
            var owned = views.forPlayer(state, seat).systems().stream().filter(system -> Objects.equals(system.ownerId(), player)).toList();
            long ships = state.systems().stream().filter(system -> Objects.equals(system.ownerId(), player)).mapToLong(StarSystem::garrison).sum()
                    + state.fleets().stream().filter(fleet -> fleet.ownerId() == player).mapToLong(Fleet::ships).sum();
            int capacity = 0;
            int unused = 0;
            int mining = 0;
            int planned = 0;
            int built = 0;
            int recycled = 0;
            long remaining = 0;
            long extracted = 0;
            for (var system : owned) {
                var industry = Objects.requireNonNull(system.industry());
                var resource = Objects.requireNonNull(industry.metal());
                capacity += industry.capacity(); unused += industry.capacity() - industry.usableCapacity();
                mining += resource.miningPoints(); remaining += resource.remaining(); extracted += resource.extraction();
                planned += resource.plannedShips(); built += resource.actualShips(); recycled += state.recyclingShips(system.id());
                order(orders, match, state.turn(), player, "EXPANSION", system.id(), 0, industry.expansionAllocation());
                order(orders, match, state.turn(), player, "MINING", system.id(), 0, resource.miningPoints());
                order(orders, match, state.turn(), player, "RECYCLING", system.id(), 0, state.recyclingShips(system.id()));
            }
            state.pendingOrders().getOrDefault(player, List.of()).forEach(order -> {
                if (order instanceof de.zettsystems.starfare.fleet.values.FleetOrder.Send send) {
                    order(orders, match, state.turn(), player, "SEND", send.fromSystemId(), send.toSystemId(), send.ships());
                }
            });
            metrics.append(match).append(',').append(state.turn()).append(',').append(player).append(',').append(profiles.get(player - 1))
                    .append(',').append(owned.size()).append(',').append(ships).append(',').append(capacity).append(',').append(unused)
                    .append(',').append(metal.stocks().get(player)).append(',').append(remaining).append(',').append(mining).append(',').append(extracted)
                    .append(',').append(planned).append(',').append(built).append(',').append(planned - built).append(',').append(recycled)
                    .append(',').append(recycling.combatLossTenths()).append(',').append(recycling.recyclingLossTenths())
                    .append(',').append(RecyclingOrders.balanceTenths(state)).append('\n');
        }
    }

    private static void order(StringBuilder out, int match, int round, int player, String action, int source, int target, int amount) {
        out.append(match).append(',').append(round).append(',').append(player).append(',').append(action)
                .append(',').append(source).append(',').append(target).append(',').append(amount).append('\n');
    }

    static String canonical(GameState state) {
        var metal = state.metals().orElseThrow();
        var recycle = Objects.requireNonNull(metal.recycling());
        return state.turn() + "|" + state.systems() + "|" + state.fleets() + "|" + new TreeMap<>(state.industries())
                + "|" + new TreeMap<>(state.colonies()) + "|" + new TreeMap<>(metal.deposits()) + "|" + new TreeMap<>(metal.stocks())
                + "|" + new TreeMap<>(metal.mining()) + "|" + new TreeMap<>(recycle.orders()) + "|" + new TreeMap<>(recycle.fractions())
                + "|" + recycle.initialTenths() + "|" + recycle.combatLossTenths() + "|" + recycle.recyclingLossTenths()
                + "|" + state.outcome().winnerIds().stream().sorted().toList() + "\n";
    }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
