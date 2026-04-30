package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}
