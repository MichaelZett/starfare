package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface GameSessionRepository extends JpaRepository<GameSessionEntity, String> {
    List<GameSessionEntity> findAllByOrderByCreatedAtAsc();

    /** Nur die Kennungen — ohne die Snapshot-JSONs aller Partien zu laden. */
    @Query("select session.id from GameSessionEntity session order by session.createdAt asc")
    List<String> findAllIdsOrderByCreatedAt();
}
