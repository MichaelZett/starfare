package de.zettsystems.starfare.game.application;

import de.zettsystems.gamesplatformapi.MatchEnded;
import de.zettsystems.gamesplatformapi.MatchResult;
import de.zettsystems.starfare.game.domain.GameEndEntity;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.platform.StarfareMatches;
import de.zettsystems.starfare.game.values.GameId;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
class DefaultGameEndRecorder implements GameEndRecorder {
    private final GameRegistry registry;
    private final GameEndRepository ends;
    private final ApplicationEventPublisher events;

    DefaultGameEndRecorder(GameRegistry registry, GameEndRepository ends) {
        this(registry, ends, _ -> { });
    }

    @Autowired
    DefaultGameEndRecorder(GameRegistry registry, GameEndRepository ends, ApplicationEventPublisher events) {
        this.registry = registry;
        this.ends = ends;
        this.events = events;
    }

    /**
     * Records the end of a finished game once and announces it to the platform within the same
     * transaction ({@link MatchEnded}); a game the winner continues does not end twice. A game
     * without human accounts is not recorded.
     */
    @Override
    @Transactional
    public void recordFinishedGame(GameId gameId) {
        if (ends.existsById(gameId.value())) {
            return;
        }
        GameSession session = registry.find(gameId).orElse(null);
        if (session == null) {
            return;
        }
        FinishedGame game = session.readState(state -> state.gameOver() && !state.seatByUser().isEmpty()
                ? new FinishedGame(state.finishedAt(), StarfareMatches.result(state).orElse(null), state.tutorial())
                : null);
        if (game == null) {
            return;
        }
        Instant recordedEnd = game.finishedAt();
        Instant finishedAt = recordedEnd == null ? Instant.now() : recordedEnd;
        ends.save(new GameEndEntity(gameId.value(), finishedAt));
        MatchResult platformResult = game.platformResult();
        if (platformResult != null) {
            events.publishEvent(new MatchEnded(StarfareMatches.GAME_KEY, gameId.value(), finishedAt, platformResult,
                    game.tutorial()));
        }
    }

    private record FinishedGame(@Nullable Instant finishedAt, @Nullable MatchResult platformResult, boolean tutorial) {
    }
}
