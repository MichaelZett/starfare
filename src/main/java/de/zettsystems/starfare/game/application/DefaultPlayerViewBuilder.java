package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.report.values.TurnReport;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
class DefaultPlayerViewBuilder implements PlayerViewBuilder {

    @Override
    public PlayerViewState forPlayer(GameState state, int playerId) {
        int turn = state.turn();
        var players = List.copyOf(state.players());

        List<FleetOrder> orders = state.pendingOrders().getOrDefault(playerId, List.of());
        Map<Integer, Integer> committedBySystem = committedShipsBySystem(orders);

        var vis = state.systems().stream()
                .map(s -> buildVisibleSystem(state, playerId, s, committedBySystem))
                .toList();

        Map<Integer, String> sysNames = state.systems().stream()
                .collect(Collectors.toMap(StarSystem::id, StarSystem::name));
        List<PlannedOrder> plannedOrders = buildPlannedOrders(orders, sysNames);
        List<StandingOrderView> standing = buildStandingOrderViews(state,
                state.standingOrders().getOrDefault(playerId, List.of()), sysNames);

        var ownFleets = state.fleets().stream().filter(f -> f.ownerId() == playerId).toList();
        var report = state.reports().getOrDefault(playerId, new TurnReport(turn - 1, List.of()));
        return new PlayerViewState(turn, players, vis, ownFleets, report, state.gameOver(), state.winnerId(),
                plannedOrders, standing);
    }

    @Override
    public PlayerViewState forObserver(GameState state) {
        int turn = state.turn();
        var players = List.copyOf(state.players());
        var systems = state.systems().stream().map(s -> {
            String color = s.ownerId() == null ? null : playerById(state, s.ownerId()).colorHex();
            return new VisibleSystem(
                    s.id(), s.name(), s.x(), s.y(),
                    s.ownerId(), s.garrison(), s.productionPerTurn(),
                    true, color, turn);
        }).toList();
        var fleets = List.copyOf(state.fleets());
        return new PlayerViewState(turn, players, systems, fleets, null, state.gameOver(), state.winnerId(),
                List.of(), List.of());
    }

    private static Map<Integer, Integer> committedShipsBySystem(List<FleetOrder> orders) {
        Map<Integer, Integer> committed = new HashMap<>();
        for (FleetOrder o : orders) {
            if (o instanceof FleetOrder.Send s) {
                committed.merge(s.fromSystemId(), s.ships(), Integer::sum);
            }
        }
        return committed;
    }

    private static VisibleSystem buildVisibleSystem(GameState state, int playerId, StarSystem s,
                                                    Map<Integer, Integer> committedBySystem) {
        boolean own = Objects.equals(s.ownerId(), playerId);
        String color = null;
        Integer lastSeen = null;
        if (own) {
            color = playerById(state, playerId).colorHex();
            lastSeen = state.turn();
        } else {
            var intel = state.intel().getOrDefault(playerId, Map.of()).get(s.id());
            if (intel != null && intel.ownerId() != null) {
                color = playerById(state, intel.ownerId()).colorHex();
                lastSeen = intel.turn();
            }
        }
        Integer garrison = own ? s.garrison() - committedBySystem.getOrDefault(s.id(), 0) : null;
        return new VisibleSystem(
                s.id(), s.name(), s.x(), s.y(),
                own ? s.ownerId() : null,
                garrison,
                own ? s.productionPerTurn() : null,
                own, color, lastSeen);
    }

    private static List<StandingOrderView> buildStandingOrderViews(GameState state, List<StandingOrder> orders,
                                                                   Map<Integer, String> sysNames) {
        var out = new ArrayList<StandingOrderView>();
        for (StandingOrder o : orders) {
            var from = state.getSystem(o.fromSystemId());
            int prod = from != null ? from.productionPerTurn() : 0;
            out.add(new StandingOrderView(o.id(), o.fromSystemId(), o.toSystemId(),
                    sysNames.getOrDefault(o.fromSystemId(), "?"),
                    sysNames.getOrDefault(o.toSystemId(), "?"),
                    prod));
        }
        return List.copyOf(out);
    }

    private static List<PlannedOrder> buildPlannedOrders(List<FleetOrder> orders, Map<Integer, String> sysNames) {
        var result = new ArrayList<PlannedOrder>();
        for (int i = 0; i < orders.size(); i++) {
            FleetOrder o = orders.get(i);
            result.add(switch (o) {
                case FleetOrder.Send s -> new PlannedOrder(i, "map.orderType.send",
                        s.fromSystemId(), s.toSystemId(),
                        sysNames.getOrDefault(s.fromSystemId(), "?"),
                        sysNames.getOrDefault(s.toSystemId(), "?"),
                        s.ships(), false, null);
                case FleetOrder.Wait _ ->
                        new PlannedOrder(i, "map.orderType.wait", null, null, "-", "-", null, false, null);
                case FleetOrder.Disband _ ->
                        new PlannedOrder(i, "map.orderType.disband", null, null, "-", "-", null, false, null);
            });
        }
        return List.copyOf(result);
    }

    private static Player playerById(GameState state, int id) {
        return state.players().stream().filter(p -> p.id() == id).findFirst().orElseThrow();
    }
}
