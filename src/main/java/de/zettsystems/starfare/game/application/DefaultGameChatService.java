package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import org.springframework.stereotype.Service;

import java.util.function.BiPredicate;
import org.jspecify.annotations.Nullable;

@Service
class DefaultGameChatService implements GameChatService {
    private final GameRegistry registry;
    private final GameAccessPolicy access;

    DefaultGameChatService(GameRegistry registry, GameAccessPolicy access) {
        this.registry = registry;
        this.access = access;
    }

    @Override
    public boolean mayRead(GameId gameId, String account) {
        return allowed(gameId, account, access::mayReadChat);
    }

    @Override
    public boolean mayWrite(GameId gameId, String account) {
        return allowed(gameId, account, access::mayWriteChat);
    }

    private boolean allowed(GameId gameId, @Nullable String account, BiPredicate<GameState, String> rule) {
        return account != null && registry.find(gameId)
                .map(_ -> registry.readState(gameId, state -> rule.test(state, account)))
                .orElse(false);
    }
}
