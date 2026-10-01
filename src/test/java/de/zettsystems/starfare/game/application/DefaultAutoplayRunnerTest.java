package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.GameStateSnapshot;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.turn.application.TurnEngine;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultAutoplayRunnerTest {
    @Test
    void everyAiTurnPassesThroughPersistingRegistryAndRecordsCompletionOnce() {
        GameId id = GameId.newId();
        GameState state = new GameState();
        state.start();
        GameSession session = new GameSession(id, "AI continuation", null, Instant.now(), state);
        GameRegistry registry = mock(GameRegistry.class);
        TurnEngine engine = mock(TurnEngine.class);
        Broadcaster broadcaster = mock(Broadcaster.class);
        GameStatisticsService statistics = mock(GameStatisticsService.class);
        List<GameStateSnapshot> persisted = new ArrayList<>();
        when(registry.find(id)).thenReturn(Optional.of(session));
        when(registry.writeState(eq(id), any())).thenAnswer(call -> {
            Function<GameState, Boolean> mutation = call.getArgument(1);
            boolean stop = session.writeState(mutation);
            persisted.add(session.readState(GameState::toSnapshot));
            return stop;
        });
        doAnswer(_ -> {
            state.nextTurn();
            if (state.turn() == 3) { state.endGame(1); }
            return null;
        }).when(engine).advanceTurn(state);

        new DefaultAutoplayRunner(registry, engine, broadcaster, statistics).autoplayToEnd(id);

        assertThat(persisted).extracting(GameStateSnapshot::turn).containsExactly(2, 3);
        assertThat(persisted.getFirst().gameOver()).isFalse();
        assertThat(persisted.getLast().gameOver()).isTrue();
        verify(statistics).recordFinishedGame(id);
        ArgumentCaptor<GameEvent> events = ArgumentCaptor.forClass(GameEvent.class);
        verify(broadcaster, times(3)).publish(events.capture());
        assertThat(events.getAllValues()).containsExactly(new GameEvent.TurnAdvanced(id, 2),
                new GameEvent.TurnAdvanced(id, 3), new GameEvent.GameFinished(id, 1));
    }
}
