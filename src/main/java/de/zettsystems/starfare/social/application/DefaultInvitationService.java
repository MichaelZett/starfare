package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.social.values.SocialEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring-injected collaborators are kept by reference for the bean's lifetime by design.")
public class DefaultInvitationService implements InvitationService {

    private final GameService games;
    private final VisibilityFilter visibility;
    private final SocialBroadcaster broadcaster;

    public DefaultInvitationService(GameService games, VisibilityFilter visibility, SocialBroadcaster broadcaster) {
        this.games = games;
        this.visibility = visibility;
        this.broadcaster = broadcaster;
    }

    @Override
    public Optional<Integer> inviteUser(GameId gameId, String ownerUsername, String invitee) {
        if (!isHost(gameId, ownerUsername) || invitee == null || invitee.isBlank()) {
            return Optional.empty();
        }
        if (ownerUsername.equalsIgnoreCase(invitee)) {
            return Optional.empty();
        }
        if (!visibility.canSee(ownerUsername, invitee)) {
            return Optional.empty();
        }
        Optional<Integer> seat = games.inviteUser(gameId, invitee);
        seat.ifPresent(s -> broadcaster.publish(new SocialEvent.InviteReceived(gameId, ownerUsername, invitee, s)));
        return seat;
    }

    @Override
    public boolean revokeInvite(GameId gameId, String ownerUsername, String invitee) {
        if (!isHost(gameId, ownerUsername) || invitee == null) {
            return false;
        }
        boolean removed = games.revokeInvite(gameId, invitee).isPresent();
        if (removed) {
            broadcaster.publish(new SocialEvent.InviteWithdrawn(gameId, ownerUsername, invitee));
        }
        return removed;
    }

    @Override
    public boolean acceptInvite(GameId gameId, String invitee) {
        if (invitee == null || invitee.isBlank()) {
            return false;
        }
        if (games.seatReservedFor(gameId, invitee).isEmpty()) {
            return false;
        }
        Optional<Integer> seat = games.joinGame(gameId, invitee);
        seat.ifPresent(s -> broadcaster.publish(new SocialEvent.InviteAccepted(gameId, invitee, s)));
        return seat.isPresent();
    }

    @Override
    public boolean declineInvite(GameId gameId, String invitee) {
        if (invitee == null) {
            return false;
        }
        boolean removed = games.revokeInvite(gameId, invitee).isPresent();
        if (removed) {
            broadcaster.publish(new SocialEvent.InviteDeclined(gameId, invitee));
        }
        return removed;
    }

    private boolean isHost(GameId gameId, String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return games.hostUsernameOf(gameId)
                .map(host -> host.equalsIgnoreCase(username))
                .orElse(false);
    }
}
