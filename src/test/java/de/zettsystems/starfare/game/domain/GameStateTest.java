package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GameStateTest {

    @Test
    void updateSystemReplacesEntry() {
        GameState state = new GameState();
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 5, 2, false));

        state.updateSystem(1, current -> current.reinforce(4));

        assertThat(state.getSystem(1).garrison()).isEqualTo(9);
    }

    @Test
    void copyOfCreatesIndependentLists() {
        GameState state = new GameState();
        state.players().add(new Player(1, "P1", false, "#fff"));
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 5, 2, false));

        GameState copy = GameState.copyOf(state);
        state.systems().add(new StarSystem(2, "S2", 1, 1, null, 3, 2, true));

        assertThat(copy.systems()).hasSize(1);
        assertThat(copy.systems()).size().isNotEqualTo(state.systems().size());
    }

    @Test
    void resetForNewGameClearsSubmittedThisTurn() {
        GameState state = new GameState();
        state.submittedThisTurn().add(1);
        state.submittedThisTurn().add(2);

        state.resetForNewGame();

        assertThat(state.submittedThisTurn()).isEmpty();
    }

    @Test
    void resetForAbortClearsSubmittedThisTurn() {
        GameState state = new GameState();
        state.submittedThisTurn().add(1);

        state.resetForAbort();

        assertThat(state.submittedThisTurn()).isEmpty();
    }

    @Test
    void copyOfCreatesIndependentSubmittedThisTurn() {
        GameState state = new GameState();
        state.submittedThisTurn().add(1);

        GameState copy = GameState.copyOf(state);
        state.submittedThisTurn().add(2);

        assertThat(copy.submittedThisTurn()).contains(1);
        assertThat(copy.submittedThisTurn()).doesNotContain(2);
        assertThat(copy.submittedThisTurn()).hasSize(1);
    }

    @Test
    void copyOfCarriesObserversAndFlags() {
        GameState state = new GameState();
        state.observers().add("alice");
        state.configureLobby(true, true);

        GameState copy = GameState.copyOf(state);
        state.observers().add("bob");

        assertThat(copy.observers()).contains("alice");
        assertThat(copy.observers()).doesNotContain("bob");
        assertThat(copy.observersAllowed()).isTrue();
        assertThat(copy.reentryAllowed()).isTrue();
    }

    @Test
    void resetForNewGameClearsPendingOrders() {
        GameState state = new GameState();
        state.pendingOrders().put(1, List.of(new FleetOrder.Send(1, 1, 2, 3)));

        state.resetForNewGame();

        assertThat(state.pendingOrders()).isEmpty();
    }

    @Test
    void resetForAbortClearsPendingOrders() {
        GameState state = new GameState();
        state.pendingOrders().put(2, List.of(new FleetOrder.Wait(2, 7)));

        state.resetForAbort();

        assertThat(state.pendingOrders()).isEmpty();
    }

    @Test
    void copyOfCreatesIndependentPendingOrders() {
        GameState state = new GameState();
        state.pendingOrders().put(1, new java.util.ArrayList<>(List.of(new FleetOrder.Send(1, 1, 2, 3))));

        GameState copy = GameState.copyOf(state);
        state.pendingOrders().get(1).add(new FleetOrder.Disband(1, 99));

        assertThat(copy.pendingOrders().get(1)).hasSize(1);
        assertThat(state.pendingOrders().get(1)).hasSize(2);
    }

    @Test
    void resetForNewGameClearsObserversAndFlags() {
        GameState state = new GameState();
        state.observers().add("alice");
        state.configureLobby(true, true);

        state.resetForNewGame();

        assertThat(state.observers()).isEmpty();
        assertThat(state.observersAllowed()).isFalse();
        assertThat(state.reentryAllowed()).isFalse();
    }

    @Test
    void turnStartedAtSurvivesSnapshotRoundTrip() {
        GameState state = new GameState();
        state.start();
        state.nextTurn();
        Instant expected = state.turnStartedAt();

        GameState restored = GameState.fromSnapshot(GameState.toSnapshot(state));

        assertThat(restored.turnStartedAt()).isEqualTo(expected);
    }

    @Test
    void snapshotWithoutTurnStartedAtStartsClockOnRestore() {
        GameState state = new GameState();
        state.start();
        // Referenzzeit aus dem Produktionscode statt aus der Systemuhr im Test:
        // ein Default wie Instant.EPOCH faellt damit genauso auf.
        Instant startedInOriginal = state.turnStartedAt();
        GameStateSnapshot legacy = withoutTurnStartedAt(GameState.toSnapshot(state));

        GameState restored = GameState.fromSnapshot(legacy);

        assertThat(restored.turnStartedAt())
                .isNotNull()
                .isAfterOrEqualTo(startedInOriginal);
    }

    private static GameStateSnapshot withoutTurnStartedAt(GameStateSnapshot s) {
        return new GameStateSnapshot(s.turn(), s.nextGlobalFleetId(), s.nextLocalFleetNo(), s.players(), s.systems(),
                s.fleets(), s.reports(), s.intel(), s.waitThisTurn(), s.submittedThisTurn(), s.gameOver(), s.winnerId(),
                s.active(), s.started(), s.joinedHumanPlayerIds(), s.originalHumanPlayerIds(), s.observers(),
                s.seatByUser(), s.invitedSeats(), s.pendingOrders(), s.standingOrders(), s.nextStandingOrderId(),
                s.observersAllowed(), s.reentryAllowed(), null, s.visibility(), s.finishedAt());
    }

    @Test
    void nextTurnRestartsTheTurnClock() {
        GameState state = new GameState();
        state.start();
        Instant first = state.turnStartedAt();

        state.nextTurn();

        assertThat(state.turnStartedAt()).isAfterOrEqualTo(first);
    }

    @Test
    void systemsWithinRoundsCoversNeighboursButNotDistantSystems() {
        GameState state = new GameState();
        state.systems().add(new StarSystem(1, "A", 0, 0, 1, 5, 1, false));
        state.systems().add(new StarSystem(2, "B", 5, 0, null, 5, 1, true));
        state.systems().add(new StarSystem(3, "C", 200, 0, null, 5, 1, true));

        Set<Integer> reachable = state.systemsWithinRounds(List.of(1), 2);

        assertThat(reachable).contains(1, 2).doesNotContain(3);
        assertThat(state.systemsWithinRounds(List.of(), 2)).isEmpty();
    }

    @Test
    void systemsWithinRoundsMatchesTravelRounds() {
        GameState state = new GameState();
        state.systems().add(new StarSystem(1, "A", 0, 0, 1, 5, 1, false));
        state.systems().add(new StarSystem(2, "B", 60, 0, null, 5, 1, true));
        state.systems().add(new StarSystem(3, "C", 200, 0, null, 5, 1, true));

        for (int target : List.of(1, 2, 3)) {
            assertThat(state.systemsWithinRounds(List.of(1), 4).contains(target))
                    .as("System %d", target)
                    .isEqualTo(state.travelRounds(1, target) <= 4);
        }
    }
}
