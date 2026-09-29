package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameChatMessageEntity;
import de.zettsystems.starfare.game.values.GameChatMessage;
import de.zettsystems.starfare.game.values.GameId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
class DefaultGameChatService implements GameChatService {
    private final GameRegistry registry;
    private final GameChatMessageRepository repository;
    private final Broadcaster broadcaster;

    DefaultGameChatService(GameRegistry registry, GameChatMessageRepository repository, Broadcaster broadcaster) {
        this.registry = registry;
        this.repository = repository;
        this.broadcaster = broadcaster;
    }

    @Override
    public List<GameChatMessage> messagesFor(GameId gameId, String account) {
        if (!mayUseChat(gameId, account)) {
            return List.of();
        }
        return repository.findByGameIdOrderBySentAtAscIdAsc(gameId.value()).stream()
                .map(message -> new GameChatMessage(gameId, message.getSenderPlayerId(), message.getText(), message.getSentAt()))
                .toList();
    }

    @Override
    public boolean send(GameId gameId, String account, String text) {
        if (!mayUseChat(gameId, account)) {
            return false;
        }
        String body = text == null ? "" : text.strip();
        if (body.isEmpty()) {
            return false;
        }
        if (body.length() > GameChatMessage.MAX_LENGTH) {
            body = body.substring(0, GameChatMessage.MAX_LENGTH);
        }
        Instant sentAt = Instant.now();
        repository.save(new GameChatMessageEntity(gameId.value(), account, body, sentAt));
        broadcaster.publish(new GameEvent.ChatMessage(gameId, account, body, sentAt));
        return true;
    }

    @Override
    public long removeMessagesOlderThan(Instant cutoff) {
        return repository.deleteBySentAtBefore(cutoff);
    }

    private boolean mayUseChat(GameId gameId, String account) {
        return account != null && !account.isBlank() && registry.find(gameId)
                .map(session -> registry.readState(gameId, state -> state.active() && state.started()
                        && state.seatByUser().containsKey(account)))
                .orElse(false);
    }
}
