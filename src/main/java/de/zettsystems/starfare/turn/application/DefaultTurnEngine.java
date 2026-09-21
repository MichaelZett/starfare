package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.ai.application.AiService;
import de.zettsystems.starfare.combat.application.CombatService;
import de.zettsystems.starfare.fleet.application.FleetService;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.AttackOrder;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.GameConfig;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.report.application.ReportService;
import de.zettsystems.starfare.report.values.TurnEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring-injected collaborators are kept by reference for the bean's lifetime by design.")
public class DefaultTurnEngine implements TurnEngine {
    private final CombatService combatService;
    private final AiService aiService;
    private final ReportService reportService;
    private final FleetService fleetService;

    public DefaultTurnEngine(CombatService combatService, AiService aiService, ReportService reportService, FleetService fleetService) {
        this.combatService = combatService;
        this.aiService = aiService;
        this.reportService = reportService;
        this.fleetService = fleetService;
    }

    @Override
    public void advanceTurn(GameState state) {
        if (state.gameOver()) {
            return;
        }
        // Die KI plant auf demselben Stand, den die Menschen waehrend der Runde gesehen haben,
        // und ihre Befehle laufen zusammen mit deren Befehlen ein: kein Informationsvorsprung.
        aiService.planAiTurns(state, fleetService);
        applyOrders(state);
        Map<Integer, Integer> routedShips = fleetService.applyStandingOrdersForProduction(state);
        applyProduction(state, routedShips);
        applyWaitOrders(state);
        resolveArrivals(state);
        checkVictory(state);
        state.captureReplayFrame();
        state.nextTurn();
    }

    private void applyProduction(GameState state, Map<Integer, Integer> routedShips) {
        for (StarSystem s : state.systems()) {
            Integer ownerId = s.ownerId();
            if (ownerId == null || s.neutral()) {
                continue;
            }
            // Verlegungen sind bereits abgeflossen; was uebrig bleibt, geht in die Garnison.
            int routed = routedShips.getOrDefault(s.id(), 0);
            if (routed > 0) {
                state.updateSystem(s.id(), current -> current.produceAndRoute(routed));
            } else {
                state.updateSystem(s.id(), StarSystem::produce);
            }
            reportService.appendEvent(state, ownerId,
                    new TurnEvent.Production(ownerId, s.id(), s.name(), s.productionPerTurn()));
        }
    }

    private static void applyWaitOrders(GameState state) {
        if (state.waitThisTurn().isEmpty()) {
            return;
        }
        var list = new ArrayList<>(state.fleets());
        state.fleets().clear();
        for (Fleet f : list) {
            if (state.waitThisTurn().contains(f.globalId())) {
                state.fleets().add(f.delayedByOneTurn());
            } else {
                state.fleets().add(f);
            }
        }
        state.waitThisTurn().clear();
    }

    private void resolveArrivals(GameState state) {
        Map<Integer, Map<Integer, MergedArrival>> merged = groupArrivals(state);
        state.fleets().removeIf(f -> f.arrivalTurn() == state.turn() + 1);
        for (var e : merged.entrySet()) {
            resolveSystemArrival(state, e.getKey(), e.getValue());
        }
    }

    private static Map<Integer, Map<Integer, MergedArrival>> groupArrivals(GameState state) {
        Map<Integer, Map<Integer, MergedArrival>> merged = new HashMap<>();
        state.fleets().stream()
                .filter(f -> f.arrivalTurn() == state.turn() + 1)
                .forEach(f -> merged
                        .computeIfAbsent(f.toSystemId(), _ -> new HashMap<>())
                        .merge(f.ownerId(),
                                new MergedArrival(f.ships(), List.of(f.localNo())),
                                (a, b) -> new MergedArrival(a.ships + b.ships, concat(a.localNos, b.localNos))));
        return merged;
    }

