package de.zettsystems.starfare.game.application;

import de.zettsystems.gamesplatformapi.MatchChanged;
import de.zettsystems.gamesplatformapi.MatchEnded;
import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Finished games leave the database together with their archive entry, without a word to the platform. */
@RecordApplicationEvents
class GameArchiveCleanupIntegrationTest extends AbstractIntegrationTest {

    @Autowired private GameArchiveCleanupService cleanup;
    @Autowired private GameSessionStore sessions;
    @Autowired private GameArchiveStore archives;
    @Autowired private BattleAcknowledgementService acknowledgements;
    @Autowired private ApplicationEvents events;

    private final List<GameId> created = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        created.forEach(id -> {
            sessions.delete(id);
            archives.delete(id);
            acknowledgements.forgetGame(id);
        });
    }

    @Test
    void deletesTheStoredGameAndItsArchiveEntry91DaysAfterTheEnd() {
        GameId expired = finishedGame(Duration.ofDays(91));
        GameId recent = finishedGame(Duration.ofDays(89));
        acknowledgements.acknowledge("17", expired, 3, 0);
        events.clear();

        int removed = cleanup.removeExpired();

        assertThat(removed).isOne();
        assertThat(sessions.load(expired)).isEmpty();
        assertThat(archives.load(expired)).isEmpty();
        assertThat(acknowledgements.acknowledgedEventIndices("17", expired, 3)).isEmpty();
        assertThat(sessions.load(recent)).isPresent();
        assertThat(archives.load(recent)).isPresent();
        assertThat(events.stream(MatchEnded.class)).as("the platform keeps the result in its statistics").isEmpty();
        assertThat(events.stream(MatchChanged.class)).isEmpty();
    }

    @Test
    void keepsAGameContinuedAfterItsVictory() {
        GameId continued = finishedGame(Duration.ofDays(100));
        GameSession session = sessions.load(continued).orElseThrow();
        session.writeState(state -> {
            state.clearGameOver();
            return null;
        });
        sessions.save(session);

        assertThat(cleanup.removeExpired()).isZero();
        assertThat(sessions.load(continued)).isPresent();
        assertThat(archives.load(continued)).isPresent();
    }

    private GameId finishedGame(Duration ago) {
        GameState state = new GameState();
        state.players().add(new Player(1, "P1", false, "#111111"));
        state.players().add(new Player(2, "P2", false, "#222222"));
        state.seatByUser().put("17", 1);
        state.seatByUser().put("23", 2);
        state.joinedHumanPlayerIds().add(1);
        state.joinedHumanPlayerIds().add(2);
        state.start();
        state.endGame(1, Instant.now().minus(ago));
        GameId id = GameId.newId();
        created.add(id);
        sessions.save(new GameSession(id, "Cleanup", "17", Instant.now().minus(ago), state));
        assertThat(archives.load(id)).as("a finished game is archived when it is stored").isPresent();
        return id;
    }
}
