package de.zettsystems.starfare.fleet.application;

import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.StandingOrder;
import de.zettsystems.starfare.game.values.StarSystem;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DefaultFleetService implements FleetService {

    @Override
    public boolean queueSend(GameState state, int playerId, int fromId, int toId, int ships) {
        StarSystem from = state.getSystem(fromId);
        if (!Objects.equals(from.ownerId(), playerId)) {
            return false;
        }
        if (ships <= 0) {
            return false;
        }
        int committed = committedShipsFrom(state, playerId, fromId);
        if (from.garrison() - committed < ships) {
            return false;
        }
        pendingFor(state, playerId).add(new FleetOrder.Send(playerId, fromId, toId, ships));
        return true;
    }

    @Override
    public boolean queueWait(GameState state, int playerId, int fleetId) {
        Fleet f = state.fleets().stream()
                .filter(x -> x.globalId() == fleetId && x.ownerId() == playerId)
                .findFirst().orElse(null);
        if (f == null) {
            return false;
        }
        List<FleetOrder> orders = pendingFor(state, playerId);
        boolean already = orders.stream()
                .anyMatch(o -> o instanceof FleetOrder.Wait w && w.fleetId() == fleetId);
        if (!already) {
            orders.add(new FleetOrder.Wait(playerId, fleetId));
        }
        return true;
    }

    @Override
    public boolean queueDisband(GameState state, int playerId, int fleetId) {
        Fleet f = state.fleets().stream()
                .filter(x -> x.globalId() == fleetId && x.ownerId() == playerId && x.launchTurn() == state.turn())
                .findFirst().orElse(null);
        if (f == null) {
            return false;
        }
        List<FleetOrder> orders = pendingFor(state, playerId);
        boolean already = orders.stream()
                .anyMatch(o -> o instanceof FleetOrder.Disband d && d.fleetId() == fleetId);
        if (!already) {
            orders.add(new FleetOrder.Disband(playerId, fleetId));
        }
        return true;
    }

    @Override
    public boolean sendFleet(GameState state, int playerId, int fromId, int toId, int ships) {
        StarSystem from = state.getSystem(fromId);
        if (!Objects.equals(from.ownerId(), playerId)) {
            return false;
        }
        if (ships <= 0 || from.garrison() < ships) {
            return false;
        }
        state.updateSystem(fromId, current -> current.launchFleet(ships));
        state.addFleet(playerId, fromId, toId, ships);
        return true;
    }

    @Override
    public boolean setFleetWait(GameState state, int playerId, int fleetId) {
        var f = state.fleets().stream()
                .filter(x -> x.globalId() == fleetId && x.ownerId() == playerId)
                .findFirst().orElse(null);
        if (f == null) {
            return false;
        }
        state.waitThisTurn().add(fleetId);
        return true;
    }

    @Override
    public boolean disbandFleet(GameState state, int playerId, int fleetId) {
        var it = state.fleets().iterator();
        while (it.hasNext()) {
            Fleet f = it.next();
            if (f.globalId() == fleetId && f.ownerId() == playerId && f.launchTurn() == state.turn()) {
                it.remove();
                state.waitThisTurn().remove(fleetId);
                state.updateSystem(f.fromSystemId(), current -> current.reinforce(f.ships()));
                return true;
            }
        }
        return false;
    }

    @Override
    public int addStandingOrder(GameState state, int playerId, int fromId, int toId) {
        if (fromId == toId) {
            return -1;
        }
        StarSystem from = state.getSystem(fromId);
        if (from == null || !Objects.equals(from.ownerId(), playerId)) {
            return -1;
        }
        StarSystem target = state.getSystem(toId);
        if (target == null) {
            return -1;
        }
        List<StandingOrder> list = standingFor(state, playerId);
        for (int i = 0; i < list.size(); i++) {
            StandingOrder existing = list.get(i);
            if (existing.fromSystemId() == fromId) {
                list.set(i, new StandingOrder(existing.id(), playerId, fromId, toId));
                return existing.id();
            }
        }
        int id = state.nextStandingOrderIdFor(playerId);
        list.add(new StandingOrder(id, playerId, fromId, toId));
        return id;
    }

    @Override
    public boolean removeStandingOrder(GameState state, int playerId, int orderId) {
        List<StandingOrder> list = state.standingOrders().get(playerId);
        if (list == null) {
            return false;
        }
        return list.removeIf(o -> o.id() == orderId);
    }

    @Override
    public boolean removeStandingOrderFrom(GameState state, int playerId, int fromSystemId) {
        List<StandingOrder> list = state.standingOrders().get(playerId);
        if (list == null) {
            return false;
        }
        return list.removeIf(o -> o.fromSystemId() == fromSystemId);
    }

    @Override
    public Set<Integer> applyStandingOrdersForProduction(GameState state) {
        Set<Integer> routed = new HashSet<>();
        state.standingOrders().forEach((playerId, list) -> applyStandingOrders(state, playerId, list, routed));
        return routed;
    }

    private static void applyStandingOrders(GameState state, int playerId, List<StandingOrder> list, Set<Integer> routed) {
        Iterator<StandingOrder> it = list.iterator();
        while (it.hasNext()) {
            StandingOrder o = it.next();
            StarSystem from = state.getSystem(o.fromSystemId());
            if (from == null || !Objects.equals(from.ownerId(), playerId) || from.neutral()) {
                it.remove();
            } else {
                routeOneOrder(state, playerId, o, from, routed);
            }
        }
    }

    private static void routeOneOrder(GameState state, int playerId, StandingOrder o, StarSystem from, Set<Integer> routed) {
        routed.add(o.fromSystemId());
        int prod = from.productionPerTurn();
        if (prod > 0) {
            state.addFleet(playerId, o.fromSystemId(), o.toSystemId(), prod);
        }
    }

    private static List<FleetOrder> pendingFor(GameState state, int playerId) {
        return state.pendingOrders().computeIfAbsent(playerId, _ -> new ArrayList<>());
    }

    private static List<StandingOrder> standingFor(GameState state, int playerId) {
        return state.standingOrders().computeIfAbsent(playerId, _ -> new ArrayList<>());
    }

    private static int committedShipsFrom(GameState state, int playerId, int fromId) {
        List<FleetOrder> orders = state.pendingOrders().get(playerId);
        if (orders == null) {
            return 0;
        }
        return orders.stream()
                .filter(o -> o instanceof FleetOrder.Send s && s.fromSystemId() == fromId)
                .mapToInt(o -> ((FleetOrder.Send) o).ships())
                .sum();
    }
}
