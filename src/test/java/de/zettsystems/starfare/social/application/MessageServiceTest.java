package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.values.DirectMessage;
import de.zettsystems.starfare.social.values.SocialEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MessageServiceTest {

    private SocialBroadcaster broadcaster;
    private PresenceTracker presence;
    private FakeVisibilityFilter visibility;
    private MessageService service;
    private List<SocialEvent> published;

    @BeforeEach
    void setUp() {
        broadcaster = new DefaultSocialBroadcaster();
        published = new ArrayList<>();
        broadcaster.subscribe(published::add);
        presence = new DefaultPresenceTracker(broadcaster);
        visibility = new FakeVisibilityFilter();
        service = new DefaultMessageService(presence, visibility, broadcaster);
        published.clear();
    }

    @Test
    void deliversMessageWhenRecipientOnlineAndVisible() {
        presence.attach("bob");

        MessageService.SendResult result = service.send("alice", "bob", "hi");

        assertThat(result).isEqualTo(MessageService.SendResult.DELIVERED);
        assertThat(last()).isInstanceOf(SocialEvent.DirectMessage.class);
        SocialEvent.DirectMessage dm = (SocialEvent.DirectMessage) last();
        assertThat(dm.from()).isEqualTo("alice");
        assertThat(dm.to()).isEqualTo("bob");
        assertThat(dm.text()).isEqualTo("hi");
    }

    @Test
    void normalisesUsernamesToLowercase() {
        presence.attach("bob");

        MessageService.SendResult result = service.send(" ALICE ", "BoB", "hi");

        assertThat(result).isEqualTo(MessageService.SendResult.DELIVERED);
        assertThat(last()).isInstanceOf(SocialEvent.DirectMessage.class);
        SocialEvent.DirectMessage dm = (SocialEvent.DirectMessage) last();
        assertThat(dm.from()).isEqualTo("alice");
        assertThat(dm.to()).isEqualTo("bob");
    }

    @Test
    void rejectsWhenSendingToSelf() {
        presence.attach("alice");

        MessageService.SendResult result = service.send("alice", "alice", "hi");

        assertThat(result).isEqualTo(MessageService.SendResult.REJECTED);
        assertThat(directMessages()).isEmpty();
    }

    @Test
    void rejectsWhenFromIsNull() {
        MessageService.SendResult result = service.send(null, "bob", "hi");

        assertThat(result).isEqualTo(MessageService.SendResult.REJECTED);
    }

    @Test
    void rejectsWhenToIsBlank() {
        MessageService.SendResult result = service.send("alice", "   ", "hi");

        assertThat(result).isEqualTo(MessageService.SendResult.REJECTED);
    }

    @Test
    void emptyWhenTextBlank() {
        presence.attach("bob");

        assertThat(service.send("alice", "bob", "   ")).isEqualTo(MessageService.SendResult.EMPTY);
        assertThat(service.send("alice", "bob", null)).isEqualTo(MessageService.SendResult.EMPTY);
        assertThat(directMessages()).isEmpty();
    }

    @Test
    void recipientOfflineReturnsOfflineResult() {
        MessageService.SendResult result = service.send("alice", "bob", "hi");

        assertThat(result).isEqualTo(MessageService.SendResult.RECIPIENT_OFFLINE);
        assertThat(directMessages()).isEmpty();
    }

    @Test
    void notVisibleReturnsNotVisible() {
        presence.attach("bob");
        visibility.hide("alice", "bob");

        MessageService.SendResult result = service.send("alice", "bob", "hi");

        assertThat(result).isEqualTo(MessageService.SendResult.NOT_VISIBLE);
        assertThat(directMessages()).isEmpty();
    }

    @Test
    void overlongMessageIsTruncated() {
        presence.attach("bob");
        String tooLong = "a".repeat(DirectMessage.MAX_LENGTH + 50);

        MessageService.SendResult result = service.send("alice", "bob", tooLong);

        assertThat(result).isEqualTo(MessageService.SendResult.DELIVERED);
        assertThat(last()).isInstanceOf(SocialEvent.DirectMessage.class);
        SocialEvent.DirectMessage dm = (SocialEvent.DirectMessage) last();
        assertThat(dm.text()).hasSize(DirectMessage.MAX_LENGTH);
    }

    @Test
    void messageIsTrimmed() {
        presence.attach("bob");

        service.send("alice", "bob", "   hello   ");

        assertThat(last()).isInstanceOf(SocialEvent.DirectMessage.class);
        SocialEvent.DirectMessage dm = (SocialEvent.DirectMessage) last();
        assertThat(dm.text()).isEqualTo("hello");
    }

    private SocialEvent last() {
        return published.getLast();
    }

    private List<SocialEvent.DirectMessage> directMessages() {
        return published.stream()
                .filter(SocialEvent.DirectMessage.class::isInstance)
                .map(SocialEvent.DirectMessage.class::cast)
                .toList();
    }

    private static final class FakeVisibilityFilter implements VisibilityFilter {
        private final Set<String> hiddenPairs = new HashSet<>();

        void hide(String a, String b) {
            hiddenPairs.add(pair(a, b));
        }

        @Override
        public boolean canSee(String observer, String target) {
            if (observer == null || target == null) {
                return false;
            }
            if (observer.equalsIgnoreCase(target)) {
                return true;
            }
            return !hiddenPairs.contains(pair(observer, target));
        }

        private String pair(String a, String b) {
            String la = a.toLowerCase();
            String lb = b.toLowerCase();
            return la.compareTo(lb) <= 0 ? la + "|" + lb : lb + "|" + la;
        }
    }
}
