package de.zettsystems.starfare.game.values;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Own arrivals, counting a waiting fleet once at its delayed arrival. */
public record ArrivalPreview(int systemId, String systemName, int turn, long ships) {
    /** Final destination and arrival round of one own fleet. */
    public record Arrival(int systemId, int turn) { }

    /** Where an own fleet ends its journey and when, a wait included; empty while it holds at a station. */
    public static Optional<Arrival> arrivalOf(Fleet fleet, PlayerViewState view) {
        var journey = fleet.journey();
        if (journey != null && (journey.blocked() || journey.stationed())) { return Optional.empty(); }
        int destination = journey == null ? fleet.toSystemId() : journey.destination();
        int arrival = (journey == null ? fleet.arrivalTurn() : journey.finalArrivalTurn()) + (view.waitingFleetIds().contains(fleet.globalId()) ? 1 : 0);
        return Optional.of(new Arrival(destination, arrival));
    }

    public static List<ArrivalPreview> from(PlayerViewState view) {
        record Destination(int system, int turn) { }
        Map<Destination, Long> totals = new HashMap<>();
        for (Fleet fleet : view.ownFleets()) {
            arrivalOf(fleet, view).ifPresent(arrival ->
                    totals.merge(new Destination(arrival.systemId(), arrival.turn()), (long) fleet.ships(), Long::sum));
        }
        for (PlannedOrder order : view.plannedOrders()) {
            Integer target = order.toSystemId();
            Integer turn = order.arrivalTurn();
            Integer ships = order.ships();
            if (order.isSend() && target != null && turn != null && ships != null) {
                totals.merge(new Destination(target, turn), ships.longValue(), Long::sum);
            }
        }
        return totals.entrySet().stream().map(entry -> new ArrivalPreview(entry.getKey().system(),
                        view.systems().stream().filter(system -> system.id() == entry.getKey().system())
                                .map(VisibleSystem::name).findFirst().orElse("?"), entry.getKey().turn(), entry.getValue()))
                .sorted(Comparator.comparingInt(ArrivalPreview::turn).thenComparing(ArrivalPreview::systemName)
                        .thenComparingInt(ArrivalPreview::systemId)).toList();
    }
}
