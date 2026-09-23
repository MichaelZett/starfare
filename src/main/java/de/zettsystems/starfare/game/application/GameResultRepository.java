package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameResultEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface GameResultRepository extends JpaRepository<GameResultEntity, String> {
    @EntityGraph(attributePaths = {"participants", "aiOpponentNames"})
    @Query("select distinct result from GameResultEntity result join result.participants participant where participant = :account")
    List<GameResultEntity> findForParticipant(String account);
}
