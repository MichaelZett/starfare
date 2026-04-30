package de.zettsystems.starfare.social.values;

import de.zettsystems.starfare.game.values.GameId;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Events fanned out by {@link de.zettsystems.starfare.social.application.SocialBroadcaster}.
 */
public sealed interface SocialEvent {

    record PresenceChanged(String username, boolean online) implements SocialEvent {
    }

    record FriendRequestReceived(String from, String to) implements SocialEvent {
    }

    record FriendshipUpdated(String userA, String userB, @Nullable FriendshipStatus status) implements SocialEvent {
    }

    record VisibilityUpdated(String username, Visibility visibility) implements SocialEvent {
    }

    record DirectMessage(String from, String to, String text, Instant sentAt) implements SocialEvent {
    }

    record InviteReceived(GameId gameId, String from, String to, int seatId) implements SocialEvent {
    }

    record InviteWithdrawn(GameId gameId, String from, String to) implements SocialEvent {
    }

    record InviteAccepted(GameId gameId, String invitee, int seatId) implements SocialEvent {
    }

    record InviteDeclined(GameId gameId, String invitee) implements SocialEvent {
    }
}
