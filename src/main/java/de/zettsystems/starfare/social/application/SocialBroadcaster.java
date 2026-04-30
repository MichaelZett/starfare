package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.game.values.Subscription;
import de.zettsystems.starfare.social.values.SocialEvent;

import java.util.function.Consumer;

public interface SocialBroadcaster {

    Subscription subscribe(Consumer<SocialEvent> listener);

    void publish(SocialEvent event);
}
