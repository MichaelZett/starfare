package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.game.domain.GalaxyGenesis;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.ComputerStrength;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.RandomnessLevel;
import de.zettsystems.starfare.game.values.StarSystem;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Calibrates the computer strengths on the galaxies of the lobby: the same generation, start values, start balance,
 * events and attack order as a new game under the chosen rules. Seat 1 is the person, played by a fixed reference profile.
 */
final class StrengthBenchmark {
    private StrengthBenchmark() { }

    /** One finished game; the reference sits on seat 1, so winner 1 is a win of the person. Activity covers the computers. */
    record Match(ComputerStrength strength, RandomnessLevel level, boolean events, int systems, int players,
                 long mapSeed, long combatSeed, List<Strategy> computers, String outcome, int winner, int rounds,
                 String digest, List<MatchResult.SeatActivity> activity) {
        Match {
            computers = List.copyOf(computers);
            activity = List.copyOf(activity);
        }

        boolean referenceWon() { return winner == 1; }

        boolean decided() { return winner != 0; }
    }

    /** Travel rounds of one lobby galaxy: every system to its nearest neighbour, the reference home to the nearest enemy home. */
    record MapFacts(int systems, int players, long mapSeed, List<Integer> neighbourRounds, int nearestEnemyRounds) {
        MapFacts {
            neighbourRounds = List.copyOf(neighbourRounds);
        }
    }

    static void main(String[] args) throws IOException {
        var options = StrengthOptions.parse(args);
        Files.createDirectories(options.output());
        Files.writeString(options.output().resolve("invocation.txt"), "strength-v1\nsource-sha256=" + SourceFingerprint.current()
                + "\njava=" + System.getProperty("java.version") + "\n" + String.join(" ", args) + "\n" + options + "\n");
        var maps = new ArrayList<MapFacts>();
        for (int systems : options.sizes()) {
            for (int players : options.players()) {
                for (int seed = 0; seed < options.seeds(); seed++) {
                    long mapSeed = options.firstSeed() + seed;
                    var setup = setup(options, ComputerStrength.DEFAULT, RandomnessLevel.DEFAULT, true, systems, players);
                    maps.add(facts(create(options, setup, mapSeed), mapSeed));
                }
            }
        }
        var matches = run(options);
        StrengthReport.write(options.output(), matches, maps);
        System.out.println("Reports: " + options.output().toAbsolutePath().normalize());
    }

    static List<Match> run(StrengthOptions options) {
        var tasks = new ArrayList<java.util.concurrent.Callable<Match>>();
        for (int systems : options.sizes()) {
            for (int players : options.players()) {
                for (int seed = 0; seed < options.seeds(); seed++) {
                    long mapSeed = options.firstSeed() + seed;
                    for (var strength : options.strengths()) {
                        for (var level : options.levels()) {
                            for (boolean events : options.events()) {
                                for (int combat = 0; combat < options.combatSeeds(); combat++) {
                                    long combatSeed = options.combatSeed() + combat;
                                    var setup = setup(options, strength, level, events, systems, players);
                                    tasks.add(() -> play(options, setup, mapSeed, combatSeed));
                                }
                            }
                        }
                    }
                }
            }
        }
        try (var pool = Executors.newFixedThreadPool(options.threads())) {
            var futures = new ArrayList<Future<Match>>();
            tasks.forEach(task -> futures.add(pool.submit(task)));
            var results = new ArrayList<Match>();
            int done = 0;
            for (var future : futures) {
                results.add(future.get());
                if (++done % 50 == 0 || done == futures.size()) { System.out.printf("%d / %d matches%n", done, futures.size()); }
            }
            return List.copyOf(results);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        } catch (ExecutionException failed) {
            throw new IllegalStateException(failed.getCause());
        }
    }

    /** A lobby setup: one person, the rest computers; without events the dice and attack order stay. */
    static GameSetup setup(StrengthOptions options, ComputerStrength strength, RandomnessLevel level, boolean events,
                           int systems, int players) {
        var setup = GameSetup.defaults().seatPlayers(1, players - 1).chooseGalaxy(systems, options.layout())
                .selectRuleset(options.rules()).chooseComputerStrength(strength).chooseRandomness(level);
        return (events ? setup : setup.chooseEvents(Set.of())).normalized();
    }

    static Match play(StrengthOptions options, GameSetup setup, long mapSeed, long combatSeed) {
        var state = create(options, setup, mapSeed);
        var strategies = strategies(options, state, mapSeed);
        var scenario = new Scenario(setup.ruleset(), setup.galaxyLayout(), setup.systemCount(), mapSeed, combatSeed,
                options.rounds(), setup.combatRandomnessPercent(), setup.victoryRules());
        var result = MatchRunner.playLobbyGame(scenario, strategies, state, "strength");
        return new Match(setup.computerStrength(), setup.randomness(), !setup.enabledEvents().isEmpty(),
                setup.systemCount(), setup.totalPlayers(), mapSeed, combatSeed, strategies.subList(1, strategies.size()),
                result.outcome(), result.winner(), result.rounds(), result.digest(),
                result.activity().subList(1, result.activity().size()));
    }

    static GameState create(StrengthOptions options, GameSetup setup, long mapSeed) {
        var state = new GameState();
        double share = options.startShare().orElse(setup.computerStrength().startShare(setup.ruleset()));
        GalaxyGenesis.populate(state, setup, new Random(mapSeed), share);
        state.initializeIndustry();
        state.initializeNavigation();
        state.start();
        return state;
    }

    /** The reference on seat 1; computers keep their drawn profile unless the options name others. */
    static List<Strategy> strategies(StrengthOptions options, GameState state, long mapSeed) {
        var draws = new Random(SimulationEngine.mix(mapSeed, 0x5eedL));
        var strategies = new ArrayList<Strategy>();
        for (Player player : state.players()) {
            if (!player.ai()) {
                strategies.add(options.reference());
            } else if (options.computerProfiles().isEmpty()) {
                strategies.add(Strategy.valueOf(player.aiStrategy().name()));
            } else {
                strategies.add(options.computerProfiles().get(draws.nextInt(options.computerProfiles().size())));
            }
        }
        return List.copyOf(strategies);
    }

    static MapFacts facts(GameState state, long mapSeed) {
        var neighbours = new ArrayList<Integer>();
        for (StarSystem system : state.systems()) {
            neighbours.add(state.systems().stream().filter(other -> other.id() != system.id())
                    .mapToInt(other -> state.travelRounds(system.id(), other.id())).min().orElseThrow());
        }
        int home = home(state, 1);
        int nearestEnemy = state.players().stream().filter(player -> player.id() != 1)
                .mapToInt(player -> state.travelRounds(home, home(state, player.id()))).min().orElseThrow();
        return new MapFacts(state.systems().size(), state.players().size(), mapSeed, neighbours, nearestEnemy);
    }

    private static int home(GameState state, int player) {
        return state.systems().stream().filter(system -> Objects.equals(system.ownerId(), player))
                .mapToInt(StarSystem::id).findFirst().orElseThrow();
    }
}
