package de.zettsystems.starfare.social.values;

import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

/**
 * Read-only view of a friendship between two users. {@code userA} and {@code userB} are lowercase
 * and ordered ascending so each pair has a single canonical representation. {@code requestedBy}
 * identifies the initiator for {@link FriendshipStatus#PENDING} or the blocker for
 * {@link FriendshipStatus#BLOCKED}; {@code null} once {@link FriendshipStatus#ACCEPTED}.
 */
public record Friendship(String userA, String userB, FriendshipStatus status, @Nullable String requestedBy) {

    /**
     * The other party from {@code username}'s perspective, or empty if {@code username} is not part of this friendship.
     */
    public Optional<String> otherSide(String username) {
        String u = username.toLowerCase(Locale.ROOT);
        if (u.equals(userA)) {
            return Optional.of(userB);
        }
        if (u.equals(userB)) {
            return Optional.of(userA);
        }
        return Optional.empty();
    }
}
