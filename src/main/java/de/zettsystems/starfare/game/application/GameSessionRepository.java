package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface GameSessionRepository extends JpaRepository<GameSessionEntity, String> {
    List<GameSessionEntity> findAllByOrderByCreatedAtAsc();
}
