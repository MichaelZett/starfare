package de.zettsystems.starfare.game.values;

import de.zettsystems.starfare.report.values.TurnReport;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Snapshot tailored to a single player for UI rendering, including game-over state.
 * {@code empire} is {@link EmpireStats#NONE} for observers; {@code waitingFleetIds} lists the
 * player's fleets that already wait this turn or have a pending wait order; {@code roundStatus}
 * shows who has submitted the running round and when it ends; {@code plannedDepartures} lists the player's
 * planned later starts of joint attacks.
 */
public record PlayerViewState(
        int turn,
        List<Player> players,
        List<VisibleSystem> systems,
        List<Fleet> ownFleets,
        @Nullable TurnReport report,
        boolean gameOver,
        @Nullable Integer winnerId,
        List<PlannedOrder> plannedOrders,
        List<StandingOrderView> standingOrders,
        EmpireStats empire,
        Set<Integer> waitingFleetIds,
        boolean battlePresentationEnabled,
        RoundStatus roundStatus,
        @Nullable GameOutcome outcome,
        SkyMarks sky,
        GalaxyExtent extent,
        List<PlannedDeparture> plannedDepartures) {
    public PlayerViewState(int turn, List<Player> players, List<VisibleSystem> systems, List<Fleet> ownFleets,
                           @Nullable TurnReport report, boolean gameOver, @Nullable Integer winnerId,
                           List<PlannedOrder> plannedOrders, List<StandingOrderView> standingOrders, EmpireStats empire,
                           Set<Integer> waitingFleetIds, boolean battlePresentationEnabled, RoundStatus roundStatus,
                           @Nullable GameOutcome outcome, SkyMarks sky, GalaxyExtent extent) {
        this(turn, players, systems, ownFleets, report, gameOver, winnerId, plannedOrders, standingOrders, empire,
                waitingFleetIds, battlePresentationEnabled, roundStatus, outcome, sky, extent, List.of());
    }
    public PlayerViewState(int turn, List<Player> players, List<VisibleSystem> systems, List<Fleet> ownFleets,
                           @Nullable TurnReport report, boolean gameOver, @Nullable Integer winnerId,
                           List<PlannedOrder> plannedOrders, List<StandingOrderView> standingOrders, EmpireStats empire,
                           Set<Integer> waitingFleetIds, boolean battlePresentationEnabled, RoundStatus roundStatus,
                           @Nullable GameOutcome outcome, SkyMarks sky) {
        this(turn, players, systems, ownFleets, report, gameOver, winnerId, plannedOrders, standingOrders, empire,
                waitingFleetIds, battlePresentationEnabled, roundStatus, outcome, sky, GalaxyExtent.LEGACY);
    }
    public PlayerViewState(int turn, List<Player> players, List<VisibleSystem> systems, List<Fleet> ownFleets,
                           @Nullable TurnReport report, boolean gameOver, @Nullable Integer winnerId,
                           List<PlannedOrder> plannedOrders, List<StandingOrderView> standingOrders, EmpireStats empire,
                           Set<Integer> waitingFleetIds, boolean battlePresentationEnabled, RoundStatus roundStatus,
                           @Nullable GameOutcome outcome) {
        this(turn, players, systems, ownFleets, report, gameOver, winnerId, plannedOrders, standingOrders, empire,
                waitingFleetIds, battlePresentationEnabled, roundStatus, outcome, SkyMarks.NONE);
    }
    public PlayerViewState(
        int turn,
        List<Player> players,
        List<VisibleSystem> systems,
        List<Fleet> ownFleets,
        @Nullable TurnReport report,
        boolean gameOver,
        @Nullable Integer winnerId,
        List<PlannedOrder> plannedOrders,
        List<StandingOrderView> standingOrders,
        EmpireStats empire,
        Set<Integer> waitingFleetIds,
        boolean battlePresentationEnabled,
        RoundStatus roundStatus
) {
        this(turn, players, systems, ownFleets, report, gameOver, winnerId, plannedOrders, standingOrders, empire, waitingFleetIds, battlePresentationEnabled, roundStatus, new GameOutcome(winnerId, null));
    }

    public boolean wonBy(@Nullable Integer player) { return outcome != null && outcome.wonBy(player); }

    public PlayerViewState(int turn, List<Player> players, List<VisibleSystem> systems, List<Fleet> ownFleets,
                           @Nullable TurnReport report, boolean gameOver, @Nullable Integer winnerId,
                           List<PlannedOrder> plannedOrders, List<StandingOrderView> standingOrders,
                           EmpireStats empire, Set<Integer> waitingFleetIds) {
        this(turn, players, systems, ownFleets, report, gameOver, winnerId, plannedOrders, standingOrders, empire,
                waitingFleetIds, GameConfig.DEFAULT_BATTLE_PRESENTATION_ENABLED, RoundStatus.NONE);
    }
}
