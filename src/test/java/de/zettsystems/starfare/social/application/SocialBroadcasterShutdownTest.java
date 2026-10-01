package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.values.SocialEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SocialBroadcasterShutdownTest {
    @Test
    void contextCloseStopsPresenceUpdatesBeforeUiDetach() {
        var context = new AnnotationConfigApplicationContext(DefaultSocialBroadcaster.class, DefaultPresenceTracker.class);
        var broadcaster = context.getBean(SocialBroadcaster.class);
        var presence = context.getBean(PresenceTracker.class);
        List<SocialEvent> received = new ArrayList<>();
        broadcaster.subscribe(received::add);
        presence.attach("alice");
        context.close();

        presence.detach("alice");
        broadcaster.subscribe(received::add);
        broadcaster.publish(new SocialEvent.PresenceChanged("bob", true));

        assertThat(presence.isOnline("alice")).isFalse();
        assertThat(received).containsExactly(new SocialEvent.PresenceChanged("alice", true));
    }
}
