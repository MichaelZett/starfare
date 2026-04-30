package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.values.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class GameServiceObserveTest extends AbstractIntegrationTest {

    @Autowired
    private GameService game;

    private GameId setupStartedGameWithObservers(boolean observersAllowed) {
        GameId gid = registry.createGame(GameSetup.defaults());
        registry.writeState(gid, state -> {
            state.resetForNewGame();
            state.configureLobby(observersAllowed, false);
            state.players().add(new Player(1, "P1", false, "#111111"));
            state.players().add(new Player(2, "AI1", true, "#222222"));
            state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));
            state.systems().add(new StarSystem(2, "S2", 1000, 0, 2, 10, 2, false));
            state.systems().add(new StarSystem(3, "S3", 500, 500, null, 5, 2, true));
            state.fleets().add(new Fleet(1, 1, 1, 1, 2, 3, 1, 3));
            state.joinedHumanPlayerIds().add(1);
            state.start();
            return null;
        });
        return gid;
    }

    @Test
    void observeGameAcceptedWhenAllowed() {
        GameId id = setupStartedGameWithObservers(true);

        assertThat(game.observeGame(id, "alice")).isTrue();
        assertThat(game.isObserver(id, "alice")).isTrue();
    }

    @Test
    void observeGameRejectedWhenDisallowed() {
        GameId id = setupStartedGameWithObservers(false);

        assertThat(game.observeGame(id, "alice")).isFalse();
        assertThat(game.isObserver(id, "alice")).isFalse();
    }

    @Test
    void observeGameIdempotent() {
        GameId id = setupStartedGameWithObservers(true);

        assertThat(game.observeGame(id, "alice")).isTrue();
        assertThat(game.observeGame(id, "alice")).isTrue();

        assertThat((int) registry.readState(id, state -> state.observers().size())).isOne();
    }

    @Test
    void leaveObserveRemovesObserver() {
        GameId id = setupStartedGameWithObservers(true);
        game.observeGame(id, "alice");
        assertThat(game.isObserver(id, "alice")).isTrue();

        assertThat(game.leaveObserve(id, "alice")).isTrue();
        assertThat(game.isObserver(id, "alice")).isFalse();
    }

    @Test
    void leaveObserveForUnknownUserIsFalse() {
        GameId id = setupStartedGameWithObservers(true);

        assertThat(game.leaveObserve(id, "unknown")).isFalse();
    }

    @Test
    void observeRejectedAfterGameOver() {
        GameId id = setupStartedGameWithObservers(true);
        registry.writeState(id, state -> {
            state.endGame(1);
            return null;
        });

        assertThat(game.observeGame(id, "alice")).isFalse();
    }

    @Test
    void observeRejectedForBlankOrNullUsername() {
        GameId id = setupStartedGameWithObservers(true);

        assertThat(game.observeGame(id, null)).isFalse();
        assertThat(game.observeGame(id, "")).isFalse();
        assertThat(game.observeGame(id, "  ")).isFalse();
    }

    @Test
    void viewForObserverShowsEverySystemFullyVisible() {
        GameId id = setupStartedGameWithObservers(true);

        PlayerViewState view = game.viewForObserver(id);

        assertThat(view.systems()).hasSize(3);
        for (VisibleSystem s : view.systems()) {
            assertThat(s.fullyVisible()).as("system " + s.id() + " should be fully visible").isTrue();
        }
    }

    @Test
    void viewForObserverShowsOwnerAndColorForOwnedSystems() {
        GameId id = setupStartedGameWithObservers(true);

        PlayerViewState view = game.viewForObserver(id);

        var s1 = view.systems().stream().filter(s -> s.id() == 1).findFirst().orElseThrow();
        var s3 = view.systems().stream().filter(s -> s.id() == 3).findFirst().orElseThrow();
        assertThat(s1.ownerId()).isOne();
        assertThat(s1.colorHex()).isEqualTo("#111111");
        assertThat(s3.ownerId()).isNull();
    }

    @Test
    void viewForObserverShowsAllFleets() {
        GameId id = setupStartedGameWithObservers(true);

        PlayerViewState view = game.viewForObserver(id);

        assertThat(view.ownFleets()).hasSize(1);
    }

    @Test
    void observersAllowedFlagIsReadable() {
        assertThat(game.observersAllowed(setupStartedGameWithObservers(true))).isTrue();
        assertThat(game.observersAllowed(setupStartedGameWithObservers(false))).isFalse();
    }

    @Test
    void observeBeforeStartStillAllowed() {
        GameId id = registry.createGame(GameSetup.defaults());
        registry.writeState(id, state -> {
            state.resetForNewGame();
            state.configureLobby(true, false);
            state.players().add(new Player(1, "P1", false, "#111111"));
            return null;
        });

        assertThat(game.observeGame(id, "alice")).isTrue();
    }

    @Test
    void observeRejectedWhenGameNotActive() {
        GameId id = registry.createGame(GameSetup.defaults());
        registry.writeState(id, state -> {
            state.resetForAbort();
            return null;
        });

        assertThat(game.observeGame(id, "alice")).isFalse();
    }
}
