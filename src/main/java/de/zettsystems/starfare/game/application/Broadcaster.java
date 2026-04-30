package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.Subscription;

import java.util.function.Consumer;

public interface Broadcaster {

    Subscription subscribe(GameId gameId, Consumer<GameEvent> listener);

    Subscription subscribeAll(Consumer<GameEvent> listener);

    void publish(GameEvent event);
}
