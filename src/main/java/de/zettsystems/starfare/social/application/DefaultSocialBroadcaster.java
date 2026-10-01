package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.game.values.Subscription;
import de.zettsystems.starfare.social.values.SocialEvent;
import org.springframework.stereotype.Component;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Component
public class DefaultSocialBroadcaster implements SocialBroadcaster {

    private final List<Consumer<SocialEvent>> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean stopped;

    @Override
    public Subscription subscribe(Consumer<SocialEvent> listener) {
        if (!stopped) {
            listeners.add(listener);
        }
        return () -> listeners.remove(listener);
    }

    @Override
    public void publish(SocialEvent event) {
        for (Consumer<SocialEvent> l : listeners) {
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
        listeners.clear();
    }
}
