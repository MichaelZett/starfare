package de.zettsystems.starfare.game.values;

import de.zettsystems.starfare.game.domain.GameState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The map grows with the system count at a fixed density; fleets fly at the same speed on every map. */
class GalaxyExtentTest {

    @Test
    void theMapGrowsWithTheSystemCountAtAFixedDensity() {
        assertThat(GalaxyExtent.forSystems(16)).isEqualTo(new GalaxyExtent(2600, 1670));
        assertThat(GalaxyExtent.forSystems(24)).isEqualTo(new GalaxyExtent(3160, 2020));
        assertThat(GalaxyExtent.forSystems(40)).isEqualTo(new GalaxyExtent(4040, 2570));
        assertThat(GalaxyExtent.forSystems(64)).isEqualTo(new GalaxyExtent(5080, 3220));
        for (int systems : new int[]{16, 24, 40, 64}) {
            GalaxyExtent extent = GalaxyExtent.forSystems(systems);
            double inner = (extent.width() - 2 * GameConfig.SYSTEM_MARGIN) * (extent.height() - 2 * GameConfig.SYSTEM_MARGIN);
            assertThat(inner / systems).as("area per system at %d systems", systems).isBetween(235_000.0, 245_000.0);
        }
    }

    @Test
    void marginsStayWithinSmallMaps() {
        GalaxyExtent tiny = new GalaxyExtent(200, 100);
        assertThat(tiny.marginX()).isEqualTo(50);
        assertThat(tiny.marginY()).isEqualTo(25);
        assertThat(GalaxyExtent.LEGACY.marginX()).isEqualTo(GameConfig.SYSTEM_MARGIN);
    }

    @Test
    void travelTimeFollowsTheDistanceAtAFixedSpeedWithinItsBounds() {
        GameState state = new GameState();
        state.systems().add(new StarSystem(1, "A", 0, 0, null, 1, 1, true));
        state.systems().add(new StarSystem(2, "Neighbour", 400, 0, null, 1, 1, true));
        state.systems().add(new StarSystem(3, "Close", 50, 0, null, 1, 1, true));
        state.systems().add(new StarSystem(4, "Across", 5000, 3000, null, 1, 1, true));
        state.systems().add(new StarSystem(5, "Beyond", 9000, 0, null, 1, 1, true));

        assertThat(state.travelRounds(1, 2)).isEqualTo(3);
        assertThat(state.travelRounds(1, 3)).isEqualTo(GameConfig.MIN_TRAVEL_ROUNDS);
        assertThat(state.travelRounds(1, 4)).isEqualTo(37);
        assertThat(state.travelRounds(1, 5)).isEqualTo(GameConfig.MAX_TRAVEL_ROUNDS);
        assertThat(state.systemsWithinRounds(java.util.List.of(1), 3)).containsExactlyInAnyOrder(1, 2, 3);
    }

    @Test
    void aGameTakesTheMapOfItsSystemCountAndOlderGamesKeepTheFixedOne() {
        GameState state = new GameState();
        assertThat(state.extent()).isEqualTo(GalaxyExtent.LEGACY);
        state.rememberSetup(GameSetup.defaults());
        assertThat(state.extent()).isEqualTo(GalaxyExtent.forSystems(GameConfig.DEFAULT_SYSTEM_COUNT));
        assertThat(GameState.fromSnapshot(GameState.toSnapshot(state)).extent()).isEqualTo(state.extent());
    }
}
