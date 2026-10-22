package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.economy.domain.Industry;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.GameStateSnapshot;
import java.util.Map;
import java.util.Objects;

/** A test-only round-boundary intervention, restored through ordinary snapshot validation. */
final class InvestmentSnapshots {
    private InvestmentSnapshots() { }
    static GameState industry(GameState state, Map<Integer, Industry> industries) {
        var s = GameState.toSnapshot(state);
        var systems = s.systems().stream().map(system -> {
            if (system.neutral()) { return system; }
            var industry = Objects.requireNonNull(industries.get(system.id()));
            int mining = Objects.requireNonNull(s.metals()).mining().get(system.id());
            return system.planShipbuilding(industry.shipbuilding() - mining);
        }).toList();
        return GameState.fromSnapshot(new GameStateSnapshot(
                s.turn(), s.nextGlobalFleetId(), s.nextLocalFleetNo(), s.players(),
                systems, s.fleets(), s.reports(), s.intel(),
                s.waitThisTurn(), s.submittedThisTurn(), s.gameOver(), s.winnerId(),
                s.active(), s.started(), s.joinedHumanPlayerIds(), s.originalHumanPlayerIds(),
                s.observers(), s.seatByUser(), s.invitedSeats(), s.pendingOrders(),
                s.standingOrders(), s.nextStandingOrderId(), s.observersAllowed(), s.reentryAllowed(),
                s.turnStartedAt(), s.visibility(), s.finishedAt(), s.ownershipHistory(),
                Map.of(), s.battlePresentationEnabled(), s.combatRandomnessPercent(), s.roundRules(),
                s.stragglerSince(), s.missedRounds(), s.victorySystemPercent(), s.originalSetup(),
                s.previousOrders(), s.ruleset(), industries, s.navigation(),
                s.diplomacy(), s.victoryRules(), s.outcome(), s.colonies(),
                s.metals()));
    }
}
