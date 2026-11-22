package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

interface GameChatMessageRepository extends JpaRepository<GameChatMessageEntity, Long> {
    long deleteBySentAtBefore(Instant cutoff);
}
