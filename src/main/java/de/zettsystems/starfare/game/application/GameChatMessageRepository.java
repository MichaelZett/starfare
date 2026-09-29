package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.Instant;

interface GameChatMessageRepository extends JpaRepository<GameChatMessageEntity, Long> {
    List<GameChatMessageEntity> findByGameIdOrderBySentAtAscIdAsc(String gameId);
    long deleteBySentAtBefore(Instant cutoff);
}
