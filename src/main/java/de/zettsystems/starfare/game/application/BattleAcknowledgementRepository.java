package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.BattleAcknowledgementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Set;

interface BattleAcknowledgementRepository
        extends JpaRepository<BattleAcknowledgementEntity, BattleAcknowledgementEntity.Key> {

    @Query("select ack.id.eventIndex from BattleAcknowledgementEntity ack "
            + "where ack.id.playerId = :playerId and ack.id.gameId = :gameId and ack.id.turn = :turn")
    Set<Integer> findEventIndices(String playerId, String gameId, int turn);

    /** Idempotent, auch wenn zwei Tabs dieselbe Schlacht gleichzeitig bestätigen. */
    @Modifying
    @Query(value = "insert into battle_acknowledgements (player_id, game_id, turn, event_index, acknowledged_at) "
            + "values (:playerId, :gameId, :turn, :eventIndex, :at) on conflict do nothing", nativeQuery = true)
    int insertIfAbsent(String playerId, String gameId, int turn, int eventIndex, Instant at);

    @Modifying
    @Query("delete from BattleAcknowledgementEntity ack "
            + "where ack.id.playerId = :playerId and ack.id.gameId = :gameId and ack.id.turn < :turn")
    int deleteEarlierTurns(String playerId, String gameId, int turn);

    @Modifying
    @Query("delete from BattleAcknowledgementEntity ack where ack.id.gameId = :gameId")
    int deleteByGameId(String gameId);
}
