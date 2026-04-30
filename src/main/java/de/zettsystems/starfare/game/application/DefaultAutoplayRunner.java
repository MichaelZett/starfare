package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.turn.application.TurnEngine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring-injected collaborators are kept by reference for the bean's lifetime by design.")
public class DefaultAutoplayRunner implements AutoplayRunner {
    private static final int MAX_TURNS = 10_000;

    private final GameRegistry registry;
    private final TurnEngine turnEngine;
    private final Broadcaster broadcaster;

    public DefaultAutoplayRunner(GameRegistry registry, TurnEngine turnEngine, Broadcaster broadcaster) {
        this.registry = registry;
        this.turnEngine = turnEngine;
        this.broadcaster = broadcaster;
    }

    @Async
    @Override
    public void autoplayToEnd(GameId gameId) {
        for (int i = 0; i < MAX_TURNS; i++) {
            List<GameEvent> events = new ArrayList<>();
            boolean stop = registry.find(gameId)
                    .map(session -> session.writeState(state -> tickOnce(gameId, state, events)))
                    .orElse(true);
            for (GameEvent event : events) {
                broadcaster.publish(event);
            }
            if (stop) {
                return;
            }
        }
    }

    private boolean tickOnce(GameId gameId, GameState state, List<GameEvent> events) {
        if (!state.active() || !state.started() || state.gameOver()) {
            return true;
        }
        if (!state.joinedHumanPlayerIds().isEmpty() || !state.observers().isEmpty()) {
            return true;
        }
        turnEngine.advanceTurn(state);
        state.submittedThisTurn().clear();
        events.add(new GameEvent.TurnAdvanced(gameId, state.turn()));
        if (state.gameOver()) {
            events.add(new GameEvent.GameFinished(gameId, state.winnerId()));
            return true;
        }
        return false;
    }
}
