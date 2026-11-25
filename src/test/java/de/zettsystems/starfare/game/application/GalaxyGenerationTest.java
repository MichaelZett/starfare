package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.values.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GalaxyGenerationTest extends AbstractIntegrationTest {

    private static final int SYSTEMS = 24;
    private static final int MIN_PROD = 1;
    private static final int MAX_PROD = 6;

    private GameSetup setup(GalaxyLayout layout) {
        return GameSetup.defaults().seatPlayers(2, 2).chooseGalaxy(SYSTEMS, layout).chooseMaxProduction(MAX_PROD)
                .normalized();
    }

    private List<StarSystem> systemsOf(GameSetup setup) {
        GameId id = registry.createGame(setup);
        return registry.readState(id, state -> List.copyOf(state.systems()));
    }

    @Test
    void classicGamesBalanceHomeGarrisonsWithinBounds() {
        GameSetup balanced = setup(GalaxyLayout.RANDOM);

        boolean differs = false;
        for (int attempt = 0; attempt < 3 && !differs; attempt++) {
            GameId id = registry.createGame(balanced);
            List<StarSystem> homes = registry.readState(id, state -> {
                assertThat(state.cosmic().homeSystems()).hasSize(4);
                return homesOf(List.copyOf(state.systems()));
            });
            assertThat(homes).extracting(StarSystem::garrison).allSatisfy(garrison -> assertThat(garrison).isBetween(4, 16));
            differs = homes.stream().map(StarSystem::garrison).distinct().count() > 1;
        }
        assertThat(differs).as("positions differ, so garrisons should too").isTrue();
    }

    /** Kleinster Abstand zwischen zwei Systemen der Menge. */
    private static double closestPair(List<StarSystem> systems) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i < systems.size(); i++) {
            for (int j = i + 1; j < systems.size(); j++) {
                best = Math.min(best, Math.hypot(systems.get(i).x() - systems.get(j).x(),
                        systems.get(i).y() - systems.get(j).y()));
            }
        }
        return best;
    }

    private static List<StarSystem> homesOf(List<StarSystem> systems) {
        return systems.stream().filter(s -> s.ownerId() != null).toList();
    }

    @Test
    void neutralProductionStaysBetweenOneAndTheChosenHighest() {
        List<StarSystem> systems = systemsOf(setup(GalaxyLayout.RANDOM));

        assertThat(systems).hasSize(SYSTEMS);
        assertThat(systems.stream().filter(s -> s.ownerId() == null))
                .allSatisfy(s -> assertThat(s.productionPerTurn()).isBetween(MIN_PROD, MAX_PROD));
    }

    @Test
    void theComputerStrengthScalesTheComputersStartProduction() {
        for (ComputerStrength strength : ComputerStrength.values()) {
            GameId id = registry.createGame(setup(GalaxyLayout.EVEN).chooseComputerStrength(strength));
            List<Integer> computerHomes = registry.readState(id, state -> state.players().stream().filter(Player::ai)
                    .map(player -> state.systems().stream().filter(s -> Integer.valueOf(player.id()).equals(s.ownerId()))
                            .findFirst().orElseThrow().productionPerTurn()).toList());
            List<Integer> humanHomes = registry.readState(id, state -> state.players().stream().filter(p -> !p.ai())
                    .map(player -> state.systems().stream().filter(s -> Integer.valueOf(player.id()).equals(s.ownerId()))
                            .findFirst().orElseThrow().productionPerTurn()).toList());
            assertThat(humanHomes).containsOnly(GameConfig.START_PRODUCTION);
            assertThat(computerHomes).as("%s", strength).containsOnly(strength.computerStart(RulesetRef.SECTOR_FORCES, GameConfig.START_PRODUCTION));
        }
    }

    @Test
    void gaussianSpreadFavoursTheMiddleOfTheRange() {
        int middle = 0;
        int edges = 0;
        // Ueber mehrere Partien, damit einzelne Ausreisser den Test nicht kippen.
        for (int run = 0; run < 12; run++) {
            for (StarSystem s : systemsOf(setup(GalaxyLayout.RANDOM))) {
                if (s.ownerId() != null) {
                    continue;
                }
                if (s.productionPerTurn() == MIN_PROD || s.productionPerTurn() == MAX_PROD) {
                    edges++;
                } else {
                    middle++;
                }
            }
        }
        assertThat(middle).as("Mittelwerte sollen die Raender klar ueberwiegen").isGreaterThan(edges * 2);
    }

    @Test
    void evenLayoutSpreadsSystemsFurtherApartThanRandom() {
        double even = 0;
        double random = 0;
        for (int run = 0; run < 8; run++) {
            even += closestPair(systemsOf(setup(GalaxyLayout.EVEN)));
            random += closestPair(systemsOf(setup(GalaxyLayout.RANDOM)));
        }

        assertThat(even).as("gleichmaessiges Layout haelt groesseren Mindestabstand").isGreaterThan(random);
    }

    @Test
    void homeSystemsRemainSeparatedForEveryLayout() {
        for (GalaxyLayout layout : GalaxyLayout.values()) {
            for (int run = 0; run < 8; run++) {
                double closestHomes = closestPair(homesOf(systemsOf(setup(layout))));
                assertThat(closestHomes).as("%s: Heimatsysteme sollen nicht gehäuft starten", layout)
                        .isGreaterThan(GalaxyExtent.forSystems(SYSTEMS).height() * 0.15);
            }
        }
    }

    @Test
    void everyPlayerGetsExactlyOneHomeSystem() {
        GameSetup setup = setup(GalaxyLayout.EVEN);
        GameId id = registry.createGame(setup);

        int players = registry.readState(id, state -> state.players().size());
        List<StarSystem> homes = registry.readState(id,
                state -> state.systems().stream().filter(s -> s.ownerId() != null).toList());

        assertThat(homes).hasSize(players);
        assertThat(homes.stream().map(StarSystem::ownerId)).doesNotHaveDuplicates();
    }

    @Test
    void nullChoicesFallBackToDefaults() {
        GameSetup normalized = new GameSetup(SYSTEMS, 1, 1, 0, null, null, null, null, null, null, 0, false)
                .normalized();

        assertThat(normalized.galaxyLayout()).isEqualTo(GalaxyLayout.EVEN);
        assertThat(normalized.maxProduction()).isEqualTo(GameConfig.DEFAULT_MAX_PRODUCTION);
        assertThat(normalized.ruleset()).isEqualTo(RulesetRef.SECTOR_FORCES);
    }

    @Test
    void systemsKeepClearOfTheMapEdges() {
        for (GalaxyLayout layout : GalaxyLayout.values()) {
            for (StarSystem s : systemsOf(setup(layout))) {
                assertThat(s.x()).as("%s: x von %s", layout, s.name())
                        .isBetween(GameConfig.SYSTEM_MARGIN, GalaxyExtent.forSystems(SYSTEMS).width() - GameConfig.SYSTEM_MARGIN);
                assertThat(s.y()).as("%s: y von %s", layout, s.name())
                        .isBetween(GameConfig.SYSTEM_MARGIN, GalaxyExtent.forSystems(SYSTEMS).height() - GameConfig.SYSTEM_MARGIN);
            }
        }
    }
}
