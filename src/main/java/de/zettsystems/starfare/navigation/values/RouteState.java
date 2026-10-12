package de.zettsystems.starfare.navigation.values;

import de.zettsystems.starfare.game.values.Fleet;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Authorized fleet timing, with one pending wait and no promised blocked arrival. */
public record RouteState(List<Integer> systems, int activeIndex, boolean atStation, boolean blocked,
                         @Nullable Integer arrivalTurn, @Nullable Integer nextTurn) {
    public RouteState { systems = List.copyOf(systems); }
    public List<Integer> compactIndices() {
        var indices = new java.util.TreeSet<Integer>();
        indices.add(0); indices.add(activeIndex); indices.add(Math.min(systems.size() - 1, activeIndex + 1));
        indices.add(systems.size() - 1); return List.copyOf(indices);
    }
    public static RouteState forFleet(Fleet fleet, boolean waiting) {
        int delay = waiting ? 1 : 0;
        var journey = fleet.journey();
        if (journey == null) {
            return new RouteState(List.of(fleet.fromSystemId(), fleet.toSystemId()), 1, false, false,
                    fleet.arrivalTurn() + delay, fleet.arrivalTurn() + delay);
        }
        boolean stopped = journey.blocked() || journey.stationed();
        Integer arrival = stopped ? null : journey.finalArrivalTurn() + delay;
        Integer next = stopped ? null : journey.readyTurn() + delay;
        if (journey.flying()) { next = fleet.arrivalTurn() + delay; }
        return new RouteState(journey.stations(), journey.legIndex(), !journey.flying(), journey.blocked(), arrival, next);
    }
}
