package de.zettsystems.starfare.game.application;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GameChatRetentionRunnerTest {
    @Test
    void removesMessagesOlderThanOneYear() {
        GameChatService chat = mock(GameChatService.class);

        new GameChatRetentionRunner(chat).removeExpiredMessages();

        verify(chat).removeMessagesOlderThan(any(Instant.class));
    }
}
