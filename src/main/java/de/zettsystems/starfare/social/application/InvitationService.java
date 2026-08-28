package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.game.values.GameId;

import java.util.Optional;

public interface InvitationService {

    Optional<Integer> inviteUser(GameId gameId, String hostPlayerId, String inviteePlayerId);

    boolean revokeInvite(GameId gameId, String hostPlayerId, String inviteePlayerId);

    boolean acceptInvite(GameId gameId, String inviteePlayerId);

    boolean declineInvite(GameId gameId, String inviteePlayerId);
}
