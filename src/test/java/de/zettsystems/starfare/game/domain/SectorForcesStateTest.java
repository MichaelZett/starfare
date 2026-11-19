package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.game.values.EventFrequency;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.SectorOptions;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Score, event fleets and the round limit of SectorForces games after Galactic Empire (1983). */
class SectorForcesStateTest {

    private static GameState galaxy(int roundLimit) {
        GameState state = new GameState();
        state.rememberSetup(GameSetup.defaults().withSectorOptions(
                new SectorOptions(EventFrequency.OFF, null, false, roundLimit)));
        for (int id = 1; id <= 2; id++) {
            state.players().add(new Player(id, "P" + id, false, "#ffffff"));
            state.intel().put(id, new HashMap<>());
        }
        state.systems().addAll(List.of(
                new StarSystem(1, "A", 0, 0, 1, 10, 2, false),
                new StarSystem(2, "B", 100, 0, 1, 5, 1, false),
                new StarSystem(3, "C", 200, 0, 1, 7, 1, false),
                new StarSystem(4, "D", 900, 0, 2, 30, 3, false)));
        state.start();
        return state;
    }

    @Test
    void scoreCountsLivingSystemsStationedShipsAndOwnFleets() {
        GameState state = galaxy(0);
        state.addFleet(1, 1, 4, 4);
        state.addFleet(2, 4, 1, 9);
        state.updateCosmic(cosmic -> cosmic.explode(3));
        int stationed = state.getSystem(1).garrison() + state.getSystem(2).garrison();

        SectorScore.Line line = SectorScore.of(state, 1);

        assertThat(line.systems()).isEqualTo(2);
        assertThat(line.stationed()).isEqualTo(stationed);
        assertThat(line.inFlight()).isEqualTo(4);
        assertThat(line.points()).isEqualTo(2 * SectorScore.POINTS_PER_SYSTEM
                + stationed * SectorScore.POINTS_PER_STATIONED_SHIP + 4 * SectorScore.POINTS_PER_SHIP_IN_FLIGHT);
    }

    @Test
    void eventFleetsLeaveWithTheComingRoundAndArriveLater() {
        GameState state = galaxy(0);
        int first = state.addFleet(1, 1, 4, 2);

        int localNo = state.launchEventFleet(1, 2, 4, 3, state.turn() + 2);

        assertThat(localNo).isEqualTo(first + 1);
        Fleet event = state.fleets().getLast();
        assertThat(event.launchTurn()).isEqualTo(state.turn() + 1);
        assertThat(event.arrivalTurn()).isEqualTo(state.turn() + 2);
        assertThat(event.globalId()).isNotEqualTo(state.fleets().getFirst().globalId());
        state.launchEventFleet(1, 2, 4, 1, state.turn() + 3);
        state.addFleet(1, 1, 4, 1);
        assertThat(state.fleets()).extracting(Fleet::globalId).doesNotHaveDuplicates();
        assertThatThrownBy(() -> state.launchEventFleet(1, 2, 4, 3, state.turn() + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theRoundLimitIsReachedInItsLastRound() {
        GameState state = galaxy(SectorOptions.MIN_ROUND_LIMIT);
        while (state.turn() < SectorOptions.MIN_ROUND_LIMIT - 1) {
            state.nextTurn();
        }
        assertThat(state.roundLimitReached()).isFalse();

        state.nextTurn();

        assertThat(state.roundLimitReached()).isTrue();
        assertThat(galaxy(0).roundLimitReached()).isFalse();
    }
}