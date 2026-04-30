package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.values.Friendship;

import java.util.List;
import java.util.Optional;

public interface FriendshipService {

    boolean request(String from, String to);

    boolean accept(String accepter, String other);

    boolean decline(String decliner, String other);

    boolean remove(String actor, String other);

    boolean block(String blocker, String other);

    boolean unblock(String actor, String other);

    Optional<Friendship> statusBetween(String user1, String user2);

    boolean areFriends(String user1, String user2);

    boolean isBlockedBetween(String user1, String user2);

    List<Friendship> incomingRequests(String username);

    List<Friendship> friendsOf(String username);
}
