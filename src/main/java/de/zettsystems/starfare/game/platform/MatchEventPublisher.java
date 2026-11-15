package de.zettsystems.starfare.game.platform;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.MatchChanged;
import de.zettsystems.gamesplatformapi.MatchEnded;
import de.zettsystems.gamesplatformapi.MatchResult;
import de.zettsystems.starfare.game.application.Broadcaster;
import de.zettsystems.starfare.game.application.GameEvent;
import de.zettsystems.starfare.game.application.GameRegistry;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Turns Starfare's game events into the platform's {@link MatchChanged} and, for aborted games,
 * {@link MatchEnded}. Starfare keeps its games in memory and stores each change before it
 * broadcasts it, outside a database transaction, so the platform events follow the stored change.
 * A regular end is announced by the statistics service, once, when it records the result.
 */
@Component
class MatchEventPublisher implements InitializingBean {

    private final Broadcaster broadcaster;
    private final GameRegistry registry;
    private final ApplicationEventPublisher events;

    MatchEventPublisher(Broadcaster broadcaster, GameRegistry registry, ApplicationEventPublisher events) {
        this.broadcaster = broadcaster;
        this.registry = registry;
        this.events = events;
    }

    @Override
    public void afterPropertiesSet() {
        broadcaster.subscribeAll(this::onGameEvent);
    }

    void onGameEvent(GameEvent event) {
        switch (event) {
            case GameEvent.GameAborted aborted -> onAborted(aborted);
            // The last turn of a game also ends it; the end is announced by MatchEnded alone.
            case GameEvent.TurnAdvanced advanced -> {
                if (!ended(advanced.gameId())) {
                    changed(advanced.gameId());
                }
            }
            case GameEvent.GameCreated _, GameEvent.PlayerJoined _, GameEvent.PlayerSubmitted _,
                 GameEvent.SeatAbandoned _, GameEvent.GameStarted _, GameEvent.GameContinued _,
                 GameEvent.VisibilityChanged _, GameEvent.InvitationChanged _ -> changed(event.gameId());
            default -> {
                // Chat, diplomacy, production, observers, host and round rules are not part of a match summary.
            }
        }
    }

    private void onAborted(GameEvent.GameAborted aborted) {
        List<AccountId> participants = StarfareMatches.accounts(aborted.accounts());
        if (aborted.running() && !participants.isEmpty()) {
            events.publishEvent(new MatchEnded(StarfareMatches.GAME_KEY, aborted.gameId().value(), Instant.now(),
                    MatchResult.aborted(participants)));
        } else {
            // An open game that is cancelled never started; it just leaves the lobby.
            changed(aborted.gameId());
        }
    }

    private boolean ended(GameId id) {
        return registry.find(id).map(session -> session.readState(GameState::gameOver)).orElse(true);
    }

    private void changed(GameId id) {
        events.publishEvent(new MatchChanged(StarfareMatches.GAME_KEY, id.value(), Instant.now()));
    }
}
