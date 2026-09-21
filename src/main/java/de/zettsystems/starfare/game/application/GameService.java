package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GameService {
    List<GameSummary> visibleGamesFor(String account, GameListScope scope);
    Optional<GameSummary> summaryFor(GameId id, String account);
    boolean changeVisibility(GameId id, String actor, GameVisibility visibility);
    Optional<PlayerViewState> reviewFor(GameId id, String account);

    Optional<PlayerViewState> reviewFor(GameId id, String account, int perspective, boolean fogOfWar);
    List<Integer> replayTurns(GameId id, String account);
    Optional<PlayerViewState> replayFor(GameId id, String account, int perspective, int turn);
    Optional<PlayerViewState> viewForAccount(GameId id, String account);

    Optional<GameOutcomeStatistics> outcomeStatisticsFor(GameId id, String account);


    GameId newGame(GameSetup setup, @Nullable String hostPlayerId, String name);

    GameId newGame(GameSetup setup);

    List<GameId> listGames();

    default List<GameId> loadedGames() {
        return listGames();
    }

    String gameNameOf(GameId gameId);

    void abortGame(GameId gameId);

    boolean abortGame(GameId gameId, @Nullable String actorPlayerId);

    Optional<String> hostPlayerIdOf(GameId gameId);

    boolean canAbort(GameId gameId, @Nullable String playerId);

    boolean hasActiveGame(GameId gameId);

    boolean hasStartedGame(GameId gameId);

    boolean joinGame(GameId gameId, int playerId);

    Optional<Integer> joinGame(GameId gameId, @Nullable String playerId);

    Optional<Integer> joinGame(GameId gameId, @Nullable String playerId, String playerName, String empireName);

    Optional<Integer> seatFor(GameId gameId, @Nullable String playerId);

    Optional<Integer> inviteUser(GameId gameId, @Nullable String inviteePlayerId);

    Optional<Integer> revokeInvite(GameId gameId, @Nullable String inviteePlayerId);

    Map<String, Integer> invitedSeatsOf(GameId gameId);

    Optional<Integer> seatReservedFor(GameId gameId, @Nullable String inviteePlayerId);

    boolean canStartGame(GameId gameId);

    boolean startGame(GameId gameId);

    PlayerViewState viewFor(GameId gameId, int playerId);

    PlayerViewState viewForObserver(GameId gameId);

    /** Read-only running-game view from a selected player's perspective. */
    Optional<PlayerViewState> observerViewFor(GameId gameId, String account, int perspective, boolean fogOfWar);

    boolean observeGame(GameId gameId, @Nullable String playerId);

    boolean leaveObserve(GameId gameId, @Nullable String playerId);

    boolean advanceForObserver(GameId gameId, @Nullable String playerId);

    boolean isAiOnly(GameId gameId);

    boolean isObserver(GameId gameId, @Nullable String playerId);

    boolean observersAllowed(GameId gameId);

    /** Lobby-level read model of the game; the UI never sees the mutable {@code GameState}. */
    GameSummary summaryOf(GameId gameId);

    int travelTurns(GameId gameId, int fromId, int toId);

    boolean sendFleet(GameId gameId, int playerId, int fromId, int toId, int ships);

    /** Sets the permanent garrison that remains at an owned system. */
    boolean setGarrisonReserve(GameId gameId, int playerId, int systemId, int reserve);

    boolean addStandingOrder(GameId gameId, int playerId, int fromId, int toId, int ships);

    /** Groesse, die eine Verlegung von {@code fromId} nach {@code toId} hoechstens haben darf. */
    int routingHeadroom(GameId gameId, int playerId, int fromId, int toId);

    boolean removeStandingOrder(GameId gameId, int playerId, int orderId);

    boolean removeStandingOrderFrom(GameId gameId, int playerId, int fromSystemId);

    boolean submitTurn(GameId gameId, int playerId);

    /**
     * Beendet die Runde, wenn ihre Frist (Runden- oder Nachzügler-Limit) abgelaufen ist. Wer nicht
     * abgegeben hat, zieht mit den bis dahin erteilten Befehlen; nach
     * {@link GameConfig#MAX_MISSED_ROUNDS} verpassten Fristen in Folge übernimmt die KI den Sitz.
     */
    boolean enforceRoundDeadline(GameId gameId, Instant now);

    int unloadInactiveSingleHumanGames();

    boolean kickHuman(GameId gameId, @Nullable String actorPlayerId, int seatId);

    boolean leaveGame(GameId gameId, int playerId);

    boolean hasSignificantEventsFor(GameId gameId, int playerId);

    boolean isWaitingForOtherPlayers(GameId gameId, int playerId);

    boolean setFleetWait(GameId gameId, int playerId, int fleetId);

    boolean cancelOrder(GameId gameId, int playerId, int orderIndex);

    boolean disbandFleet(GameId gameId, int playerId, int fleetId);
}
