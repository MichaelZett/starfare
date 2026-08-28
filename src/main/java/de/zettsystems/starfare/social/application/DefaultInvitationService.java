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
    public Optional<Integer> inviteUser(GameId gameId, String hostPlayerId, String inviteePlayerId) {
        if (!isHost(gameId, hostPlayerId) || inviteePlayerId == null || inviteePlayerId.isBlank()) {
            return Optional.empty();
        }
        if (hostPlayerId.equals(inviteePlayerId)) {
            return Optional.empty();
        }
        if (!visibility.canSee(hostPlayerId, inviteePlayerId)) {
            return Optional.empty();
        }
        Optional<Integer> seat = games.inviteUser(gameId, inviteePlayerId);
        seat.ifPresent(s ->
                broadcaster.publish(new SocialEvent.InviteReceived(gameId, hostPlayerId, inviteePlayerId, s)));
        return seat;
    }

    @Override
    public boolean revokeInvite(GameId gameId, String hostPlayerId, String inviteePlayerId) {
        if (!isHost(gameId, hostPlayerId) || inviteePlayerId == null) {
            return false;
        }
        boolean removed = games.revokeInvite(gameId, inviteePlayerId).isPresent();
        if (removed) {
            broadcaster.publish(new SocialEvent.InviteWithdrawn(gameId, hostPlayerId, inviteePlayerId));
        }
        return removed;
    }

    @Override
    public boolean acceptInvite(GameId gameId, String inviteePlayerId) {
        if (inviteePlayerId == null || inviteePlayerId.isBlank()) {
            return false;
        }
        if (games.seatReservedFor(gameId, inviteePlayerId).isEmpty()) {
            return false;
        }
        Optional<Integer> seat = games.joinGame(gameId, inviteePlayerId);
        seat.ifPresent(s -> broadcaster.publish(new SocialEvent.InviteAccepted(gameId, inviteePlayerId, s)));
        return seat.isPresent();
    }

    @Override
    public boolean declineInvite(GameId gameId, String inviteePlayerId) {
        if (inviteePlayerId == null) {
            return false;
        }
        boolean removed = games.revokeInvite(gameId, inviteePlayerId).isPresent();
        if (removed) {
            broadcaster.publish(new SocialEvent.InviteDeclined(gameId, inviteePlayerId));
        }
        return removed;
    }

    private boolean isHost(GameId gameId, String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return false;
        }
        return games.hostPlayerIdOf(gameId)
                .map(host -> host.equals(playerId))
                .orElse(false);
    }
}
