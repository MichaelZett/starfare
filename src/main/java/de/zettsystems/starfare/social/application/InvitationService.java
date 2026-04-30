package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.game.values.GameId;

import java.util.Optional;

public interface InvitationService {

    Optional<Integer> inviteUser(GameId gameId, String ownerUsername, String invitee);

    boolean revokeInvite(GameId gameId, String ownerUsername, String invitee);

    boolean acceptInvite(GameId gameId, String invitee);

    boolean declineInvite(GameId gameId, String invitee);
}
