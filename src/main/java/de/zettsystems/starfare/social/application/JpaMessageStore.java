package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.domain.DirectMessageEntity;
import de.zettsystems.starfare.social.values.DirectMessage;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.Instant;
import jakarta.transaction.Transactional;
import java.util.Objects;

@Repository
class JpaMessageStore implements MessageStore {

    private final DirectMessageRepository repository;

    JpaMessageStore(DirectMessageRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(DirectMessage message) {
        repository.save(new DirectMessageEntity(message.from(), message.to(), message.text(), message.sentAt()));
    }

    @Override
    public List<DirectMessage> conversation(String firstUser, String secondUser) {
        return repository.findConversation(firstUser, secondUser, firstUser).stream()
                .map(message -> new DirectMessage(Objects.requireNonNull(message.getId(), "Persisted message must have an id"), message.getSenderPlayerId(), message.getRecipientPlayerId(),
                        message.getText(), message.getSentAt(), message.getReadAt()))
                .toList();
    }
    @Override @Transactional public void markConversationRead(String viewer, String otherUser) { repository.markConversationRead(viewer, otherUser, Instant.now()); }
    @Override @Transactional public void archiveConversation(String viewer, String otherUser) { Instant now = Instant.now(); repository.archiveSent(viewer, otherUser, now); repository.archiveReceived(viewer, otherUser, now); }
    @Override @Transactional public long deleteOlderThan(Instant cutoff) { return repository.deleteBySentAtBefore(cutoff); }
}
