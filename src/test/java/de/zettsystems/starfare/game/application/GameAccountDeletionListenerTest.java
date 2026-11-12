package de.zettsystems.starfare.game.application;

import de.zettsystems.identity.values.AccountDeleted;
import de.zettsystems.starfare.game.domain.GameArchiveEntity;
import de.zettsystems.starfare.game.domain.GameSessionEntity;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.Player;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameAccountDeletionListenerTest {
    private final GameSessionRepository sessions = mock(GameSessionRepository.class);
    private final GameArchiveRepository archives = mock(GameArchiveRepository.class);
    private final GameSessionStore store = mock(GameSessionStore.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final GameAccountDeletionListener listener = new GameAccountDeletionListener(sessions, archives, store, mapper, jdbc);

    @Test void updatesDetachedCopiesAndInvalidatesLiveStateOnlyAfterCommit() {
        var state = state();
        String snapshot = mapper.writeValueAsString(GameState.toSnapshot(state));
        var session = new GameSessionEntity("game", "Name", "42", Instant.now(), snapshot);
        var archive = new GameArchiveEntity("archive", "Name", "42", Instant.now(), snapshot);
        when(sessions.findAll()).thenReturn(List.of(session));
        when(archives.findAll()).thenReturn(List.of(archive));
        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThatCode(() -> listener.accountDeleted(new AccountDeleted(42L, null))).doesNotThrowAnyException();
            assertThat(session.getHostPlayerId()).isEqualTo("43");
            assertThat(archive.getHostPlayerId()).isNull();
            assertThat(session.getStateJson()).doesNotContain("\"42\"");
            assertThat(archive.getStateJson()).doesNotContain("\"42\"");
            assertThat(state.seatByUser()).containsKey("42");
            verifyNoInteractions(store);
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
            verify(store).invalidate(GameId.of("game"));
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }

    @Test void leavesUnrelatedSnapshotsAloneAndDoesNotInvalidateAfterAFailedCleanup() {
        String snapshot = mapper.writeValueAsString(GameState.toSnapshot(state()));
        var session = new GameSessionEntity("game", "Name", "43", Instant.now(), snapshot);
        when(sessions.findAll()).thenReturn(List.of(session));
        when(archives.findAll()).thenReturn(List.of());
        doThrow(new IllegalStateException("SQL failed")).when(jdbc).update(anyString(), eq("99"));
        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThatThrownBy(() -> listener.accountDeleted(new AccountDeleted(99L, null))).isInstanceOf(IllegalStateException.class);
            assertThat(session.getStateJson()).isEqualTo(snapshot);
            assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
            verifyNoInteractions(store);
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }

    @Test void evictsOnlyDerivedStateAndReloadsTheCommittedSnapshot() {
        var archiveStore = mock(GameArchiveStore.class);
        var id = GameId.of("game");
        String snapshot = mapper.writeValueAsString(GameState.toSnapshot(state()));
        var entity = new GameSessionEntity("game", "Name", "42", Instant.now(), snapshot);
        when(sessions.findById("game")).thenReturn(java.util.Optional.of(entity));
        var cached = new JpaGameSessionStore(sessions, archiveStore, mapper);
        var first = cached.load(id).orElseThrow();
        assertThat(cached.load(id)).containsSame(first);
        cached.invalidate(id);
        assertThat(cached.load(id).orElseThrow()).isNotSameAs(first);
        verify(sessions, times(2)).findById("game");
    }
    private static GameState state() {
        var state = new GameState();
        state.players().add(new Player(1, "Deleted", false, "#ffffff"));
        state.seatByUser().put("42", 1);
        state.seatByUser().put("43", 2);
        state.joinedHumanPlayerIds().add(1);
        return state;
    }
}
