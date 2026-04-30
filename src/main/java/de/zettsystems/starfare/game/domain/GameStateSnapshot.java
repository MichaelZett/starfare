package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StandingOrder;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.report.values.TurnReport;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Jackson-serializable snapshot of {@link GameState} for DB persistence.
 * Reconstructed via {@link GameState#fromSnapshot(GameStateSnapshot)}.
 */
public record GameStateSnapshot(
        int turn,
        int nextGlobalFleetId,
        Map<Integer, Integer> nextLocalFleetNo,
        List<Player> players,
        List<StarSystem> systems,
        List<Fleet> fleets,
        Map<Integer, TurnReport> reports,
        Map<Integer, Map<Integer, GameState.Intel>> intel,
        Set<Integer> waitThisTurn,
        Set<Integer> submittedThisTurn,
        boolean gameOver,
        @Nullable Integer winnerId,
        boolean active,
        boolean started,
        Set<Integer> joinedHumanPlayerIds,
        Set<Integer> originalHumanPlayerIds,
        Set<String> observers,
        Map<String, Integer> seatByUser,
        Map<Integer, List<FleetOrder>> pendingOrders,
        Map<Integer, List<StandingOrder>> standingOrders,
        Map<Integer, Integer> nextStandingOrderId,
        boolean observersAllowed,
        boolean reentryAllowed
) {}
