package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.game.values.VisibleSystem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;

class IdleShipsTest {

    @Test
    void shipsBeyondOneRoundOfProductionAreIdle() {
        assertThat(IdleShips.of(system(5, 4))).isEqualTo(OptionalInt.of(5));
    }

    @Test
    void shipsBuiltThisRoundAreNotIdleYet() {
        assertThat(IdleShips.of(system(4, 4))).isEmpty();
    }

    @Test
    void noFreeShipsNeverCount() {
        assertThat(IdleShips.of(system(0, 0))).isEmpty();
    }

    @Test
    void aSystemWithoutProductionShowsItsLastShips() {
        assertThat(IdleShips.of(system(1, 0))).isEqualTo(OptionalInt.of(1));
    }

    @Test
    void foreignSystemsWithoutFreeShipCountStayQuiet() {
        assertThat(IdleShips.of(system(null, 4))).isEmpty();
    }

    @Test
    void unknownProductionStaysQuiet() {
        assertThat(IdleShips.of(system(9, null))).isEmpty();
    }

    private static VisibleSystem system(Integer free, Integer production) {
        return new VisibleSystem(1, "Alpha", 0, 0, 1, 20, production, true, "#abc", null, false,
                0, 0, free, List.of());
    }
}
