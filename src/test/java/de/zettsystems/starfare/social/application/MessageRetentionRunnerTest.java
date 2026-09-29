package de.zettsystems.starfare.social.application;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MessageRetentionRunnerTest {
    @Test
    void removesExpiredDirectMessages() {
        MessageService messages = mock(MessageService.class);

        new MessageRetentionRunner(messages).removeExpiredMessages();

        verify(messages).removeExpiredMessages();
    }
}
