package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.economy.domain.Industry;
import de.zettsystems.starfare.diplomacy.values.DiplomacyState;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.fleet.values.ScheduledDeparture;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.navigation.values.NavigationSettings;
import de.zettsystems.starfare.report.values.TurnReport;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Jackson-serializable snapshot of {@link GameState} for DB persistence.
 * Reconstructed via {@link GameState#fromSnapshot(GameStateSnapshot)}.
 */
// Older snapshots still carry "active" from the removed abort-in-place (it was always true) and the
// colonies, metals and mining automation of the removed Spaceward economy (games of those versions
// are no longer supported and are skipped on load).
@JsonIgnoreProperties({"active", "colonies", "metals", "miningAutomation"})
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
        @Nullable Integer combatRandomnessPercent,
        @Nullable RoundRules roundRules,
        @Nullable Instant stragglerSince,
        @Nullable Map<Integer, Integer> missedRounds,
        @Nullable Integer victorySystemPercent,
        @Nullable GameSetup originalSetup,
        @Nullable Map<Integer, List<FleetOrder>> previousOrders,
        @Nullable RulesetRef ruleset,
        @Nullable Map<Integer, Industry> industries,
        @Nullable NavigationSettings navigation,
        @Nullable DiplomacyState diplomacy,
        @Nullable VictoryRules victoryRules,
        @Nullable GameOutcome outcome,
        @Nullable Set<Integer> resignedSeats,
        @Nullable CosmicState cosmic,
        @Nullable Map<Integer, Integer> timedOutPlaces,
        // Games created before the map grew with the system count have no extent and keep the earlier fixed map.
        @Nullable GalaxyExtent extent,
        // Games saved before joint attacks have no planned later starts.
        @Nullable List<ScheduledDeparture> scheduledDepartures,
        @Nullable Map<Integer, List<ScheduledDeparture>> previousDepartures) {
    public GameStateSnapshot(int turn, int nextGlobalFleetId, Map<Integer, Integer> nextLocalFleetNo,
                             List<Player> players, List<StarSystem> systems, List<Fleet> fleets,
                             Map<Integer, TurnReport> reports, Map<Integer, Map<Integer, GameState.Intel>> intel,
                             Set<Integer> waitThisTurn, Set<Integer> submittedThisTurn, boolean gameOver,
                             @Nullable Integer winnerId, boolean started,
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
                submittedThisTurn, gameOver, winnerId, started, joinedHumanPlayerIds, originalHumanPlayerIds,
                observers, seatByUser, invitedSeats, pendingOrders, standingOrders, nextStandingOrderId,
                observersAllowed, reentryAllowed, turnStartedAt, visibility, finishedAt, ownershipHistory,
                replayFrames, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null);
    }
}
