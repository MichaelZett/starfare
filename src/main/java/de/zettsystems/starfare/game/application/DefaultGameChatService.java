package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameChatMessageEntity;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameChatMessage;
import de.zettsystems.starfare.game.values.GameId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.function.BiPredicate;
import org.jspecify.annotations.Nullable;

@Service
class DefaultGameChatService implements GameChatService {
    private final GameRegistry registry;
    private final GameChatMessageRepository repository;
    private final Broadcaster broadcaster;
    private final GameAccessPolicy access;

    DefaultGameChatService(GameRegistry registry, GameChatMessageRepository repository, Broadcaster broadcaster,
                           GameAccessPolicy access) {
        this.registry = registry;
        this.repository = repository;
        this.broadcaster = broadcaster;
        this.access = access;
    }

    @Override
    public List<GameChatMessage> messagesFor(GameId gameId, String account) {
        if (!allowed(gameId, account, access::mayReadChat)) {
            return List.of();
        }
        return repository.findByGameIdOrderBySentAtAscIdAsc(gameId.value()).stream()
                .map(message -> new GameChatMessage(gameId, message.getSenderPlayerId(), message.getText(), message.getSentAt()))
                .toList();
    }

    @Override
    public boolean mayWrite(GameId gameId, String account) {
        return allowed(gameId, account, access::mayWriteChat);
    }

    @Override
    public boolean send(GameId gameId, String account, String text) {
        if (!mayWrite(gameId, account)) {
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

    private boolean allowed(GameId gameId, @Nullable String account, BiPredicate<GameState, String> rule) {
        return account != null && registry.find(gameId)
                .map(_ -> registry.readState(gameId, state -> rule.test(state, account)))
                .orElse(false);
    }
}
