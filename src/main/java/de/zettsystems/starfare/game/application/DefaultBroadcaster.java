package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.Subscription;
import org.springframework.stereotype.Component;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Component
public class DefaultBroadcaster implements Broadcaster {
    private final Map<GameId, List<Consumer<GameEvent>>> perGame = new ConcurrentHashMap<>();
    private final List<Consumer<GameEvent>> global = new CopyOnWriteArrayList<>();
    private volatile boolean stopped;

    @Override
    public Subscription subscribe(GameId gameId, Consumer<GameEvent> listener) {
        List<Consumer<GameEvent>> list = perGame.computeIfAbsent(gameId, _ -> new CopyOnWriteArrayList<>());
        if (!stopped) {
            list.add(listener);
        }
        return () -> list.remove(listener);
    }

    @Override
    public Subscription subscribeAll(Consumer<GameEvent> listener) {
        if (!stopped) {
            global.add(listener);
        }
        return () -> global.remove(listener);
    }

    @Override
    public void publish(GameEvent event) {
        List<Consumer<GameEvent>> list = perGame.get(event.gameId());
        if (list != null) {
            for (Consumer<GameEvent> l : list) {
                if (stopped) {
                    return;
                }
                l.accept(event);
            }
        }
        for (Consumer<GameEvent> l : global) {
            if (stopped) {
                return;
            }
            l.accept(event);
        }
    }

    @EventListener(ContextClosedEvent.class)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    void stopPublishing() {
        stopped = true;
        perGame.clear();
        global.clear();
    }
}
