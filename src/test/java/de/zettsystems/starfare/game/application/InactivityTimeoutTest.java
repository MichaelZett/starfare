package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Timeout auf 1 ms, damit jeder gestartete Zug sofort als abgelaufen gilt.
 * Das Prüfintervall steht hoch, damit der Scheduler nicht in die Tests funkt —
 * die Tests rufen {@link GameService#expireInactiveSeats(GameId)} selbst auf.
 * Folge: auch ein gerade erst begonnener Folgezug gilt sofort als abgelaufen,
 * deshalb prüft hier kein Test den Zustand nach einem regulären Zugwechsel.
 */
@TestPropertySource(properties = {
        "starfare.game.inactivity-timeout=1ms",
        "starfare.game.inactivity-check-interval=1h"
})
class InactivityTimeoutTest extends AbstractIntegrationTest {

    @Autowired
    private GameService game;

    private GameId runningGameWithTwoHumans() {
        GameId id = registry.createGame(GameSetup.defaults());
        registry.writeState(id, state -> {
            state.resetForNewGame();
            state.configureLobby(true, true);
            state.players().add(new Player(1, "P1", false, "#111111"));
            state.players().add(new Player(2, "P2", false, "#222222"));
            state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));
            state.systems().add(new StarSystem(2, "S2", 1000, 0, 2, 10, 2, false));
            state.systems().add(new StarSystem(3, "S3", 500, 500, null, 5, 1, true));
            state.originalHumanPlayerIds().add(1);
            state.originalHumanPlayerIds().add(2);
            state.joinedHumanPlayerIds().add(1);
            state.joinedHumanPlayerIds().add(2);
            state.seatByUser().put("alice", 1);
            state.seatByUser().put("bob", 2);
            state.start();
            return null;
        });
        return id;
    }

    private boolean isAi(GameId id, int seatId) {
        return registry.readState(id, state -> state.players().stream()
                .filter(p -> p.id() == seatId).findFirst().orElseThrow().ai());
    }

    @Test
    void seatThatDidNotSubmitIsTakenOverByAi() {
        GameId id = runningGameWithTwoHumans();
        game.submitTurn(id, 1);

        assertThat(game.expireInactiveSeats(id)).isTrue();

        assertThat(isAi(id, 2)).as("saeumiger Sitz").isTrue();
        assertThat(isAi(id, 1)).as("Sitz hat abgegeben").isFalse();
        Set<Integer> joined = registry.readState(id, GameState::joinedHumanPlayerIds);
        assertThat(joined).containsExactly(1);
    }

    @Test
    void timedOutSeatCanBeReclaimedWhenReentryIsAllowed() {
        GameId id = runningGameWithTwoHumans();

        game.expireInactiveSeats(id);

        Map<String, Integer> seats = registry.readState(id, GameState::seatByUser);
        assertThat(seats).as("die Sitzzuordnung bleibt, sonst greift kein Wiedereinstieg")
                .containsEntry("bob", 2);
        assertThat(isAi(id, 2)).isTrue();

        assertThat(registry.claimSeat(id, "bob")).contains(2);
        assertThat(isAi(id, 2)).as("Sitz ist wieder menschlich").isFalse();
    }

    @Test
    void timedOutSeatStaysAiWhenReentryIsForbidden() {
        GameId id = runningGameWithTwoHumans();
        registry.writeState(id, state -> {
            state.configureLobby(true, false);
            return null;
        });

        game.expireInactiveSeats(id);

        assertThat(registry.claimSeat(id, "bob")).isEmpty();
        assertThat(isAi(id, 2)).isTrue();
    }

    @Test
    void expiringTheLastPendingSeatAdvancesTheTurn() {
        GameId id = runningGameWithTwoHumans();
        game.submitTurn(id, 1);
        int before = registry.readState(id, GameState::turn);

        game.expireInactiveSeats(id);

        int after = registry.readState(id, GameState::turn);
        assertThat(after).isGreaterThan(before);
    }

    @Test
    void gameThatHasNotStartedIsUntouched() {
        GameId id = registry.createGame(GameSetup.defaults());
        registry.writeState(id, state -> {
            state.resetForNewGame();
            state.players().add(new Player(1, "P1", false, "#111111"));
            state.joinedHumanPlayerIds().add(1);
            return null;
        });

        assertThat(game.expireInactiveSeats(id)).isFalse();
        assertThat(isAi(id, 1)).isFalse();
    }

    @Test
    void finishedGameIsUntouched() {
        GameId id = runningGameWithTwoHumans();
        registry.writeState(id, state -> {
            state.endGame(1);
            return null;
        });

        assertThat(game.expireInactiveSeats(id)).isFalse();
        assertThat(isAi(id, 1)).isFalse();
        assertThat(isAi(id, 2)).isFalse();
    }

    @Test
    void singleHumanGameNeverHandsItsPlayerToAi() {
        GameId id = runningGameWithTwoHumans();
        registry.writeState(id, state -> {
            state.players().removeIf(player -> player.id() == 2);
            state.originalHumanPlayerIds().remove(2);
            state.joinedHumanPlayerIds().remove(2);
            state.seatByUser().remove("bob");
            return null;
        });

        assertThat(game.expireInactiveSeats(id)).isFalse();
        assertThat(isAi(id, 1)).isFalse();
    }

    @Test
    void hostRoleMovesOnWhenTheHostTimesOut() {
        GameId id = registry.createGame(GameSetup.defaults(), "alice", "timeout-game");
        registry.writeState(id, state -> {
            state.resetForNewGame();
            state.configureLobby(true, true);
            state.players().add(new Player(1, "P1", false, "#111111"));
            state.players().add(new Player(2, "P2", false, "#222222"));
            state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));
            state.systems().add(new StarSystem(2, "S2", 1000, 0, 2, 10, 2, false));
            state.systems().add(new StarSystem(3, "S3", 500, 500, null, 5, 1, true));
            state.originalHumanPlayerIds().add(1);
            state.originalHumanPlayerIds().add(2);
            state.joinedHumanPlayerIds().add(1);
            state.joinedHumanPlayerIds().add(2);
            state.seatByUser().put("alice", 1);
            state.seatByUser().put("bob", 2);
            state.start();
            return null;
        });
        game.submitTurn(id, 2);

        assertThat(game.expireInactiveSeats(id)).isTrue();

        assertThat(registry.require(id).hostPlayerId())
                .as("alice ist raus, bob uebernimmt")
                .isEqualTo("bob");
    }
}
