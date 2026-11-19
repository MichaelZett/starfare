package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.diplomacy.values.DiplomacyView;
import de.zettsystems.starfare.diplomacy.values.DiplomacyOrder;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.navigation.values.RoutePreview;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public interface GameService {
    Optional<de.zettsystems.starfare.economy.values.MiningAutomationView> miningAutomationFor(GameId id, String account);
    Optional<de.zettsystems.starfare.economy.values.MiningPlan> previewMining(GameId id, String account,
            de.zettsystems.starfare.economy.values.MiningRequest request);
    boolean configureMining(GameId id, String account, de.zettsystems.starfare.economy.values.MiningRequest request);
    boolean protectMiningSystem(GameId id, String account, int turn, int system, boolean protect);
    Optional<de.zettsystems.starfare.research.values.ResearchView> shipResearchFor(GameId id, String account);
    boolean planShipResearch(GameId id, String account, int turn, int expectedPercent, int percent);

    Optional<de.zettsystems.starfare.economy.values.ProductionPlanPreview> previewProduction(
            GameId id, String account, de.zettsystems.starfare.economy.values.ProductionAllocation plan);

    de.zettsystems.starfare.economy.values.ProductionCommit commitProduction(
            GameId id, String account, de.zettsystems.starfare.economy.values.ProductionAllocation plan);

    Optional<DiplomacyView> diplomacyFor(GameId id, String account);
    boolean diplomacyOrder(GameId id, String account, int turn, DiplomacyOrder order);
    boolean planRecycling(GameId id, int player, int turn, int system, int ships);

    boolean allocateProduction(GameId id, int playerId, int expectedTurn, int systemId, int expansion, int mining);
    boolean allocateExpansion(GameId id, int playerId, int expectedTurn, int systemId, int points);

    de.zettsystems.starfare.game.values.RulesetCatalog rulesets();

    boolean editOrder(GameId gameId, int playerId, int expectedTurn, PlannedOrder expected, int target, int ships);
    boolean undoOrders(GameId gameId, int playerId, int expectedTurn);
    boolean dispatchFleets(GameId gameId, int playerId, int expectedTurn, int target,
                          List<de.zettsystems.starfare.fleet.values.FleetDispatch> dispatches);
    boolean setReserves(GameId gameId, int playerId, int expectedTurn, List<Integer> systems, int reserve, boolean production);
    List<GameSummary> visibleGamesFor(String account, GameListScope scope);
    Optional<GameSummary> summaryFor(GameId id, String account);
    boolean changeVisibility(GameId id, String actor, GameVisibility visibility);
    Optional<PlayerViewState> reviewFor(GameId id, String account);

    Optional<PlayerViewState> reviewFor(GameId id, String account, int perspective, boolean fogOfWar);
    List<Integer> replayTurns(GameId id, String account);
    List<ReplayEventMarker> replayEventMarkers(GameId id, String account, int perspective);
    Optional<PlayerViewState> replayFor(GameId id, String account, int perspective, int turn);
    Optional<PlayerViewState> viewForAccount(GameId id, String account);

    Optional<GameOutcomeStatistics> outcomeStatisticsFor(GameId id, String account);

    /** Whether {@link #continueAfterVictory} would succeed: the account won and full conquest is not yet the goal. */
    boolean canContinueAfterVictory(GameId gameId, String account);

    /** Lets the recorded winner continue the running match towards full conquest. */
    boolean continueAfterVictory(GameId gameId, String account);

    /** Current host-controlled round rules, visible only to a game participant. */
    Optional<RoundRules> roundRulesFor(GameId gameId, String account);

    /** Changes the two running round deadlines; only the current host may do so. */
    boolean updateRoundRules(GameId gameId, String actor, RoundRules rules);

    GameId newGame(GameSetup setup, @Nullable String hostPlayerId, String name);

    GameId newGame(GameSetup setup);

    List<GameId> listGames();

    List<GameId> loadedGames();

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

    /**
     * Starts without asking who starts: for internal runs (single-player start in the wizard,
     * tests, fixtures). Anything a person triggers goes through {@link #startGame(GameId, String)}.
     */
    boolean startGame(GameId gameId);

    /** Whether {@code actor} may start the game now: only its host, once all human seats are taken. */
    boolean canStartGame(GameId gameId, @Nullable String actor);

    /** Starts the game on behalf of {@code actor}; refused for anyone but the host. */
    boolean startGame(GameId gameId, @Nullable String actor);

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
    java.util.Optional<RoutePreview> routeFor(GameId id, int player, int from, int to);

    /** Direct range and target reachability around a source during target selection; empty without range rules. */
    java.util.Optional<de.zettsystems.starfare.navigation.values.RangeView> rangeFor(GameId id, int player, int from);

    boolean sendFleet(GameId gameId, int playerId, int fromId, int toId, int ships);

    boolean sendFleet(GameId gameId, int playerId, int expectedTurn, int fromId, int toId, int ships, int beneficiary);
    List<Player> conquestCandidates(GameId gameId, int playerId);

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
     * {@link GameConfig#MAX_MISSED_ROUNDS} verpassten Fristen in derselben Partie (nicht nur in Folge)
     * übernimmt die KI den Sitz endgültig, ohne Wiedereinstieg.
     */
    boolean enforceRoundDeadline(GameId gameId, Instant now);

    int unloadInactiveSingleHumanGames();

    boolean kickHuman(GameId gameId, @Nullable String actorPlayerId, int seatId);

    /** Gives up for good: the computer takes over the seat and the player cannot reclaim it. */
    boolean resignGame(GameId gameId, int playerId);

    boolean leaveGame(GameId gameId, int playerId);

    boolean hasSignificantEventsFor(GameId gameId, int playerId);

    boolean isWaitingForOtherPlayers(GameId gameId, int playerId);

    boolean setFleetWait(GameId gameId, int playerId, int fleetId);

    boolean cancelOrder(GameId gameId, int playerId, int orderIndex);

    boolean disbandFleet(GameId gameId, int playerId, int fleetId);
}
