package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.game.values.Subscription;
import de.zettsystems.starfare.social.values.SocialEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Component
public class DefaultSocialBroadcaster implements SocialBroadcaster {

    private final List<Consumer<SocialEvent>> listeners = new CopyOnWriteArrayList<>();

    @Override
    public Subscription subscribe(Consumer<SocialEvent> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    @Override
    public void publish(SocialEvent event) {
        for (Consumer<SocialEvent> l : listeners) {
            l.accept(event);
        }
    }
}
