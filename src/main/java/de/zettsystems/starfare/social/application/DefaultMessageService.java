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

    public DefaultMessageService(PresenceTracker presence, VisibilityFilter visibility, SocialBroadcaster broadcaster) {
        this.presence = presence;
        this.visibility = visibility;
        this.broadcaster = broadcaster;
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
        broadcaster.publish(new SocialEvent.DirectMessage(f, t, body, Instant.now()));
        return SendResult.DELIVERED;
    }
}
