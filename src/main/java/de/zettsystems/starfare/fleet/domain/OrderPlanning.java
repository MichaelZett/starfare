package de.zettsystems.starfare.fleet.domain;

import de.zettsystems.starfare.fleet.values.FleetDispatch;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.fleet.values.ScheduledDeparture;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.PlannedOrder;
import de.zettsystems.starfare.navigation.domain.Routes;
import de.zettsystems.starfare.navigation.values.RoutePreview;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

    /**
     * A joint attack on one target: every source arrives in the round of the slowest one. The slowest sources are
     * sent this round as ordinary orders; faster ones get a planned later start so that they launch just in time.
     * Their ships stay in the garrison until then but are reserved. Validated as a whole: either all of it is
     * planned or nothing changes.
     */
    public static boolean jointAttack(GameState state, int player, int target, List<FleetDispatch> dispatches) {
        if (dispatches.isEmpty() || dispatches.stream().map(FleetDispatch::sourceId).distinct().count() != dispatches.size()
                || state.findSystem(target).isEmpty() || state.destroyed(target)) {
            return false;
        }
        Map<Integer, Integer> arrivals = new HashMap<>();
        for (FleetDispatch dispatch : dispatches) {
            Optional<Integer> rounds = dispatch.sourceId() == target ? Optional.empty()
                    : Routes.plan(state, player, dispatch.sourceId(), target).map(RoutePreview::rounds);
            if (rounds.isEmpty()) { return false; }
            arrivals.put(dispatch.sourceId(), state.turn() + rounds.orElseThrow());
        }
        int common = arrivals.values().stream().mapToInt(Integer::intValue).max().orElseThrow();
        List<FleetOrder> orders = new ArrayList<>(state.pendingOrders().getOrDefault(player, List.of()));
        List<ScheduledDeparture> departures = new ArrayList<>(state.departuresOf(player));
        for (FleetDispatch dispatch : dispatches) {
            int delay = common - arrivals.getOrDefault(dispatch.sourceId(), common);
            if (delay == 0) {
                orders.add(new FleetOrder.Send(player, dispatch.sourceId(), target, dispatch.ships()));
            } else {
                departures.add(new ScheduledDeparture(player, dispatch.sourceId(), target, dispatch.ships(),
                        state.turn() + delay, common, null));
            }
        }
        if (!valid(state, player, orders, departures)) { return false; }
        state.rememberOrders(player);
        state.pendingOrders().put(player, orders);
        state.replaceDepartures(player, departures);
        return true;
    }

    /** Drops one planned later start of the player; the ships it reserved become free again. */
    public static boolean cancelDeparture(GameState state, int player, ScheduledDeparture expected) {
        List<ScheduledDeparture> departures = new ArrayList<>(state.departuresOf(player));
        if (expected.ownerId() != player || !departures.remove(expected)) { return false; }
        state.rememberOrders(player);
        state.replaceDepartures(player, departures);
        return true;
    }

    public static boolean replace(GameState state, int player, List<FleetOrder> replacement) {
        if (!valid(state, player, replacement)) { return false; }
        state.rememberOrders(player);
        state.pendingOrders().put(player, new ArrayList<>(replacement));
        return true;
    }

    public static boolean valid(GameState state, int player, List<FleetOrder> orders) {
        return valid(state, player, orders, state.departuresOf(player));
    }

    /**
     * Orders of this round and planned later starts together never commit more ships than a garrison holds. Only
     * new sends and new later starts are checked: a start planned earlier stays even if a battle has since thinned
     * the garrison below it, and it then only blocks further sends from that system.
     */
    public static boolean valid(GameState state, int player, List<FleetOrder> orders, List<ScheduledDeparture> departures) {
        Map<Integer, Long> committed = new HashMap<>();
        java.util.Set<Integer> checked = new java.util.HashSet<>();
        for (FleetOrder order : orders) {
            if (order instanceof FleetOrder.Send(int owner, int from, int to, int ships, Integer beneficiary)) {
                if (owner != player || !ConquestOrders.allowed(state, player, beneficiary)
                        || !sendable(state, player, from, to, ships)) { return false; }
                committed.merge(from, (long) ships, Long::sum);
                checked.add(from);
            }
        }
        for (ScheduledDeparture departure : departures) {
            boolean known = state.scheduledDepartures().contains(departure);
            if (departure.ownerId() != player || (!known && (departure.departureTurn() <= state.turn()
                    || !ConquestOrders.allowed(state, player, departure.beneficiaryId())
                    || !sendable(state, player, departure.fromSystemId(), departure.toSystemId(), departure.ships())))) {
                return false;
            }
            committed.merge(departure.fromSystemId(), (long) departure.ships(), Long::sum);
            if (!known) { checked.add(departure.fromSystemId()); }
        }
        return checked.stream().allMatch(system -> committed.getOrDefault(system, 0L) <= state.getSystem(system).garrison());
    }

    private static boolean sendable(GameState state, int player, int from, int to, int ships) {
        var source = state.findSystem(from).orElse(null);
        return source != null && Objects.equals(source.ownerId(), player) && ships > 0 && from != to
                && Routes.plan(state, player, from, to).isPresent();
    }
}
