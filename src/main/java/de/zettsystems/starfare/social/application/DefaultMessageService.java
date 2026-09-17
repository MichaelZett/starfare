package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.values.DirectMessage;
import de.zettsystems.starfare.social.values.PlayerIds;
import de.zettsystems.starfare.social.values.SocialEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring-injected collaborators are kept by reference for the bean's lifetime by design.")
public class DefaultMessageService implements MessageService {

    private final PresenceTracker presence;
    private final VisibilityFilter visibility;
    private final SocialBroadcaster broadcaster;
    private final MessageStore store;

    public DefaultMessageService(PresenceTracker presence, VisibilityFilter visibility, SocialBroadcaster broadcaster,
                                 MessageStore store) {
        this.presence = presence;
        this.visibility = visibility;
        this.broadcaster = broadcaster;
        this.store = store;
    }

    @Override
    public SendResult send(String from, String to, String text) {
        String f = PlayerIds.normalize(from);
        String t = PlayerIds.normalize(to);
        if (f == null || t == null || f.equals(t)) {
            return SendResult.REJECTED;
        }
        String body = text == null ? "" : text.strip();
        if (body.isEmpty()) {
            return SendResult.EMPTY;
        }
        if (body.length() > DirectMessage.MAX_LENGTH) {
            body = body.substring(0, DirectMessage.MAX_LENGTH);
        }
        if (!presence.isOnline(t)) {
            return SendResult.RECIPIENT_OFFLINE;
        }
        if (!visibility.canSee(f, t)) {
            return SendResult.NOT_VISIBLE;
        }
        DirectMessage message = new DirectMessage(0, f, t, body, Instant.now(), null);
        store.save(message);
        broadcaster.publish(new SocialEvent.DirectMessage(message.from(), message.to(), message.text(), message.sentAt()));
        return SendResult.DELIVERED;
    }

    @Override
    public List<DirectMessage> conversation(String firstUser, String secondUser) {
        String first = PlayerIds.normalize(firstUser);
        String second = PlayerIds.normalize(secondUser);
        if (first == null || second == null || first.equals(second)) {
            return List.of();
        }
        store.markConversationRead(first, second);
        return store.conversation(first, second);
    }

    @Override
    public void archiveConversation(String viewer, String otherUser) {
        String first = PlayerIds.normalize(viewer);
        String second = PlayerIds.normalize(otherUser);
        if (first != null && second != null && !first.equals(second)) { store.archiveConversation(first, second); }
    }

    @Override
    public long removeExpiredMessages() {
        return store.deleteOlderThan(Instant.now().minus(Duration.ofDays(365)));
    }
}
