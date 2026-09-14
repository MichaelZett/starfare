package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.report.values.TurnReport;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
class DefaultPlayerViewBuilder implements PlayerViewBuilder {

    /** Reiserunden, die eigene Systeme und Flotten an Sensorreichweite abdecken. */
    private static final int SENSOR_RANGE_ROUNDS = 2;

    /**
     * Wie viele Runden Kampfaufklärung als frisch gilt. Ein Kampf schreibt seine Intel
     * mit der Rundennummer <em>vor</em> {@code nextTurn()}, die Zahl aus dem
     * Rundenbericht ist also schon eine Runde alt, wenn der Spieler die Karte sieht.
     */
    private static final int FRESH_INTEL_TURNS = 1;

    @Override
    public PlayerViewState forPlayer(GameState state, int playerId) {
        if (state.gameOver()) {
            // Partie entschieden — der Nebel hat keinen Zweck mehr und verdeckt nur,
            // wie es ausgegangen ist.
            return revealedView(state, playerId);
        }

        return filteredView(state, playerId);
    }

    @Override
    public PlayerViewState forReview(GameState state, int playerId, boolean fogOfWar) {
        return fogOfWar ? filteredView(state, playerId) : forObserver(state);
    }

    private static PlayerViewState filteredView(GameState state, int playerId) {
        int turn = state.turn();
        var players = List.copyOf(state.players());
        List<FleetOrder> orders = state.gameOver() ? List.of()
                : state.pendingOrders().getOrDefault(playerId, List.of());
        Map<Integer, Integer> committedBySystem = committedShipsBySystem(orders);
        Map<Integer, Integer> routedBySystem = routedShipsBySystem(state, playerId);

        Set<Integer> sensorCoverage = sensorCoverage(state, playerId);
        var vis = state.systems().stream()
                .map(s -> buildVisibleSystem(state, playerId, s, committedBySystem, sensorCoverage, routedBySystem))
                .toList();

        Map<Integer, String> sysNames = state.systems().stream()
                .collect(Collectors.toMap(StarSystem::id, StarSystem::name));
        List<PlannedOrder> plannedOrders = buildPlannedOrders(orders, sysNames);
        List<StandingOrderView> standing = buildStandingOrderViews(state,
                state.standingOrders().getOrDefault(playerId, List.of()), sysNames);

        var ownFleets = state.fleets().stream().filter(f -> f.ownerId() == playerId).toList();
        var report = state.reports().getOrDefault(playerId, new TurnReport(turn - 1, List.of()));
        return new PlayerViewState(turn, players, vis, ownFleets, report, state.gameOver(), state.winnerId(),
                plannedOrders, standing, empireStats(state, playerId, ownFleets), waitingFleetIds(state, orders));
    }

    @Override
    public PlayerViewState forObserver(GameState state) {
        int turn = state.turn();
        return new PlayerViewState(turn, List.copyOf(state.players()), revealedSystems(state),
                List.copyOf(state.fleets()), null, state.gameOver(), state.winnerId(),
                List.of(), List.of(), EmpireStats.NONE, Set.of());
    }

    /** Alle Systeme ohne Nebel: fuer Zuschauer und fuer die entschiedene Partie. */
    private static List<VisibleSystem> revealedSystems(GameState state) {
        int turn = state.turn();
        return state.systems().stream().map(s -> {
            Integer ownerId = s.ownerId();
            String color = ownerId != null ? playerById(state, ownerId).colorHex() : null;
            return new VisibleSystem(
                    s.id(), s.name(), s.x(), s.y(),
                    ownerId, s.garrison(), s.productionPerTurn(),
                    true, color, turn, false, null);
        }).toList();
    }

    private static PlayerViewState revealedView(GameState state, int playerId) {
        int turn = state.turn();
        var ownFleets = state.fleets().stream().filter(f -> f.ownerId() == playerId).toList();
        var report = state.reports().getOrDefault(playerId, new TurnReport(turn - 1, List.of()));
        return new PlayerViewState(turn, List.copyOf(state.players()), revealedSystems(state),
                ownFleets, report, state.gameOver(), state.winnerId(),
                List.of(), List.of(), empireStats(state, playerId, ownFleets), Set.of());
    }

    private static EmpireStats empireStats(GameState state, int playerId, List<Fleet> ownFleets) {
        int systems = 0;
        int production = 0;
        int garrison = 0;
        for (StarSystem s : state.systems()) {
            Integer ownerId = s.ownerId();
            if (ownerId != null && ownerId == playerId) {
                systems++;
                production += s.productionPerTurn();
                garrison += s.garrison();
            }
        }
        int inTransit = ownFleets.stream().mapToInt(Fleet::ships).sum();
        return new EmpireStats(systems, production, garrison + inTransit);
    }

