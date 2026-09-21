package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.report.values.TurnReport;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
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
        @Nullable Map<String, Integer> invitedSeats,
        Map<Integer, List<FleetOrder>> pendingOrders,
        @Nullable Map<Integer, List<StandingOrder>> standingOrders,
        Map<Integer, Integer> nextStandingOrderId,
        boolean observersAllowed,
        boolean reentryAllowed,
        @Nullable Instant turnStartedAt,
        @Nullable GameVisibility visibility,
        @Nullable Instant finishedAt,
        @Nullable Map<Integer, List<SystemOwnership>> ownershipHistory,
        @Nullable Map<Integer, ReplayFrame> replayFrames,
        @Nullable Boolean battlePresentationEnabled,
        @Nullable RoundRules roundRules,
        @Nullable Instant stragglerSince,
        @Nullable Map<Integer, Integer> missedRounds
) {
    public GameStateSnapshot(int turn, int nextGlobalFleetId, Map<Integer, Integer> nextLocalFleetNo,
                             List<Player> players, List<StarSystem> systems, List<Fleet> fleets,
                             Map<Integer, TurnReport> reports, Map<Integer, Map<Integer, GameState.Intel>> intel,
                             Set<Integer> waitThisTurn, Set<Integer> submittedThisTurn, boolean gameOver,
                             @Nullable Integer winnerId, boolean active, boolean started,
                             Set<Integer> joinedHumanPlayerIds, Set<Integer> originalHumanPlayerIds,
                             Set<String> observers, Map<String, Integer> seatByUser,
                             @Nullable Map<String, Integer> invitedSeats,
                             Map<Integer, List<FleetOrder>> pendingOrders,
                             @Nullable Map<Integer, List<StandingOrder>> standingOrders,
                             Map<Integer, Integer> nextStandingOrderId, boolean observersAllowed,
                             boolean reentryAllowed, @Nullable Instant turnStartedAt,
                             @Nullable GameVisibility visibility, @Nullable Instant finishedAt,
                             @Nullable Map<Integer, List<SystemOwnership>> ownershipHistory,
                             @Nullable Map<Integer, ReplayFrame> replayFrames) {
        this(turn, nextGlobalFleetId, nextLocalFleetNo, players, systems, fleets, reports, intel, waitThisTurn,
                submittedThisTurn, gameOver, winnerId, active, started, joinedHumanPlayerIds, originalHumanPlayerIds,
                observers, seatByUser, invitedSeats, pendingOrders, standingOrders, nextStandingOrderId,
                observersAllowed, reentryAllowed, turnStartedAt, visibility, finishedAt, ownershipHistory,
                replayFrames, null, null, null, null);
    }
}
