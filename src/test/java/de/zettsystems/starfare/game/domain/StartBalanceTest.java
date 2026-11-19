package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StartBalanceTest {

    /** Home 1 sits among neutral systems, homes 2 and 3 face each other closely. */
    private static GameState galaxy() {
        GameState state = new GameState();
        for (int id = 1; id <= 3; id++) {
            state.players().add(new Player(id, "P" + id, false, "#ffffff"));
        }
        List<StarSystem> systems = new ArrayList<>(List.of(
                new StarSystem(1, "Quiet", 0, 0, 1, 20, 5, false),
                new StarSystem(2, "Front", 1200, 0, 2, 20, 5, false),
                new StarSystem(3, "Border", 1300, 0, 3, 20, 5, false)));
        for (int id = 4; id <= 9; id++) {
            systems.add(new StarSystem(id, "N" + id, 60.0 * (id - 3), 80, null, 3, 2, true));
        }
        state.systems().addAll(systems);
        return state;
    }

    @Test
    void threatenedHomesGetMoreShipsAndShelteredOnesFewer() {
        GameState state = galaxy();

        StartBalance.apply(state, List.of(1, 2, 3));

        assertThat(state.getSystem(1).garrison()).isLessThan(20);
        assertThat(state.getSystem(2).garrison()).isGreaterThan(20);
        assertThat(state.getSystem(3).garrison()).isGreaterThan(20);
        assertThat(state.getSystem(2).ownerId()).isEqualTo(2);
        assertThat(state.getSystem(2).productionPerTurn()).isEqualTo(5);
    }

    @Test
    void garrisonsStayBetweenHalfAndTwiceTheRegularStart() {
        GameState state = galaxy();

        StartBalance.apply(state, List.of(1, 2, 3));

        assertThat(state.systems().stream().filter(system -> system.id() <= 3))
                .allSatisfy(home -> assertThat(home.garrison()).isBetween(10, 40));
    }

    @Test
    void symmetricHomesStayUnchanged() {
        GameState state = new GameState();
        state.players().add(new Player(1, "P1", false, "#ffffff"));
        state.players().add(new Player(2, "P2", false, "#ffffff"));
        state.systems().addAll(List.of(
                new StarSystem(1, "A", 0, 0, 1, 20, 5, false),
                new StarSystem(2, "B", 400, 0, 2, 20, 5, false),
                new StarSystem(3, "Middle", 200, 0, null, 3, 2, true)));

        StartBalance.apply(state, List.of(1, 2));

        assertThat(state.getSystem(1).garrison()).isEqualTo(20);
        assertThat(state.getSystem(2).garrison()).isEqualTo(20);
    }

    @Test
    void twoHomesAreBalancedToo() {
        GameState state = new GameState();
        state.players().add(new Player(1, "P1", false, "#ffffff"));
        state.players().add(new Player(2, "P2", false, "#ffffff"));
        state.systems().addAll(List.of(
                new StarSystem(1, "Sheltered", 0, 0, 1, 20, 5, false),
                new StarSystem(2, "Bare", 1000, 0, 2, 20, 5, false),
                new StarSystem(3, "N1", 60, 80, null, 3, 2, true),
                new StarSystem(4, "N2", 120, 80, null, 3, 2, true)));

        StartBalance.apply(state, List.of(1, 2));

        assertThat(state.getSystem(1).garrison()).isLessThan(20);
        assertThat(state.getSystem(2).garrison()).isGreaterThan(20);
    }

    @Test
    void pressureWeighsOpponentsAgainstNeutralsAndIgnoresOtherColonies() {
        GameState state = new GameState();
        state.players().add(new Player(1, "P1", false, "#ffffff"));
        state.players().add(new Player(2, "P2", false, "#ffffff"));
        state.systems().addAll(List.of(
                new StarSystem(1, "Home", 0, 0, 1, 20, 5, false),
                new StarSystem(2, "Rival", 500, 0, 2, 20, 5, false),
                new StarSystem(3, "Neutral", 150, 0, null, 3, 2, true),
                new StarSystem(4, "Colony", 80, 0, 1, 3, 2, false)));
        double expected = StartBalance.ENEMY_WEIGHT
                / (StartBalance.DISTANCE_PER_ROUND * state.travelRounds(1, 2) + StartBalance.ENEMY_OFFSET)
                - StartBalance.NEUTRAL_WEIGHT
                / (StartBalance.DISTANCE_PER_ROUND * state.travelRounds(1, 3) + StartBalance.NEUTRAL_OFFSET);

        assertThat(StartBalance.pressure(state, 1, List.of(1, 2))).isCloseTo(expected, org.assertj.core.data.Offset.offset(1e-9));
    }
}