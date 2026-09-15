package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameArchiveEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

interface GameArchiveRepository extends JpaRepository<GameArchiveEntity, String> {
    List<GameArchiveEntity> findAllByFinishedAtBeforeOrderByFinishedAtAsc(Instant cutoff);
}
