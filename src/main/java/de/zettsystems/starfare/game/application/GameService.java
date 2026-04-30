package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.PlayerViewState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GameService {

    GameId newGame(GameSetup setup, @Nullable String hostUsername, String name);

    GameId newGame(GameSetup setup);

    List<GameId> listGames();

    String gameNameOf(GameId gameId);

    void abortGame(GameId gameId);

    boolean abortGame(GameId gameId, @Nullable String actorUsername);

    Optional<String> hostUsernameOf(GameId gameId);

    boolean canAbort(GameId gameId, @Nullable String username);

    boolean hasActiveGame(GameId gameId);

    boolean hasStartedGame(GameId gameId);

    boolean joinGame(GameId gameId, int playerId);

    Optional<Integer> joinGame(GameId gameId, @Nullable String username);

    Optional<Integer> seatFor(GameId gameId, @Nullable String username);

    Optional<Integer> inviteUser(GameId gameId, @Nullable String invitee);

    Optional<Integer> revokeInvite(GameId gameId, @Nullable String invitee);

    Map<String, Integer> invitedSeatsOf(GameId gameId);

    Optional<Integer> seatReservedFor(GameId gameId, @Nullable String invitee);

    boolean canStartGame(GameId gameId);

    boolean startGame(GameId gameId);

    PlayerViewState viewFor(GameId gameId, int playerId);

    PlayerViewState viewForObserver(GameId gameId);

    boolean observeGame(GameId gameId, @Nullable String username);

    boolean leaveObserve(GameId gameId, @Nullable String username);

    boolean advanceForObserver(GameId gameId, @Nullable String username);

    boolean isAiOnly(GameId gameId);

    boolean isObserver(GameId gameId, @Nullable String username);

    boolean observersAllowed(GameId gameId);

    GameState snapshot(GameId gameId);

    int travelTurns(GameId gameId, int fromId, int toId);

    boolean sendFleet(GameId gameId, int playerId, int fromId, int toId, int ships);

    boolean addStandingOrder(GameId gameId, int playerId, int fromId, int toId);

    boolean removeStandingOrder(GameId gameId, int playerId, int orderId);

    boolean removeStandingOrderFrom(GameId gameId, int playerId, int fromSystemId);

    boolean submitTurn(GameId gameId, int playerId);

    boolean kickHuman(GameId gameId, @Nullable String actorUsername, int seatId);

    boolean leaveGame(GameId gameId, int playerId);

    boolean hasSignificantEventsFor(GameId gameId, int playerId);

    boolean isWaitingForOtherPlayers(GameId gameId, int playerId);

    boolean setFleetWait(GameId gameId, int playerId, int fleetId);

    boolean cancelOrder(GameId gameId, int playerId, int orderIndex);

    boolean disbandFleet(GameId gameId, int playerId, int fleetId);
}
