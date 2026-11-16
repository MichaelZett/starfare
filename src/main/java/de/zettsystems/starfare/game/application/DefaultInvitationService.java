package de.zettsystems.starfare.game.application;

import de.zettsystems.gamesplatformapi.PlatformPresence;
import de.zettsystems.starfare.game.platform.StarfareMatches;

import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.EmpireNameGenerator;
import de.zettsystems.starfare.game.values.GameId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring-injected collaborators are kept by reference for the bean's lifetime by design.")
public class DefaultInvitationService implements InvitationService {

    private final GameService games;
    private final PlatformPresence presence;
    private final PlayerDirectory players;

    /**
     * @param presence who may see whom; the platform's in a host, Starfare's own in its own start
     */
    public DefaultInvitationService(GameService games, PlatformPresence presence, PlayerDirectory players) {
        this.games = games;
        this.presence = presence;
        this.players = players;
    }

    @Override
    public Optional<Integer> inviteUser(GameId gameId, String hostPlayerId, String inviteePlayerId) {
        if (!isHost(gameId, hostPlayerId) || inviteePlayerId == null || inviteePlayerId.isBlank()) {
            return Optional.empty();
        }
        if (hostPlayerId.equals(inviteePlayerId)) {
            return Optional.empty();
        }
        if (!canSee(hostPlayerId, inviteePlayerId)) {
            return Optional.empty();
        }
        return games.inviteUser(gameId, inviteePlayerId);
    }

    @Override
    public boolean revokeInvite(GameId gameId, String hostPlayerId, String inviteePlayerId) {
        if (!isHost(gameId, hostPlayerId) || inviteePlayerId == null) {
            return false;
        }
        return games.revokeInvite(gameId, inviteePlayerId).isPresent();
    }

    @Override
    public boolean acceptInvite(GameId gameId, String inviteePlayerId) {
        if (inviteePlayerId == null || inviteePlayerId.isBlank()) {
            return false;
        }
        if (games.seatReservedFor(gameId, inviteePlayerId).isEmpty()) {
            return false;
        }
        // Like joining through the dialog: the seat shows the person and an empire, not "P2".
        String name = players.displayName(inviteePlayerId);
        return games.joinGame(gameId, inviteePlayerId, name, EmpireNameGenerator.forHuman(name)).isPresent();
    }

    @Override
    public boolean declineInvite(GameId gameId, String inviteePlayerId) {
        if (inviteePlayerId == null) {
            return false;
        }
        return games.revokeInvite(gameId, inviteePlayerId).isPresent();
    }

    /** Account ids as text, as Starfare stores its players; an id that is no account sees nobody. */
    private boolean canSee(String observer, String target) {
        return StarfareMatches.account(observer)
                .flatMap(viewer -> StarfareMatches.account(target).map(other -> presence.canSee(viewer, other)))
                .orElse(false);
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
