package de.zettsystems.starfare.social.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Removes direct messages after the documented one-year retention period. */
@Component
class MessageRetentionRunner {
    private final MessageService messages;

    MessageRetentionRunner(MessageService messages) {
        this.messages = messages;
    }

    @Scheduled(cron = "0 0 3 * * *")
    void removeExpiredMessages() {
        messages.removeExpiredMessages();
    }
}
