package de.zettsystems.starfare.ai.application;

import de.zettsystems.starfare.ai.domain.AiPolicy;
import de.zettsystems.starfare.ai.values.AiObservation;
import de.zettsystems.starfare.economy.application.EconomyService;
import de.zettsystems.starfare.fleet.application.FleetService;
import de.zettsystems.starfare.game.application.PlayerViewBuilder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.navigation.domain.Routes;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Captures visible facts before applying a selected profile under the caller's game lock. */
@Service
public class DefaultProfilePlanning implements ProfilePlanning {
    private final PlayerViewBuilder views;
    private final EconomyService economy;

    public DefaultProfilePlanning(PlayerViewBuilder views, EconomyService economy) {
        this.views = views;
        this.economy = economy;
    }

    @Override
    public void plan(GameState state, FleetService fleets, Player player) {
        var view = views.forPlayer(state, player.id());
        var allies = state.players().stream().filter(p -> state.allied(player.id(), p.id()))
                .map(Player::id).collect(Collectors.toSet());
        var observation = new AiObservation(player.id(), view, routes(state, player.id(), view), allies);
        var orders = AiPolicy.plan(player.aiStrategy(), observation);
        orders.expansion().forEach((system, points) -> economy.allocateExpansion(state, player.id(), system, points));
        orders.sends().forEach(order -> fleets.queueSend(state, player.id(), order.fromSystemId(), order.toSystemId(), order.ships()));
    }

    private static Map<AiObservation.Route, Integer> routes(GameState state, int player, PlayerViewState view) {
        Map<AiObservation.Route, Integer> travel = new HashMap<>();
        var limited = Routes.limited(state) ? Routes.reachableRounds(state, player) : Map.<Integer, Map<Integer, Integer>>of();
        for (var source : view.systems()) {
            if (!Objects.equals(source.ownerId(), player) || !source.fullyVisible()) { continue; }
            if (Routes.limited(state)) {
                limited.getOrDefault(source.id(), Map.of()).forEach((target, rounds) ->
                        travel.put(new AiObservation.Route(source.id(), target), rounds));
            } else {
                view.systems().stream().filter(target -> target.id() != source.id()).forEach(target ->
                        travel.put(new AiObservation.Route(source.id(), target.id()), state.travelRounds(source.id(), target.id())));
            }
        }
        return travel;
    }
}
