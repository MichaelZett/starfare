package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.AbstractRepositoryTest;
import de.zettsystems.starfare.social.domain.DirectMessageEntity;
import de.zettsystems.starfare.social.values.DirectMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DirectMessageRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private DirectMessageRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

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

        assertThat(repository.findConversation("bob", "alice", "bob"))
                .extracting(DirectMessageEntity::getText)
                .containsExactly("first", "second");
    }

    @Test
    @jakarta.transaction.Transactional
    void storeTracksReadAndPerUserArchiveStateAndDeletesExpiredMessages() {
        Instant old = Instant.parse("2025-01-01T00:00:00Z");
        Instant current = Instant.parse("2026-09-16T00:00:00Z");
        JpaMessageStore store = new JpaMessageStore(repository);
        store.save(new de.zettsystems.starfare.social.values.DirectMessage(0, "alice", "bob", "old", old, null));
        store.save(new de.zettsystems.starfare.social.values.DirectMessage(0, "bob", "alice", "current", current, null));

        store.markConversationRead("bob", "alice");
        entityManager.flush();
        entityManager.clear();

        assertThat(store.conversation("bob", "alice"))
                .extracting(DirectMessage::text, message -> message.readAt() != null)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("old", true),
                        org.assertj.core.groups.Tuple.tuple("current", false));

        store.archiveConversation("alice", "bob");
        entityManager.flush();
        entityManager.clear();

        assertThat(store.conversation("alice", "bob")).isEmpty();
        assertThat(store.conversation("bob", "alice")).hasSize(2);
        assertThat(store.deleteOlderThan(Instant.parse("2026-01-01T00:00:00Z"))).isOne();
        assertThat(store.conversation("bob", "alice"))
                .extracting(DirectMessage::text)
                .containsExactly("current");
    }
}
