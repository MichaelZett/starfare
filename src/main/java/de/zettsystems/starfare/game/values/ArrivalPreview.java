package de.zettsystems.starfare.game.values;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Own arrivals, counting a waiting fleet once at its delayed arrival. */
public record ArrivalPreview(int systemId, String systemName, int turn, long ships) {
    public static List<ArrivalPreview> from(PlayerViewState view) {
        record Destination(int system, int turn) { }
        Map<Destination, Long> totals = new HashMap<>();
        for (Fleet fleet : view.ownFleets()) {
            var journey = fleet.journey();
            if (journey != null && journey.blocked()) { continue; }
            int destination = journey == null ? fleet.toSystemId() : journey.destination();
            int arrival = (journey == null ? fleet.arrivalTurn() : journey.finalArrivalTurn()) + (view.waitingFleetIds().contains(fleet.globalId()) ? 1 : 0);
            totals.merge(new Destination(destination, arrival), (long) fleet.ships(), Long::sum);
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
