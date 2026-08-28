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
    public void attach(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }
        boolean becameOnline = refCounts.merge(playerId, 1, Integer::sum) == 1;
        if (becameOnline) {
            broadcaster.publish(new SocialEvent.PresenceChanged(playerId, true));
        }
    }

    @Override
    public void detach(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }
        boolean[] becameOffline = {false};
        refCounts.computeIfPresent(playerId, (_, count) -> {
            if (count <= 1) {
                becameOffline[0] = true;
                return null;
            }
            return count - 1;
        });
        if (becameOffline[0]) {
            broadcaster.publish(new SocialEvent.PresenceChanged(playerId, false));
        }
    }

    @Override
    public boolean isOnline(String playerId) {
        if (playerId == null) {
            return false;
        }
        return refCounts.containsKey(playerId);
    }

    @Override
    public List<UserPresence> onlineUsers() {
        return refCounts.keySet().stream().sorted().map(UserPresence::new).toList();
    }
}
