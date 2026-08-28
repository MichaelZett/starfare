package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.values.GalaxyLayout;
import de.zettsystems.starfare.game.values.GameConfig;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.ProductionDistribution;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GalaxyGenerationTest extends AbstractIntegrationTest {

    private static final int SYSTEMS = 24;
    private static final int MIN_PROD = 2;
    private static final int MAX_PROD = 6;

    private GameSetup setup(GalaxyLayout layout, ProductionDistribution spread) {
        return new GameSetup(SYSTEMS, 2, 2, List.of(4, 4, 4, 4),
                MIN_PROD, MAX_PROD, 8, true, true, List.of(), spread, layout).normalized();
    }

    private List<StarSystem> systemsOf(GameSetup setup) {
        GameId id = registry.createGame(setup);
        return registry.readState(id, state -> List.copyOf(state.systems()));
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
    void productionStaysInsideTheConfiguredRangeForBothSpreads() {
        for (ProductionDistribution spread : ProductionDistribution.values()) {
            List<StarSystem> systems = systemsOf(setup(GalaxyLayout.RANDOM, spread));

            assertThat(systems).hasSize(SYSTEMS);
            assertThat(systems.stream().filter(s -> s.ownerId() == null))
                    .as("Verteilung %s", spread)
                    .allSatisfy(s -> assertThat(s.productionPerTurn()).isBetween(MIN_PROD, MAX_PROD));
        }
    }

    @Test
    void gaussianSpreadFavoursTheMiddleOfTheRange() {
        int middle = 0;
        int edges = 0;
        // Ueber mehrere Partien, damit einzelne Ausreisser den Test nicht kippen.
        for (int run = 0; run < 12; run++) {
            for (StarSystem s : systemsOf(setup(GalaxyLayout.RANDOM, ProductionDistribution.GAUSSIAN))) {
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
            even += closestPair(systemsOf(setup(GalaxyLayout.EVEN, ProductionDistribution.UNIFORM)));
            random += closestPair(systemsOf(setup(GalaxyLayout.RANDOM, ProductionDistribution.UNIFORM)));
        }

        assertThat(even).as("gleichmaessiges Layout haelt groesseren Mindestabstand").isGreaterThan(random);
    }

    @Test
    void evenLayoutPlacesHomeSystemsApart() {
        double even = 0;
        double random = 0;
        for (int run = 0; run < 8; run++) {
            even += closestPair(homesOf(systemsOf(setup(GalaxyLayout.EVEN, ProductionDistribution.UNIFORM))));
            random += closestPair(homesOf(systemsOf(setup(GalaxyLayout.RANDOM, ProductionDistribution.UNIFORM))));
        }

        assertThat(even).as("Heimatsysteme sollen weiter auseinander liegen").isGreaterThan(random);
    }

    @Test
    void everyPlayerGetsExactlyOneHomeSystem() {
        GameSetup setup = setup(GalaxyLayout.EVEN, ProductionDistribution.GAUSSIAN);
        GameId id = registry.createGame(setup);

        int players = registry.readState(id, state -> state.players().size());
        List<StarSystem> homes = registry.readState(id,
                state -> state.systems().stream().filter(s -> s.ownerId() != null).toList());

        assertThat(homes).hasSize(players);
        assertThat(homes.stream().map(StarSystem::ownerId)).doesNotHaveDuplicates();
    }

    @Test
    void defaultsKeepTheHistoricBehaviour() {
        GameSetup defaults = GameSetup.defaults().normalized();

        assertThat(defaults.productionDistribution()).isEqualTo(ProductionDistribution.UNIFORM);
        assertThat(defaults.galaxyLayout()).isEqualTo(GalaxyLayout.RANDOM);
    }

    @Test
    void nullChoicesFallBackToDefaults() {
        GameSetup normalized = new GameSetup(SYSTEMS, 1, 1, List.of(4, 4),
                MIN_PROD, MAX_PROD, 8, true, true, List.of(), null, null).normalized();

        assertThat(normalized.productionDistribution()).isEqualTo(ProductionDistribution.UNIFORM);
        assertThat(normalized.galaxyLayout()).isEqualTo(GalaxyLayout.RANDOM);
    }

    @Test
    void systemsKeepClearOfTheMapEdges() {
        for (GalaxyLayout layout : GalaxyLayout.values()) {
            for (StarSystem s : systemsOf(setup(layout, ProductionDistribution.UNIFORM))) {
                assertThat(s.x()).as("%s: x von %s", layout, s.name())
                        .isBetween(GameConfig.SYSTEM_MARGIN, GameConfig.MAX_X - GameConfig.SYSTEM_MARGIN);
                assertThat(s.y()).as("%s: y von %s", layout, s.name())
                        .isBetween(GameConfig.SYSTEM_MARGIN, GameConfig.MAX_Y - GameConfig.SYSTEM_MARGIN);
            }
        }
    }
}