    private static Set<Integer> waitingFleetIds(GameState state, List<FleetOrder> orders) {
        Set<Integer> waiting = new HashSet<>(state.waitThisTurn());
        for (FleetOrder order : orders) {
            if (order instanceof FleetOrder.Wait wait) {
                waiting.add(wait.fleetId());
            }
        }
        return Set.copyOf(waiting);
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

    /** Je Quellsystem die Summe der ausgehenden Produktionsverlegungen. */
    private static Map<Integer, Integer> routedShipsBySystem(GameState state, int playerId) {
        Map<Integer, Integer> out = new HashMap<>();
        for (StandingOrder o : state.standingOrders().getOrDefault(playerId, List.of())) {
            out.merge(o.fromSystemId(), o.ships(), Integer::sum);
        }
        return out;
    }

    private static VisibleSystem buildVisibleSystem(GameState state, int playerId, StarSystem s,
                                                    Map<Integer, Integer> committedBySystem,
                                                    Set<Integer> sensorCoverage,
                                                    Map<Integer, Integer> routedBySystem) {
        boolean own = Objects.equals(s.ownerId(), playerId);
        var intel = state.intel().getOrDefault(playerId, Map.of()).get(s.id());
        boolean inRange = !own && sensorCoverage.contains(s.id());

        Integer visibleOwner;
        Integer garrison;
        String color = null;
        Integer lastSeen = null;
        if (own) {
            visibleOwner = s.ownerId();
            garrison = s.garrison() - committedBySystem.getOrDefault(s.id(), 0);
            color = playerById(state, playerId).colorHex();
            lastSeen = state.turn();
        } else if (inRange) {
            // Live-Sicht: Besitzer exakt. Die Garnison bleibt geschätzt — es sei denn,
            // hier wurde eben gekämpft: dann kennt der Spieler die genaue Zahl bereits
            // aus dem Rundenbericht und dürfte sie auf der Karte nicht gröber sehen.
            visibleOwner = s.ownerId();
            garrison = freshIntelGarrison(state, intel).orElseGet(() -> approximateGarrison(s.garrison()));
            color = visibleOwner != null ? playerById(state, visibleOwner).colorHex() : null;
            lastSeen = state.turn();
        } else if (intel != null) {
            // Nur Aufklaerung aus vergangenen Kaempfen; Alter steckt im Tooltip.
            visibleOwner = intel.ownerId();
            garrison = intel.garrison();
            if (visibleOwner != null) {
                color = playerById(state, visibleOwner).colorHex();
                lastSeen = intel.turn();
            }
        } else {
            visibleOwner = null;
            garrison = null;
        }
        return new VisibleSystem(
                s.id(), s.name(), s.x(), s.y(),
                visibleOwner,
                garrison,
                own ? s.productionPerTurn() : null,
                own, color, lastSeen, inRange,
                own ? routedBySystem.getOrDefault(s.id(), 0) : null);
    }

    /**
     * Systeme in Sensorreichweite eigener Systeme oder Flottenziele. Einmal je Ansicht
     * berechnet: die Reichweitenpruefung je Systempaar war quadratisch in der Systemzahl.
     */
    private static Set<Integer> sensorCoverage(GameState state, int playerId) {
        Set<Integer> sources = new HashSet<>();
        for (StarSystem s : state.systems()) {
            Integer ownerId = s.ownerId();
            if (ownerId != null && ownerId == playerId) {
                sources.add(s.id());
            }
        }
        for (Fleet fleet : state.fleets()) {
            if (fleet.ownerId() == playerId) {
                sources.add(fleet.toSystemId());
            }
        }
        return state.systemsWithinRounds(sources, SENSOR_RANGE_ROUNDS);
    }

    private static Optional<Integer> freshIntelGarrison(GameState state, GameState.@Nullable Intel intel) {
        if (intel == null || intel.garrison() == null) {
            return Optional.empty();
        }
        return intel.turn() >= state.turn() - FRESH_INTEL_TURNS
                ? Optional.of(intel.garrison())
                : Optional.empty();
    }

    private static int approximateGarrison(int ships) {
        if (ships < 10) return 5;
        if (ships < 25) return 15;
        if (ships < 50) return 35;
        return 75;
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
                    prod, o.ships()));
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
