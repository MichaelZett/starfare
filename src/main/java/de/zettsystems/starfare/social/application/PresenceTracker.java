package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.values.UserPresence;

import java.util.List;

public interface PresenceTracker {

    void attach(String playerId);

    void detach(String playerId);

    boolean isOnline(String playerId);

    List<UserPresence> onlineUsers();
}
