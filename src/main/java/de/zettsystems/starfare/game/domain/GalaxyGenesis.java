package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.game.values.ComputerStrength;
import de.zettsystems.starfare.game.values.EmpireNameGenerator;
import de.zettsystems.starfare.game.values.GalaxyExtent;
import de.zettsystems.starfare.game.values.GameConfig;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.game.values.SystemNameGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * The opening position of a new game: seats, computer profiles, systems with their production, home systems
 * with the start of each empire and the start balance. Seeded, so the strategy bench plays the same galaxies
 * as the lobby.
 */
public final class GalaxyGenesis {
    private static final List<String> AI_NAMES = List.of("Orion", "Lyra", "Vega", "Nova", "Atlas",
            "Mira", "Sagan", "Astra", "Kepler", "Selene");

    private GalaxyGenesis() {
    }

    /** Fills a fresh game state from the setup; people take the first seats, computers the rest. */
    public static void populate(GameState state, GameSetup setup, Random random) {
        populate(state, setup, random, setup.computerStrength().startShare(setup.ruleset()));
    }

    /**
     * As {@link #populate(GameState, GameSetup, Random)}, with another start share of the computers. The strategy
     * bench calibrates the shares of {@link ComputerStrength} with it.
     */
    public static void populate(GameState state, GameSetup setup, Random random, double computerStartShare) {
        state.resetForNewGame();
        state.rememberSetup(setup);
        // Observers, re-entry and the battle presentation are always allowed; the randomness level sets the dice.
        state.configureLobby(true, true, true, setup.combatRandomnessPercent());
        state.configureVictoryRules(setup.victoryRules());
        state.configureRoundRules(setup.roundRules());
        state.players().clear();
        state.systems().clear();
        state.fleets().clear();
        state.reports().clear();
        state.intel().clear();

        // Profiles come from their own stream: every strength gets the same galaxy from the same seed.
        var profiles = new Random(random.nextLong());
        int pid = 1;
        for (int i = 1; i <= setup.humanPlayers(); i++) {
            int seatId = pid++;
            state.players().add(new Player(seatId, "P" + i, false, setup.colorForSeat(i - 1)));
            state.originalHumanPlayerIds().add(seatId);
        }
        ComputerStrength strength = setup.computerStrength();
        for (int i = 1; i <= setup.aiPlayers(); i++) {
            state.players().add(new Player(pid++, aiName(i), true, setup.colorForSeat(setup.humanPlayers() + i - 1),
                    EmpireNameGenerator.forAi(i), strength.strategy(setup.ruleset(), setup.totalPlayers(), profiles)));
        }
        for (Player p : state.players()) {
            state.intel().put(p.id(), new HashMap<>());
        }

        List<String> names = SystemNameGenerator.sample(setup.systemCount(), random);
        List<double[]> positions = GalaxyPlacement.positionsFor(setup.systemCount(), setup.galaxyLayout(),
                state.extent(), random);
        for (int i = 1; i <= setup.systemCount(); i++) {
            double[] pos = positions.get(i - 1);
            int prod = neutralProduction(setup.maxProduction(), random);
            int garrison = Math.max(1, prod - 1);
            state.systems().add(new StarSystem(i, names.get(i - 1), pos[0], pos[1], null, garrison, prod, true));
        }

        List<StarSystem> homes = new ArrayList<>(homeSystems(state, random));
        // The greedy choice makes the first home random and the last one the most cramped; the seats draw lots for
        // them, otherwise the first seat, usually the host, would start best (strategy bench, 25.11.2026).
        Collections.shuffle(homes, random);
        for (int playerSeat = 0; playerSeat < state.players().size(); playerSeat++) {
            Player p = state.players().get(playerSeat);
            StarSystem s = homes.get(playerSeat);
            int startProduction = p.ai() ? ComputerStrength.computerStart(GameConfig.START_PRODUCTION, computerStartShare)
                    : GameConfig.START_PRODUCTION;
            int startGarrison = Math.max(p.ai() ? ComputerStrength.computerStart(GameConfig.START_GARRISON, computerStartShare)
                    : GameConfig.START_GARRISON, startProduction);
            state.updateSystem(s.id(), current -> current.colonize(p.id(), startGarrison, startProduction));
        }
        List<Integer> homeIds = homes.stream().limit(state.players().size()).map(StarSystem::id).toList();
        state.updateCosmic(cosmic -> cosmic.rememberHomes(homeIds));
        spaceOut(state, GameConfig.SPACEOUT_ITERATIONS, GameConfig.SPACEOUT_MIN_DIST);
        if (setup.sectorOptions().positionalBalance()) {
            // After spacing out: the balance depends on the final travel times.
            StartBalance.apply(state, homeIds);
        }
    }

