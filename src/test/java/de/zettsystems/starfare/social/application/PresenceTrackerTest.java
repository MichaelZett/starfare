package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.values.SocialEvent;
import de.zettsystems.starfare.social.values.UserPresence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PresenceTrackerTest {

    private SocialBroadcaster broadcaster;
    private List<SocialEvent> events;
    private PresenceTracker tracker;

    @BeforeEach
    void setUp() {
        broadcaster = new DefaultSocialBroadcaster();
        events = new ArrayList<>();
        broadcaster.subscribe(events::add);
        tracker = new DefaultPresenceTracker(broadcaster);
    }

    @Test
    void attachPublishesPresenceChangedOnFirstConnection() {
        tracker.attach("alice");

        assertThat(tracker.isOnline("alice")).isTrue();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst()).isEqualTo(new SocialEvent.PresenceChanged("alice", true));
    }

    @Test
    void secondAttachDoesNotRepublish() {
        tracker.attach("alice");
        tracker.attach("alice");

        assertThat(events).hasSize(1);
    }

    @Test
    void detachPublishesPresenceChangedOnLastDisconnect() {
        tracker.attach("alice");
        tracker.attach("alice");
        tracker.detach("alice");

        assertThat(tracker.isOnline("alice")).isTrue();
        assertThat(events).hasSize(1);

        tracker.detach("alice");

        assertThat(tracker.isOnline("alice")).isFalse();
        assertThat(events).hasSize(2);
        assertThat(events.get(1)).isEqualTo(new SocialEvent.PresenceChanged("alice", false));
    }

    @Test
    void detachOfUnknownUserIsNoOp() {
        tracker.detach("ghost");

        assertThat(events).isEmpty();
        assertThat(tracker.isOnline("ghost")).isFalse();
    }

    @Test
    void onlineUsersReturnsSortedSnapshot() {
        tracker.attach("bob");
        tracker.attach("alice");
        tracker.attach("carol");

        List<String> names = tracker.onlineUsers().stream().map(UserPresence::username).toList();
        assertThat(names).containsExactlyElementsOf(List.of("alice", "bob", "carol"));
    }

    @Test
    void blankUsernamesAreIgnored() {
        tracker.attach("");
        tracker.attach(null);
        tracker.detach("");

        assertThat(events).isEmpty();
    }
}
