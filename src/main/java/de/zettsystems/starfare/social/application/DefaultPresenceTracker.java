package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.values.SocialEvent;
import de.zettsystems.starfare.social.values.UserPresence;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DefaultPresenceTracker implements PresenceTracker {

    private final Map<String, Integer> refCounts = new ConcurrentHashMap<>();
    private final SocialBroadcaster broadcaster;

    public DefaultPresenceTracker(SocialBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @Override
    public void attach(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        boolean becameOnline = refCounts.merge(username, 1, Integer::sum) == 1;
        if (becameOnline) {
            broadcaster.publish(new SocialEvent.PresenceChanged(username, true));
        }
    }

    @Override
    public void detach(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        boolean[] becameOffline = {false};
        refCounts.computeIfPresent(username, (_, count) -> {
            if (count <= 1) {
                becameOffline[0] = true;
                return null;
            }
            return count - 1;
        });
        if (becameOffline[0]) {
            broadcaster.publish(new SocialEvent.PresenceChanged(username, false));
        }
    }

    @Override
    public boolean isOnline(String username) {
        if (username == null) {
            return false;
        }
        return refCounts.containsKey(username);
    }

    @Override
    public List<UserPresence> onlineUsers() {
        return refCounts.keySet().stream().sorted().map(UserPresence::new).toList();
    }
}
