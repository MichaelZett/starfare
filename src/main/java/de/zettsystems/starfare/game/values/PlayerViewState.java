package de.zettsystems.starfare.game.values;

import de.zettsystems.starfare.report.values.TurnReport;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Snapshot tailored to a single player for UI rendering, including game-over state.
 * {@code empire} is {@link EmpireStats#NONE} for observers; {@code waitingFleetIds} lists the
 * player's fleets that already wait this turn or have a pending wait order.
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
        boolean battlePresentationEnabled
) {
    public PlayerViewState(int turn, List<Player> players, List<VisibleSystem> systems, List<Fleet> ownFleets,
                           @Nullable TurnReport report, boolean gameOver, @Nullable Integer winnerId,
                           List<PlannedOrder> plannedOrders, List<StandingOrderView> standingOrders,
                           EmpireStats empire, Set<Integer> waitingFleetIds) {
        this(turn, players, systems, ownFleets, report, gameOver, winnerId, plannedOrders, standingOrders, empire,
                waitingFleetIds, GameConfig.DEFAULT_BATTLE_PRESENTATION_ENABLED);
    }
}