    private static String aiName(int index) {
        String name = AI_NAMES.get((index - 1) % AI_NAMES.size());
        int series = (index - 1) / AI_NAMES.size();
        return series == 0 ? name : name + " " + (series + 1);
    }

    /**
     * Bell curve from 1 to the highest production: the middle of the range is the expectation, two sigma reach the
     * edges. Outliers are drawn again instead of clamped, otherwise they would pile up on the edges.
     */
    private static int neutralProduction(int max, Random r) {
        int min = GameConfig.MIN_PRODUCTION;
        if (max <= min) {
            return min;
        }
        double mean = (min + max) / 2.0;
        double sigma = (max - min) / 4.0;
        for (int attempt = 0; attempt < 10; attempt++) {
            long value = Math.round(mean + r.nextGaussian() * sigma);
            if (value >= min && value <= max) {
                return (int) value;
            }
        }
        return (int) Math.round(mean);
    }

    private static List<StarSystem> homeSystems(GameState state, Random r) {
        var candidates = new ArrayList<>(state.systems());
        Collections.shuffle(candidates, r);
        int needed = state.players().size();
        if (needed >= candidates.size()) {
            return candidates.subList(0, Math.min(needed, candidates.size()));
        }
        // Greedy as far apart as possible: otherwise the luck of the start neighbourhood decides the game
        // before the first move.
        var chosen = new ArrayList<StarSystem>(needed);
        chosen.add(candidates.removeFirst());
        while (chosen.size() < needed) {
            StarSystem best = candidates.getFirst();
            double bestDist = -1;
            for (StarSystem candidate : candidates) {
                double nearest = chosen.stream()
                        .mapToDouble(c -> Math.hypot(c.x() - candidate.x(), c.y() - candidate.y()))
                        .min().orElse(0);
                if (nearest > bestDist) {
                    bestDist = nearest;
                    best = candidate;
                }
            }
            candidates.remove(best);
            chosen.add(best);
        }
        return chosen;
    }

    private static void spaceOut(GameState state, int iterations, double minDist) {
        GalaxyExtent extent = state.extent();
        double minX = extent.marginX();
        double maxX = extent.width() - minX;
        double minY = extent.marginY();
        double maxY = extent.height() - minY;
        for (int it = 0; it < iterations; it++) {
            for (int i = 0; i < state.systems().size(); i++) {
                for (int j = i + 1; j < state.systems().size(); j++) {
                    StarSystem a = state.systems().get(i);
                    StarSystem b = state.systems().get(j);
                    double dx = b.x() - a.x();
                    double dy = b.y() - a.y();
                    double dist = Math.hypot(dx, dy);
                    if (dist < 1e-6) {
                        dx = 1;
                        dy = 0;
                        dist = 1;
                    }
                    if (dist < minDist) {
                        double push = (minDist - dist) / 2.0;
                        double ux = dx / dist;
                        double uy = dy / dist;
                        state.systems().set(i, a.relocateTo(Math.clamp(a.x() - ux * push, minX, maxX),
                                Math.clamp(a.y() - uy * push, minY, maxY)));
                        state.systems().set(j, b.relocateTo(Math.clamp(b.x() + ux * push, minX, maxX),
                                Math.clamp(b.y() + uy * push, minY, maxY)));
                    }
                }
            }
        }
    }
}
