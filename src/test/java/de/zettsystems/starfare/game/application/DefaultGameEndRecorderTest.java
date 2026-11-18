package de.zettsystems.starfare.game.application;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.MatchEnded;
import de.zettsystems.starfare.game.domain.GameEndEntity;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameOutcome;
import de.zettsystems.starfare.game.values.Player;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultGameEndRecorderTest {

    @Test
    void recordsTheFirstEndAndAnnouncesItToThePlatform() {
        GameRegistry registry = mock(GameRegistry.class);
        GameEndRepository ends = mock(GameEndRepository.class);
        GameId id = new GameId("finished");
        Instant finishedAt = Instant.parse("2026-09-16T10:00:00Z");
        GameState state = finishedState(finishedAt, Map.of("17", 1, "23", 2));
        GameSession session = session(state);
        when(registry.find(id)).thenReturn(Optional.of(session));
        List<Object> published = new ArrayList<>();
        ApplicationEventPublisher events = published::add;

        new DefaultGameEndRecorder(registry, ends, events).recordFinishedGame(id);

        ArgumentCaptor<GameEndEntity> saved = ArgumentCaptor.forClass(GameEndEntity.class);
        verify(ends).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(id.value());
        assertThat(saved.getValue().getFinishedAt()).isEqualTo(finishedAt);
        assertThat(published).singleElement().isInstanceOfSatisfying(MatchEnded.class, ended -> {
            assertThat(ended.matchId()).isEqualTo(id.value());
            assertThat(ended.endedAt()).isEqualTo(finishedAt);
            assertThat(ended.participants()).containsExactlyInAnyOrder(AccountId.of(17), AccountId.of(23));
        });
    }

    @Test
    void anUndatedEndIsRecordedNow() {
        GameRegistry registry = mock(GameRegistry.class);
        GameEndRepository ends = mock(GameEndRepository.class);
        GameId id = new GameId("undated");
        GameSession session = session(finishedState(null, Map.of("17", 1)));
        when(registry.find(id)).thenReturn(Optional.of(session));
        Instant before = Instant.now();

        new DefaultGameEndRecorder(registry, ends).recordFinishedGame(id);

        ArgumentCaptor<GameEndEntity> saved = ArgumentCaptor.forClass(GameEndEntity.class);
        verify(ends).save(saved.capture());
        assertThat(saved.getValue().getFinishedAt()).isAfterOrEqualTo(before);
    }

    @Test
    void doesNotRecordExistingMissingActiveOrAccountlessGame() {
        GameRegistry registry = mock(GameRegistry.class);
        GameEndRepository ends = mock(GameEndRepository.class);
        DefaultGameEndRecorder recorder = new DefaultGameEndRecorder(registry, ends);
        GameId existing = new GameId("existing");
        when(ends.existsById(existing.value())).thenReturn(true);
        recorder.recordFinishedGame(existing);

        GameId missing = new GameId("missing");
        when(registry.find(missing)).thenReturn(Optional.empty());
        recorder.recordFinishedGame(missing);

        GameState running = mock(GameState.class);
        GameId active = new GameId("active");
        GameSession activeSession = session(running);
        when(registry.find(active)).thenReturn(Optional.of(activeSession));
        recorder.recordFinishedGame(active);

        GameId computerOnly = new GameId("computer-only");
        GameSession computerSession = session(finishedState(Instant.now(), Map.of()));
        when(registry.find(computerOnly)).thenReturn(Optional.of(computerSession));
        recorder.recordFinishedGame(computerOnly);

        verify(ends, never()).save(any());
    }

    private static GameState finishedState(@Nullable Instant finishedAt, Map<String, Integer> seats) {
        GameState state = mock(GameState.class);
        when(state.gameOver()).thenReturn(true);
        when(state.seatByUser()).thenReturn(seats);
        when(state.outcome()).thenReturn(new GameOutcome(2, null));
        when(state.winnerId()).thenReturn(2);
        when(state.finishedAt()).thenReturn(finishedAt);
        when(state.players()).thenReturn(seats.values().stream()
                .map(seat -> new Player(seat, "P" + seat, false, "#ffffff")).toList());
        return state;
    }

    private static GameSession session(GameState state) {
        GameSession session = mock(GameSession.class);
        when(session.readState(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Function<GameState, Object> reader = invocation.getArgument(0);
            return reader.apply(state);
        });
        return session;
    }
}
