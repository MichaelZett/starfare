package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.game.domain.GalaxyPlacement;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Synthetic, seeded maps using the production position generator. No registry or database. */
record Scenario(RulesetRef rules, GalaxyLayout layout, int systems, long mapSeed,
                long combatSeed, int roundLimit, int randomness, VictoryRules victoryRules) {
    Scenario(RulesetRef rules, GalaxyLayout layout, int systems, long mapSeed, long combatSeed, int roundLimit, int randomness) {
        this(rules, layout, systems, mapSeed, combatSeed, roundLimit, randomness, VictoryRules.defaults(rules, 70));
    }
    Scenario withVictoryRules(VictoryRules chosen) {
        return new Scenario(rules, layout, systems, mapSeed, combatSeed, roundLimit, randomness, chosen.forRuleset(rules));
    }
    Scenario {
        if (!List.of(RulesetRef.SECTOR_FORCES, RulesetRef.SPACEWARD, RulesetRef.SPACEWARD_ALLIANCE).contains(rules)
                || systems < 8 || systems > 120 || roundLimit < 1 || roundLimit > 2000
                || randomness < 0 || randomness > 30) {
            throw new IllegalArgumentException("Unsupported scenario settings");
        }
    }

    GameState create(int seats) {
        if (seats < 2 || seats > 7 || seats > systems) { throw new IllegalArgumentException("Expected 2..7 seats"); }
        var setup = new GameSetup(systems, 0, seats, Collections.nCopies(seats, 5),
                1, 10, 20, true, false, GameConfig.PLAYER_PALETTE,
                ProductionDistribution.UNIFORM, layout, false, randomness, RoundRules.defaults(), victoryRules.individualSystemPercent(), rules, victoryRules);
        var state = new GameState();
        state.resetForNewGame();
        state.rememberSetup(setup);
        state.configureLobby(true, false, false, randomness);
        var random = new Random(mapSeed);
        var positions = GalaxyPlacement.positionsFor(setup, random);
        for (int index = 0; index < systems; index++) {
            var point = positions.get(index);
            int production = 1 + random.nextInt(10);
            state.systems().add(new StarSystem(index + 1, "S" + (index + 1), point[0], point[1],
                    null, Math.max(1, production - 1), production, true));
        }
        List<StarSystem> homes = homes(state.systems(), seats, random);
        for (int seat = 1; seat <= seats; seat++) {
            state.players().add(new Player(seat, "Empire " + seat, true, GameConfig.PLAYER_PALETTE.get(seat - 1)));
            state.intel().put(seat, new java.util.HashMap<>());
            int owner = seat;
            state.updateSystem(homes.get(seat - 1).id(), system -> system.colonize(owner, 20, 5));
        }
        state.start();
        return state;
    }

    private static List<StarSystem> homes(List<StarSystem> systems, int seats, Random random) {
        var candidates = new ArrayList<>(systems);
        Collections.shuffle(candidates, random);
        var homes = new ArrayList<StarSystem>();
        homes.add(candidates.removeFirst());
        while (homes.size() < seats) {
            var next = candidates.stream().max(Comparator.comparingDouble(candidate -> homes.stream()
                    .mapToDouble(home -> Math.hypot(candidate.x() - home.x(), candidate.y() - home.y()))
                    .min().orElseThrow())).orElseThrow();
            homes.add(next);
            candidates.remove(next);
        }
        return List.copyOf(homes);
    }
}
