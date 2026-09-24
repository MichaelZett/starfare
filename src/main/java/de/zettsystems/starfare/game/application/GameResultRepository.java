package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameResultEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface GameResultRepository extends JpaRepository<GameResultEntity, String> {
    @EntityGraph(attributePaths = "participants")
    @Query("select distinct result from GameResultEntity result join result.participants participant where participant = :account")
    List<GameResultEntity> findForParticipant(String account);

    // Load the second collection separately into the same persistence context.
    @EntityGraph(attributePaths = "aiOpponentNames")
    @Query("select distinct result from GameResultEntity result where result.gameId in :ids")
    List<GameResultEntity> findWithAiOpponentsByGameIdIn(List<String> ids);
}
