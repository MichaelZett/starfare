package de.zettsystems.starfare.fleet.domain;

import de.zettsystems.starfare.fleet.values.FleetDispatch;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.PlannedOrder;
import de.zettsystems.starfare.navigation.domain.Routes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Atomic planning operations; callers hold the session write lock. */
public final class OrderPlanning {
    private OrderPlanning() { }

    public static boolean edit(GameState state, int player, PlannedOrder expected, int target, int ships) {
        List<FleetOrder> current = state.pendingOrders().getOrDefault(player, List.of());
        int index = expected.index();
        if (index < 0 || index >= current.size() || !(current.get(index) instanceof FleetOrder.Send old)
                || !Objects.equals(expected.fromSystemId(), old.fromSystemId())
                || !Objects.equals(expected.toSystemId(), old.toSystemId())
                || !Objects.equals(expected.ships(), old.ships())
                || !Objects.equals(expected.beneficiaryId(), old.beneficiaryId())) { return false; }
        List<FleetOrder> replacement = new ArrayList<>(current);
        replacement.set(index, new FleetOrder.Send(player, old.fromSystemId(), target, ships, old.beneficiaryId()));
        return replace(state, player, replacement);
    }

    public static boolean dispatch(GameState state, int player, int target, List<FleetDispatch> dispatches) {
        if (dispatches.isEmpty() || dispatches.stream().map(FleetDispatch::sourceId).distinct().count() != dispatches.size()) {
            return false;
        }
        List<FleetOrder> replacement = new ArrayList<>(state.pendingOrders().getOrDefault(player, List.of()));
        dispatches.forEach(dispatch -> replacement.add(new FleetOrder.Send(player, dispatch.sourceId(), target, dispatch.ships())));
        return replace(state, player, replacement);
    }

    public static boolean replace(GameState state, int player, List<FleetOrder> replacement) {
        if (!valid(state, player, replacement)) { return false; }
        state.rememberOrders(player);
        state.pendingOrders().put(player, new ArrayList<>(replacement));
        return true;
    }

    public static boolean valid(GameState state, int player, List<FleetOrder> orders) {
        Map<Integer, Long> committed = new HashMap<>();
        for (FleetOrder order : orders) {
            if (order instanceof FleetOrder.Send send) {
                var source = state.systems().stream().filter(system -> system.id() == send.fromSystemId()).findFirst().orElse(null);
                if (source == null || !Objects.equals(source.ownerId(), player) || send.ownerId() != player
                        || !ConquestOrders.allowed(state, player, send.beneficiaryId())
                        || send.ships() <= 0 || send.fromSystemId() == send.toSystemId()
                        || state.systems().stream().noneMatch(system -> system.id() == send.toSystemId())
                        || Routes.plan(state, player, send.fromSystemId(), send.toSystemId()).isEmpty()) { return false; }
                long total = committed.merge(send.fromSystemId(), (long) send.ships(), Long::sum);
                if (total > source.availableShips()) { return false; }
            }
        }
        return true;
    }

    public static boolean reserves(GameState state, int player, List<Integer> systems, int reserve, boolean production) {
        if (systems.isEmpty() || reserve < 0 || systems.stream().anyMatch(id -> state.systems().stream()
                .noneMatch(system -> system.id() == id && Objects.equals(system.ownerId(), player)))) { return false; }
        for (int id : systems) {
            var system = state.getSystem(id);
            int desired = production ? system.productionPerTurn() : reserve;
            long committed = state.pendingOrders().getOrDefault(player, List.of()).stream()
                    .filter(order -> order instanceof FleetOrder.Send send && send.fromSystemId() == id)
                    .mapToLong(order -> ((FleetOrder.Send) order).ships()).sum();
            if (committed > Math.max(0L, system.garrison() - (long) desired)) { return false; }
        }
        systems.forEach(id -> state.updateSystem(id, system -> system.reserveGarrison(production ? system.productionPerTurn() : reserve)));
        return true;
    }
}
