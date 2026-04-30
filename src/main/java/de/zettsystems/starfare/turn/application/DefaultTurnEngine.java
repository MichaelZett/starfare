package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.ai.application.AiService;
import de.zettsystems.starfare.combat.application.CombatService;
import de.zettsystems.starfare.fleet.application.FleetService;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.report.application.ReportService;
import de.zettsystems.starfare.report.values.TurnEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        applyOrders(state);
        java.util.Set<Integer> routedSystems = fleetService.applyStandingOrdersForProduction(state);
        applyProduction(state, routedSystems);
        applyWaitOrders(state);
        resolveArrivals(state);
        aiService.doAiTurns(state, fleetService);
        checkVictory(state);
        state.nextTurn();
    }

    private void applyProduction(GameState state, java.util.Set<Integer> routedSystems) {
        for (StarSystem s : state.systems()) {
            if (s.ownerId() == null || s.neutral()) {
                continue;
            }
            if (!routedSystems.contains(s.id())) {
                state.updateSystem(s.id(), StarSystem::produce);
            }
            reportService.appendEvent(state, s.ownerId(),
                    new TurnEvent.Production(s.ownerId(), s.id(), s.name(), s.productionPerTurn()));
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
        var attackers = ownerMap.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().ships, a.getValue().ships))
                .toList();
        for (var atk : attackers) {
            combatService.resolveAttack(state, atk.getKey(), toId, atk.getValue().ships, atk.getValue().localNos);
        }
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
                .collect(java.util.stream.Collectors.groupingBy(StarSystem::ownerId, java.util.stream.Collectors.counting()));
        counts.forEach((pid, c) -> {
            if (c > total / 2) {
                state.endGame(pid);
                reportService.appendEvent(state, pid, new TurnEvent.Victory(pid));
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
