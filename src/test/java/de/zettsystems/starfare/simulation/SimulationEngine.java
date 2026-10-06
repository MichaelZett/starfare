package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.combat.application.DefaultCombatService;
import de.zettsystems.starfare.combat.application.DefaultCoalitionCombatService;
import de.zettsystems.starfare.diplomacy.application.DefaultDiplomacyService;
import de.zettsystems.starfare.economy.application.DefaultEconomyService;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.application.DefaultPlayerViewBuilder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.navigation.application.DefaultNavigationService;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.turn.application.DefaultTurnEngine;
import de.zettsystems.starfare.turn.application.RoundPipeline;
import de.zettsystems.starfare.turn.application.SpacewardTurnEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

/** Trusted boundary: observations first, decisions second, validated commands third, real engine last. */
final class SimulationEngine {
    enum Intervention { NONE, OMIT_FIRST_SEND, EXPAND_FIRST, SHIPS_FIRST }

    private final DefaultPlayerViewBuilder views = new DefaultPlayerViewBuilder();
    private final DefaultFleetService fleets = new DefaultFleetService();
    private final DefaultEconomyService economy = new DefaultEconomyService();
    private final Random combatRandom = new Random();
    private final Random orderRandom = new Random();
    private final java.util.Map<Integer, CachedRoutes> routeCache = new java.util.HashMap<>();
    private record CachedRoutes(String geometry, java.util.Map<Observation.Route, Integer> routes) { }
    private final DefaultTurnEngine classic;
    private final SpacewardTurnEngine spaceward;
    private final SpacewardTurnEngine allianceSpaceward;

    SimulationEngine() {
        var reports = new DefaultReportService();
        var navigation = new DefaultNavigationService();
        var combat = new DefaultCombatService(reports, combatRandom::nextDouble);
        var coalitions = new DefaultCoalitionCombatService(reports, navigation, _ -> combatRandom.nextDouble());
        var pipeline = new RoundPipeline(combat, reports, fleets, navigation, new DefaultDiplomacyService(),
                coalitions, () -> orderRandom);
        classic = new DefaultTurnEngine(pipeline, (_, _) -> { }, reports, fleets);
        spaceward = new SpacewardTurnEngine(pipeline, economy, (_, _) -> { }, fleets, reports);
        allianceSpaceward = new de.zettsystems.starfare.turn.application.AllianceSpacewardTurnEngine(pipeline, economy, (_, _) -> { }, fleets, reports);
    }

    List<StrategyOrders> plan(GameState state, List<Strategy> strategies) {
        validateStrategies(state, strategies);
        var plans = new ArrayList<StrategyOrders>();
        for (int seat = 0; seat < strategies.size(); seat++) {
            var strategy = strategies.get(seat);
            plans.add(strategy == Strategy.BASELINE ? BaselinePolicy.plan(state, seat + 1, views)
                    : HeuristicPolicy.plan(strategy, observe(state, seat + 1)));
        }
        return List.copyOf(plans);
    }

    private Observation observe(GameState state, int player) {
        var view = views.forPlayer(state, player);
        var geometry = new StringBuilder().append(state.ruleset()).append(state.navigationSettings());
        view.systems().forEach(system -> geometry.append('|').append(system.id()).append(':').append(system.x())
                .append(':').append(system.y()).append(':').append(java.util.Objects.equals(system.ownerId(), player))
                .append(':').append(de.zettsystems.starfare.navigation.domain.Routes.stationAllowed(state, player, system.id())));
        String key = geometry.toString();
        var cached = routeCache.get(player);
        if (cached == null || !cached.geometry().equals(key)) {
            cached = new CachedRoutes(key, Observation.routes(state, player, view));
            routeCache.put(player, cached);
        }
        return Observation.capture(state, player, view, cached.routes());
    }

    List<StrategyOrders> advance(GameState state, List<Strategy> strategies, long combatSeed, Intervention intervention) {
        var plans = new ArrayList<>(plan(state, strategies));
        plans.set(0, intervene(state, plans.getFirst(), intervention));
        for (int seat = 0; seat < plans.size(); seat++) { apply(state, seat + 1, plans.get(seat)); }
        // Absolute round seeds allow forks to share future entropy without copying Random internals.
        combatRandom.setSeed(mix(combatSeed, state.turn()));
        orderRandom.setSeed(mix(combatSeed ^ 0x6a09e667f3bcc909L, state.turn()));
        if (state.ruleset().spaceward()) {
            (state.ruleset().equals(RulesetRef.SPACEWARD_ALLIANCE) ? allianceSpaceward : spaceward).advanceTurn(state);
        }
        else { classic.advanceTurn(state); }
        return List.copyOf(plans);
    }

    private void apply(GameState state, int player, StrategyOrders orders) {
        new TreeMap<>(orders.expansion()).forEach((system, points) -> {
            if (!economy.allocateExpansion(state, player, system, points)) {
                throw new IllegalStateException("Policy submitted invalid industry allocation");
            }
        });
        for (var send : orders.sends()) {
            if (send.ownerId() != player || !fleets.queueSend(state, player,
                    send.fromSystemId(), send.toSystemId(), send.ships(), send.beneficiaryId())) {
                throw new IllegalStateException("Policy submitted invalid fleet order: " + send);
            }
        }
    }

    static StrategyOrders intervene(GameState state, StrategyOrders orders, Intervention intervention) {
        if (intervention == Intervention.NONE) { return orders; }
        if (intervention == Intervention.OMIT_FIRST_SEND) {
            return new StrategyOrders(orders.sends().stream().skip(1).toList(), orders.expansion());
        }
        if (!state.ruleset().spaceward()) { throw new IllegalArgumentException("Industry requires Spaceward"); }
        var expansion = new TreeMap<>(orders.expansion());
        state.systems().stream().filter(s -> java.util.Objects.equals(s.ownerId(), 1)).findFirst().ifPresent(system -> {
            var industry = state.industries().get(system.id());
            int points = intervention == Intervention.EXPAND_FIRST && industry.capacity() < 50 ? industry.capacity() : 0;
            expansion.put(system.id(), points);
        });
        return new StrategyOrders(orders.sends(), expansion);
    }

    private static void validateStrategies(GameState state, List<Strategy> strategies) {
        if (strategies.size() != state.players().size()
                || (!state.ruleset().spaceward() && strategies.stream().anyMatch(Strategy::requiresIndustry))) {
            throw new IllegalArgumentException("Strategies must match seats and rules");
        }
        if (!state.pendingOrders().isEmpty()) { throw new IllegalArgumentException("Simulation expects an unplanned round"); }
    }

    static long mix(long seed, long index) {
        long value = seed + index * 0x9e3779b97f4a7c15L;
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