    private void resolveSystemArrival(GameState state, int toId, Map<Integer, MergedArrival> ownerMap) {
        Integer ownerOnTarget = state.getSystem(toId).ownerId();
        if (ownerOnTarget != null) {
            applyReinforcement(state, toId, ownerOnTarget, ownerMap);
        }
        for (var atk : attackOrder(state, ownerMap)) {
            combatService.resolveAttack(state, atk.getKey(), toId, atk.getValue().ships, atk.getValue().localNos);
        }
    }

    /** Reihenfolge der Angreifer laut Spielregel; jeder kämpft gegen die dann stehende Garnison. */
    private static List<Map.Entry<Integer, MergedArrival>> attackOrder(GameState state,
                                                                       Map<Integer, MergedArrival> ownerMap) {
        List<Map.Entry<Integer, MergedArrival>> attackers = new ArrayList<>(ownerMap.entrySet());
        if (state.roundRules().attackOrder() == AttackOrder.RANDOM) {
            Collections.shuffle(attackers, ThreadLocalRandom.current());
        } else {
            attackers.sort(Comparator.comparingInt((Map.Entry<Integer, MergedArrival> e) -> e.getValue().ships)
                    .reversed()
                    .thenComparing(Map.Entry::getKey));
        }
        return attackers;
    }

    private void applyReinforcement(GameState state, int toId, int ownerOnTarget, Map<Integer, MergedArrival> ownerMap) {
        MergedArrival self = ownerMap.get(ownerOnTarget);
        if (self == null || self.ships <= 0) {
            return;
        }
        StarSystem t = state.getSystem(toId);
        state.updateSystem(toId, current -> current.reinforce(self.ships));
        int totalAfter = state.getSystem(toId).garrison();
        reportService.appendEvent(state, ownerOnTarget,
                new TurnEvent.Reinforcement(ownerOnTarget, toId, t.name(), self.ships, totalAfter,
                        "F" + listNos(self.localNos)));
        ownerMap.remove(ownerOnTarget);
    }

    private void applyOrders(GameState state) {
        state.pendingOrders().forEach((_, orders) -> {
            for (FleetOrder order : orders) {
                switch (order) {
                    case FleetOrder.Send(int ownerId, int from, int to, int ships) ->
                            fleetService.sendFleet(state, ownerId, from, to, ships);
                    case FleetOrder.Wait(int ownerId, int fleetId) ->
                            fleetService.setFleetWait(state, ownerId, fleetId);
                    case FleetOrder.Disband(int ownerId, int fleetId) ->
                            fleetService.disbandFleet(state, ownerId, fleetId);
                }
            }
        });
        state.pendingOrders().clear();
    }

    private void checkVictory(GameState state) {
        if (state.gameOver()) {
            return;
        }
        int total = state.systems().size();
        var counts = state.systems().stream().filter(s -> s.ownerId() != null)
                .collect(Collectors.groupingBy(StarSystem::ownerId, Collectors.counting()));
        counts.forEach((pid, c) -> {
            // Ganzzahlig statt ueber Prozent-Division, damit nichts weggerundet wird.
            if (c * 100 >= (long) total * GameConfig.VICTORY_SYSTEM_PERCENT) {
                state.endGame(pid);
                reportService.appendEvent(state, pid, new TurnEvent.Victory(pid));
                String winnerName = state.players().stream().filter(player -> player.id() == pid)
                        .findFirst().orElseThrow().label();
                state.players().stream().filter(player -> player.id() != pid)
                        .forEach(player -> reportService.appendEvent(state, player.id(),
                                new TurnEvent.Defeat(pid, winnerName)));
            }
        });
    }

    private static List<Integer> concat(List<Integer> a, List<Integer> b) {
        var out = new ArrayList<>(a);
        out.addAll(b);
        return List.copyOf(out);
    }

    private static String listNos(List<Integer> nos) {
        return nos.stream().sorted().map(String::valueOf).reduce((x, y) -> x + "," + y).orElse("");
    }

    record MergedArrival(int ships, List<Integer> localNos) {
    }
}
