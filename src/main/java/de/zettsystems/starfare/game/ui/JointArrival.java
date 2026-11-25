package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.game.values.ArrivalPreview;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.game.values.VisibleSystem;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Joint attack on the map: in which round a fleet sent now from each own system with ships to spare would reach
 * one target, compared with the round to meet there. Once sources are picked, that round is the latest arrival
 * among them (the common arrival; faster sources start later). Before that it is the arrival of the selected fleet,
 * or for a selected system the earliest own arrival already on its way or planned; without one, the badges show
 * plain rounds.
 *
 * @param targetId    the system to meet at
 * @param arrivals    own system to arrival round, only systems that can reach the target and have ships to send
 * @param reference   round to meet before any source is picked, if any
 * @param mayDispatch whether the player may still queue orders this round
 * @param selected    the sources picked for a joint attack, a subset of {@code arrivals}
 */
record JointArrival(int targetId, Map<Integer, Integer> arrivals, @Nullable Integer reference, boolean mayDispatch,
                    Set<Integer> selected) {

    /** How an arrival relates to the round to meet. */
    enum Fit { MATCH, EARLY, LATE, OPEN }

    /** The target and the round to meet there. */
    record Aim(int targetId, @Nullable Integer reference) { }

    JointArrival {
        arrivals = Map.copyOf(arrivals);
        selected = Set.copyOf(selected);
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

    /** Badges only for systems that can send something; nothing to show without any. Picks outside them drop out. */
    static @Nullable JointArrival of(PlayerViewState view, Aim aim, Map<Integer, Integer> arrivals, boolean mayDispatch,
                                     Set<Integer> picked) {
        Set<Integer> ready = view.systems().stream()
                .filter(JointArrival::hasShipsToSend)
                .map(VisibleSystem::id).collect(Collectors.toSet());
        Map<Integer, Integer> shown = arrivals.entrySet().stream()
                .filter(entry -> ready.contains(entry.getKey()) && entry.getKey() != aim.targetId())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Set<Integer> selected = picked.stream().filter(shown::containsKey).collect(Collectors.toSet());
        return shown.isEmpty() ? null : new JointArrival(aim.targetId(), shown, aim.reference(), mayDispatch, selected);
    }

    private static boolean hasShipsToSend(VisibleSystem system) {
        Integer available = system.availableShips();
        return available != null && available > 0;
    }

    @Nullable Integer arrivalFrom(int systemId) {
        return arrivals.get(systemId);
    }

    boolean isSelected(int systemId) {
        return selected.contains(systemId);
    }

    /** Latest arrival among the picked sources: the round the joint attack arrives; none before a pick. */
    @Nullable Integer commonArrival() {
        return selected.stream().map(arrivals::get).max(Integer::compare).orElse(null);
    }

    /** The round every badge is compared with: the common arrival once sources are picked, else the reference. */
    @Nullable Integer meet() {
        Integer common = commonArrival();
        return common != null ? common : reference;
    }

    /** Picked sources in id order with their arrival rounds, as the confirmation lists them. */
    List<Map.Entry<Integer, Integer>> selectedArrivals() {
        return selected.stream().sorted().map(id -> Map.entry(id, arrivals.get(id))).toList();
    }

    Fit fit(int arrival) {
        Integer meet = meet();
        if (meet == null) { return Fit.OPEN; }
        if (arrival == meet) { return Fit.MATCH; }
        return arrival < meet ? Fit.EARLY : Fit.LATE;
    }

    /** Rounds between this arrival and the round to meet; zero without one. */
    int gap(int arrival) {
        Integer meet = meet();
        return meet == null ? 0 : Math.abs(meet - arrival);
    }
}
