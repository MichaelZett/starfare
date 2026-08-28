package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.AbstractRepositoryTest;
import de.zettsystems.starfare.social.domain.DirectMessageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DirectMessageRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private DirectMessageRepository repository;

    @BeforeEach
    void clearMessages() {
        repository.deleteAll();
    }

    @Test
    void findsBothDirectionsInChronologicalOrder() {
        Instant base = Instant.parse("2026-08-27T10:00:00Z");
        repository.save(new DirectMessageEntity("alice", "bob", "first", base));
        repository.save(new DirectMessageEntity("eve", "bob", "unrelated", base.plusSeconds(1)));
        repository.save(new DirectMessageEntity("bob", "alice", "second", base.plusSeconds(2)));

        assertThat(repository.findConversation("bob", "alice"))
                .extracting(DirectMessageEntity::getText)
                .containsExactly("first", "second");
    }
}
