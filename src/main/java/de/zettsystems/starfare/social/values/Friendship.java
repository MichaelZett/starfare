package de.zettsystems.starfare.social.values;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Read-only view of a friendship between two users. {@code playerA} and {@code playerB} are
 * ordered ascending so each pair has a single canonical representation. {@code requestedBy}
 * identifies the initiator for {@link FriendshipStatus#PENDING} or the blocker for
 * {@link FriendshipStatus#BLOCKED}; {@code null} once {@link FriendshipStatus#ACCEPTED}.
 */
public record Friendship(String playerA, String playerB, FriendshipStatus status, @Nullable String requestedBy) {

    /**
     * The other party from {@code playerId}'s perspective, or empty if {@code playerId} is not part of this friendship.
     */
    public Optional<String> otherSide(String playerId) {
        if (playerId.equals(playerA)) {
            return Optional.of(playerB);
        }
        if (playerId.equals(playerB)) {
            return Optional.of(playerA);
        }
        return Optional.empty();
    }
}
