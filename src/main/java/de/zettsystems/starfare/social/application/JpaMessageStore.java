package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.domain.DirectMessageEntity;
import de.zettsystems.starfare.social.values.DirectMessage;
import org.springframework.stereotype.Repository;

import java.util.List;

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
        return repository.findConversation(firstUser, secondUser).stream()
                .map(message -> new DirectMessage(message.getSenderPlayerId(), message.getRecipientPlayerId(),
                        message.getText(), message.getSentAt()))
                .toList();
    }
}
