package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.fleet.application.FleetService;
import de.zettsystems.starfare.fleet.values.ScheduledDeparture;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.navigation.domain.Routes;
import de.zettsystems.starfare.report.application.ReportService;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnEvent.JointDeparture.Outcome;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Planned later starts of joint attacks during round resolution. Without any planned start both steps do nothing,
 * so games that never use joint attacks resolve exactly as before.
 */
@org.springframework.stereotype.Component
final class JointDepartures {
    private final FleetService fleets;
    private final ReportService reports;

    JointDepartures(FleetService fleets, ReportService reports) {
        this.fleets = fleets;
        this.reports = reports;
    }

    /**
     * Launches every start due this round, at the step where queued sends become fleets. A start sends what is
     * left if the garrison shrank; it is cancelled with a report line when nothing is left, the source changed
     * hands or the target can no longer be reached. A target that has become the player's own or an ally's is
     * still flown to, like any fleet already under way.
     */
    void launchDue(GameState state) {
        if (state.scheduledDepartures().isEmpty()) { return; }
        List<ScheduledDeparture> due = new ArrayList<>();
        state.scheduledDepartures().removeIf(departure -> departure.departureTurn() <= state.turn() && due.add(departure));
        for (ScheduledDeparture departure : due) {
            launch(state, departure);
        }
    }

    /** After the battles of a round: starts whose source is no longer the planner's are cancelled right away. */
    void cancelLost(GameState state) {
        if (state.scheduledDepartures().isEmpty()) { return; }
        List<ScheduledDeparture> lost = new ArrayList<>();
        state.scheduledDepartures().removeIf(departure -> !ownsSource(state, departure) && lost.add(departure));
        lost.forEach(departure -> report(state, departure, 0, Outcome.SOURCE_LOST));
    }

    private void launch(GameState state, ScheduledDeparture departure) {
        if (!ownsSource(state, departure)) {
            report(state, departure, 0, Outcome.SOURCE_LOST);
            return;
        }
        int owner = departure.ownerId();
        int from = departure.fromSystemId();
        int to = departure.toSystemId();
        if (state.findSystem(to).isEmpty() || state.destroyed(to) || Routes.plan(state, owner, from, to).isEmpty()) {
            report(state, departure, 0, Outcome.NO_ROUTE);
            return;
        }
        int ships = Math.min(departure.ships(), state.getSystem(from).garrison());
        if (ships <= 0) {
            report(state, departure, 0, Outcome.NO_SHIPS);
            return;
        }
        boolean launched = state.ruleset().isSpaceward()
                ? fleets.sendFleet(state, owner, from, to, ships, departure.beneficiaryId())
                : fleets.sendFleet(state, owner, from, to, ships);
        if (!launched) {
            report(state, departure, 0, Outcome.NO_ROUTE);
        } else if (ships < departure.ships()) {
            report(state, departure, ships, Outcome.REDUCED);
        }
    }

    private static boolean ownsSource(GameState state, ScheduledDeparture departure) {
        return state.findSystem(departure.fromSystemId())
                .map(StarSystem::ownerId).filter(owner -> Objects.equals(owner, departure.ownerId())).isPresent();
    }

    private void report(GameState state, ScheduledDeparture departure, int sent, Outcome outcome) {
        String source = state.findSystem(departure.fromSystemId()).map(StarSystem::name).orElse("?");
        String target = state.findSystem(departure.toSystemId()).map(StarSystem::name).orElse("?");
        reports.appendEvent(state, departure.ownerId(), new TurnEvent.JointDeparture(departure.ownerId(),
                departure.fromSystemId(), source, departure.toSystemId(), target, departure.ships(), sent, outcome));
    }
}
