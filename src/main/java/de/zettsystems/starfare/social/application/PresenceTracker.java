package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.values.UserPresence;

import java.util.List;

public interface PresenceTracker {

    void attach(String username);

    void detach(String username);

    boolean isOnline(String username);

    List<UserPresence> onlineUsers();
}
