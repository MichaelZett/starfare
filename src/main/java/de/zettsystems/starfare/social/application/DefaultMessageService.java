package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.values.DirectMessage;
import de.zettsystems.starfare.social.values.SocialEvent;
import de.zettsystems.starfare.social.values.Usernames;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
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
        String f = Usernames.normalize(from);
        String t = Usernames.normalize(to);
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
        DirectMessage message = new DirectMessage(f, t, body, Instant.now());
        store.save(message);
        broadcaster.publish(new SocialEvent.DirectMessage(message.from(), message.to(), message.text(), message.sentAt()));
        return SendResult.DELIVERED;
    }

    @Override
    public java.util.List<DirectMessage> conversation(String firstUser, String secondUser) {
        String first = Usernames.normalize(firstUser);
        String second = Usernames.normalize(secondUser);
        if (first == null || second == null || first.equals(second)) {
            return java.util.List.of();
        }
        return store.conversation(first, second);
    }
}
