package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.game.application.PlayerViewBuilder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.game.values.VisibleSystem;
import de.zettsystems.starfare.navigation.domain.Routes;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Policies see player-visible facts and legal route durations, never the mutable game state. */
record Observation(int player, PlayerViewState view, Map<Route, Integer> travel, Set<Integer> allies) {
    record Route(int from, int to) { }
    Observation {
        travel = Map.copyOf(travel);
        allies = Set.copyOf(allies);
    }

    static Observation capture(GameState state, int player, PlayerViewBuilder views) {
        PlayerViewState view = views.forPlayer(state, player);
        return capture(state, player, view, routes(state, player, view));
    }

    static Map<Route, Integer> routes(GameState state, int player, PlayerViewState view) {
        Map<Route, Integer> travel = new HashMap<>();
        Map<Integer, Map<Integer, Integer>> limitedRoutes = Routes.limited(state) ? Routes.reachableRounds(state, player) : Map.of();
        for (VisibleSystem source : view.systems()) {
            if (!Objects.equals(source.ownerId(), player) || !source.fullyVisible()) { continue; }
            if (Routes.limited(state)) {
                limitedRoutes.getOrDefault(source.id(), Map.of()).forEach((target, rounds) ->
                        travel.put(new Route(source.id(), target), rounds));
            } else {
                view.systems().stream().filter(target -> target.id() != source.id()).forEach(target ->
                        travel.put(new Route(source.id(), target.id()), state.travelRounds(source.id(), target.id())));
            }
        }
        return Map.copyOf(travel);
    }

    static Observation capture(GameState state, int player, PlayerViewState view, Map<Route, Integer> travel) {
        var allies = state.players().stream().filter(p -> state.allied(player, p.id()))
                .map(p -> p.id()).collect(Collectors.toSet());
        return new Observation(player, view, travel, allies);
    }

    boolean owned(VisibleSystem system) { return system.fullyVisible() && Objects.equals(system.ownerId(), player); }
    boolean hostile(VisibleSystem system) {
        return !owned(system) && (system.ownerId() == null || !allies.contains(system.ownerId()));
    }
    int rounds(int from, int to) { return travel.getOrDefault(new Route(from, to), Integer.MAX_VALUE); }
}
