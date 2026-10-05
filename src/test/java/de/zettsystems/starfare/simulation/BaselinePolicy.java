package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.ai.application.DefaultAiService;
import de.zettsystems.starfare.ai.application.DefaultSpacewardPlanning;
import de.zettsystems.starfare.economy.application.DefaultEconomyService;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.application.PlayerViewBuilder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.RulesetRef;
import java.util.HashMap;
import java.util.List;

/** Runs the existing AI unchanged on an isolated copy; only this seat can plan. */
final class BaselinePolicy {
    private BaselinePolicy() { }

    static StrategyOrders plan(GameState original, int player, PlayerViewBuilder views) {
        var state = GameState.copyOf(original);
        state.pendingOrders().clear();
        state.players().replaceAll(p -> p.id() == player ? p.asAi() : p.asHuman());
        var fleets = new DefaultFleetService();
        if (state.ruleset().spaceward()) {
            new DefaultSpacewardPlanning(new DefaultEconomyService(), views).plan(state, fleets);
        } else {
            new DefaultAiService().planAiTurns(state, fleets);
        }
        var expansion = new HashMap<Integer, Integer>();
        state.systems().stream().filter(s -> java.util.Objects.equals(s.ownerId(), player)).forEach(s -> {
            var industry = state.industries().get(s.id());
            if (industry != null) { expansion.put(s.id(), industry.expansionAllocation()); }
        });
        var sends = state.pendingOrders().getOrDefault(player, List.of()).stream()
                .filter(FleetOrder.Send.class::isInstance).map(FleetOrder.Send.class::cast).toList();
        return new StrategyOrders(sends, expansion);
    }
}
