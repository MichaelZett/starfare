package de.zettsystems.starfare.game.values;

import de.zettsystems.starfare.report.values.TurnReport;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Snapshot tailored to a single player for UI rendering, including game-over state.
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
        List<StandingOrderView> standingOrders
) { }
