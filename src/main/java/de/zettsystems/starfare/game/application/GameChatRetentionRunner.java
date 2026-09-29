package de.zettsystems.starfare.game.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/** Removes party-chat messages after the documented one-year retention period. */
@Component
class GameChatRetentionRunner {
    private final GameChatService chat;

    GameChatRetentionRunner(GameChatService chat) {
        this.chat = chat;
    }

    @Scheduled(cron = "0 15 3 * * *")
    void removeExpiredMessages() {
        chat.removeMessagesOlderThan(Instant.now().minus(Duration.ofDays(365)));
    }
}
