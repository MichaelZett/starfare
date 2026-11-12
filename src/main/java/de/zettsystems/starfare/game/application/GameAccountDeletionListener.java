package de.zettsystems.starfare.game.application;

import de.zettsystems.identity.values.AccountDeleted;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.GameStateSnapshot;
import de.zettsystems.starfare.game.values.GameId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

/** Updates detached snapshot copies inside identity's transaction; live caches change only after commit. */
@Component
public class GameAccountDeletionListener {
    private static final List<String> DELETE_BINDINGS = List.of(
            "DELETE FROM battle_acknowledgements WHERE player_id = ?",
            "DELETE FROM game_chat_messages WHERE sender_player_id = ?",
            "DELETE FROM game_result_participants WHERE player_id = ?",
            "DELETE FROM game_result_winners WHERE player_id = ?");
    private static final String CLEAR_WINNER = "UPDATE game_results SET winner_player_id = NULL, version = version + 1 WHERE winner_player_id = ?";
    private final GameSessionRepository sessions;
    private final GameArchiveRepository archives;
    private final GameSessionStore store;
    private final ObjectMapper mapper;
    private final JdbcTemplate jdbc;

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected repositories and collaborators are shared Spring beans") // rationale: dependency injection shares collaborators.
    public GameAccountDeletionListener(GameSessionRepository sessions, GameArchiveRepository archives,
                                       GameSessionStore store, ObjectMapper mapper, JdbcTemplate jdbc) {
        this.sessions = sessions;
        this.archives = archives;
        this.store = store;
        this.mapper = mapper;
        this.jdbc = jdbc;
    }

    @EventListener
    @org.springframework.core.annotation.Order(0)
    @Transactional(propagation = Propagation.MANDATORY)
    public void accountDeleted(AccountDeleted event) {
        String account = event.userId().toString();
        var changed = releaseSessions(account);
        releaseArchives(account);
        DELETE_BINDINGS.forEach(sql -> jdbc.update(sql, account));
        jdbc.update(CLEAR_WINNER, account);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { changed.forEach(store::invalidate); }
        });
    }

    private List<GameId> releaseSessions(String account) {
        var changed = new ArrayList<GameId>();
        for (var entity : sessions.findAll()) {
            GameState state = read(entity.getStateJson());
            boolean released = state.releaseAccount(account);
            if (account.equals(entity.getHostPlayerId())) {
                entity.transferHostTo(state.seatByUser().keySet().stream().sorted().findFirst().orElse(null));
                released = true;
            }
            if (released) {
                entity.replaceSnapshot(write(state));
                changed.add(GameId.of(java.util.Objects.requireNonNull(entity.getId(), "Persisted game id")));
            }
        }
        return changed;
    }

    private void releaseArchives(String account) {
        for (var entity : archives.findAll()) {
            GameState state = read(entity.getStateJson());
            boolean released = state.releaseAccount(account);
            if (account.equals(entity.getHostPlayerId())) {
                entity.transferHostTo(null);
                released = true;
            }
            if (released) { entity.replaceSnapshot(write(state)); }
        }
    }

    private GameState read(String json) { return GameState.fromSnapshot(mapper.readValue(json, GameStateSnapshot.class)); }
    private String write(GameState state) { return mapper.writeValueAsString(GameState.toSnapshot(state)); }
}
