package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.game.values.ArrivalPreview;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.game.values.VisibleSystem;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Planning aid on the map: in which round a fleet sent now from each own system with ships to spare would reach
 * one target, compared with the round the player wants to meet there. That round is the arrival of the selected
 * fleet, or for a selected system the earliest own arrival already on its way or queued; without one, the badges
 * show plain rounds.
 *
 * @param targetId    the system to meet at
 * @param arrivals    own system to arrival round, only systems that can reach the target and have ships to send
 * @param reference   round to meet, if any
 * @param mayDispatch whether the player may still queue orders this round
 */
record JointArrival(int targetId, Map<Integer, Integer> arrivals, @Nullable Integer reference, boolean mayDispatch) {

    /** How an arrival relates to the round to meet. */
    enum Fit { MATCH, EARLY, LATE, OPEN }

    /** The target and the round to meet there. */
    record Aim(int targetId, @Nullable Integer reference) { }

    JointArrival {
        arrivals = Map.copyOf(arrivals);
    }

    /** A selected own fleet: its final destination and arrival; none while it holds at a station. */
    static @Nullable Aim forFleet(PlayerViewState view, int fleetId) {
        for (Fleet fleet : view.ownFleets()) {
            if (fleet.globalId() == fleetId) {
                return ArrivalPreview.arrivalOf(fleet, view).map(arrival -> new Aim(arrival.systemId(), arrival.turn()))
                        .orElse(null);
            }
        }
        return null;
    }

    /** A selected system: the earliest own arrival there, flying or queued, is the round to meet. */
    static Aim forSystem(PlayerViewState view, int systemId) {
        Integer reference = ArrivalPreview.from(view).stream().filter(arrival -> arrival.systemId() == systemId)
                .map(ArrivalPreview::turn).min(Integer::compare).orElse(null);
        return new Aim(systemId, reference);
    }

    /** Badges only for systems that can send something; nothing to show without any. */
    static @Nullable JointArrival of(PlayerViewState view, Aim aim, Map<Integer, Integer> arrivals, boolean mayDispatch) {
        Set<Integer> ready = view.systems().stream()
                .filter(JointArrival::hasShipsToSend)
                .map(VisibleSystem::id).collect(Collectors.toSet());
        Map<Integer, Integer> shown = arrivals.entrySet().stream()
                .filter(entry -> ready.contains(entry.getKey()) && entry.getKey() != aim.targetId())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return shown.isEmpty() ? null : new JointArrival(aim.targetId(), shown, aim.reference(), mayDispatch);
    }

    private static boolean hasShipsToSend(VisibleSystem system) {
        Integer available = system.availableShips();
        return available != null && available > 0;
    }

    @Nullable Integer arrivalFrom(int systemId) {
        return arrivals.get(systemId);
    }

    Fit fit(int arrival) {
        Integer meet = reference;
        if (meet == null) { return Fit.OPEN; }
        if (arrival == meet) { return Fit.MATCH; }
        return arrival < meet ? Fit.EARLY : Fit.LATE;
    }

    /** Rounds between this arrival and the round to meet; zero without one. */
    int gap(int arrival) {
        Integer meet = reference;
        return meet == null ? 0 : Math.abs(meet - arrival);
    }

    /** Systems arriving together with the reference, in id order: the preselection for a joint send. */
    List<Integer> matchingSources() {
        return arrivals.entrySet().stream().filter(entry -> fit(entry.getValue()) == Fit.MATCH)
                .map(Map.Entry::getKey).sorted(Comparator.naturalOrder()).toList();
    }
}
