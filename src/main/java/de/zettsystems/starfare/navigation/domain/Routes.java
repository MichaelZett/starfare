package de.zettsystems.starfare.navigation.domain;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.navigation.values.RoutePreview;
import java.util.*;

/** Pure planning used by commands, production routing, AI and previews. */
public final class Routes {
    private Routes() { }
    public static boolean limited(GameState state) { return state.ruleset().spaceward(); }
    public static boolean stationAllowed(GameState state, int owner, int system) {
        StarSystem station = state.systems().stream().filter(candidate -> candidate.id() == system).findFirst().orElse(null);
        return station != null && (Objects.equals(station.ownerId(), owner) || state.allied(owner, station.ownerId()));
    }
    public static boolean withinRange(GameState state, int from, int to) {
        if (state.systems().stream().noneMatch(system -> system.id() == from) || state.systems().stream().noneMatch(system -> system.id() == to)) { return false; }
        return !limited(state) || state.distance(from, to) <= state.navigationSettings().orElseThrow().range();
    }
    public static Optional<RoutePreview> plan(GameState state, int owner, int from, int to) {
        if (from == to || !stationAllowed(state, owner, from) || state.systems().stream().noneMatch(system -> system.id() == to)) { return Optional.empty(); }
        if (withinRange(state, from, to)) { return Optional.of(preview(state, List.of(from, to))); }
        return new Search(state, owner, from, to).find();
    }

    /** All legal travel durations from one station, without repeating a full search for every destination. */
    public static Map<Integer, Integer> reachableRounds(GameState state, int owner, int from) {
        if (!stationAllowed(state, owner, from)) { return Map.of(); }
        return reachableRounds(state, owner).getOrDefault(from, Map.of());
    }

    /** Batched station network for planners comparing many sources and destinations. */
    public static Map<Integer, Map<Integer, Integer>> reachableRounds(GameState state, int owner) {
        Set<Integer> stations = new HashSet<>();
        for (StarSystem system : state.systems()) {
            if (stationAllowed(state, owner, system.id())) { stations.add(system.id()); }
        }
        if (stations.isEmpty()) { return Map.of(); }
        var edges = travelEdges(state);
        Map<Integer, Map<Integer, Integer>> routes = new HashMap<>();
        for (int source : stations) { routes.put(source, shortestRounds(source, stations, edges)); }
        return Map.copyOf(routes);
    }

    private static Map<Integer, Map<Integer, Integer>> travelEdges(GameState state) {
        Map<Integer, Map<Integer, Integer>> edges = new HashMap<>();
        for (StarSystem source : state.systems()) {
            Map<Integer, Integer> destinations = new HashMap<>();
            for (StarSystem target : state.systems()) {
                if (source.id() != target.id() && withinRange(state, source.id(), target.id())) {
                    destinations.put(target.id(), state.travelRounds(source.id(), target.id()));
                }
            }
            edges.put(source.id(), destinations);
        }
        return edges;
    }

    private static Map<Integer, Integer> shortestRounds(int from, Set<Integer> stations,
                                                       Map<Integer, Map<Integer, Integer>> edges) {
        Map<Integer, Integer> costs = new HashMap<>();
        Set<Integer> visited = new HashSet<>();
        costs.put(from, 0);
        while (true) {
            Integer current = costs.keySet().stream().filter(id -> stations.contains(id) && !visited.contains(id))
                    .min(Comparator.comparingInt((Integer id) -> costs.getOrDefault(id, Integer.MAX_VALUE))
                            .thenComparingInt(Integer::intValue)).orElse(null);
            if (current == null) { break; }
            visited.add(current);
            int departureCost = costs.getOrDefault(current, 0) + (current == from ? 0 : 1);
            edges.getOrDefault(current, Map.of()).forEach((target, duration) ->
                    costs.merge(target, departureCost + duration, Math::min));
        }
        costs.remove(from);
        return Map.copyOf(costs);
    }

    /** Only automatic evacuation may start at a station without current treaty access. */
    public static Optional<RoutePreview> returnHome(GameState state, int owner, int from) {
        return state.systems().stream().filter(system -> Objects.equals(system.ownerId(), owner) && system.id() != from)
                .map(system -> withinRange(state, from, system.id()) ? Optional.of(preview(state, List.of(from, system.id()))) : new Search(state, owner, from, system.id()).find())
                .flatMap(Optional::stream).min(Comparator.comparingInt(RoutePreview::rounds).thenComparing(route -> route.systems().getLast()));
    }
    private static final class Search {
        private final GameState state;
        private final int owner;
        private final int from;
        private final int to;
        private final Map<Integer, Integer> costs = new HashMap<>();
        private final Map<Integer, Integer> previous = new HashMap<>();
        private final Set<Integer> visited = new HashSet<>();
        private Search(GameState state, int owner, int from, int to) {
            this.state = state; this.owner = owner; this.from = from; this.to = to;
            costs.put(from, 0);
        }
        private Optional<RoutePreview> find() {
            while (true) {
                Integer current = costs.keySet().stream().filter(id -> !visited.contains(id))
                        .min(Comparator.comparingInt((Integer id) -> costs.getOrDefault(id, Integer.MAX_VALUE))
                                .thenComparingInt(Integer::intValue)).orElse(null);
                if (current == null) { return Optional.empty(); }
                if (current == to) { return Optional.of(preview(state, reconstruct(previous, from, to))); }
                visited.add(current); explore(current);
            }
        }
        private void explore(int current) {
            for (StarSystem target : state.systems()) {
                if (!usable(current, target.id())) { continue; }
                int cost = costs.getOrDefault(current, Integer.MAX_VALUE) + state.travelRounds(current, target.id())
                        + (target.id() == to ? 0 : 1);
                if (cost < costs.getOrDefault(target.id(), Integer.MAX_VALUE)) {
                    costs.put(target.id(), cost); previous.put(target.id(), current);
                }
            }
        }
        private boolean usable(int current, int target) {
            return !visited.contains(target) && (target == to || stationAllowed(state, owner, target))
                    && withinRange(state, current, target);
        }
    }

    private static List<Integer> reconstruct(Map<Integer, Integer> previous, int from, int to) {
        List<Integer> path = new ArrayList<>(); path.add(to);
        while (path.getLast() != from) { path.add(Objects.requireNonNull(previous.get(path.getLast()))); }
        Collections.reverse(path); return List.copyOf(path);
    }
    public static RoutePreview preview(GameState state, List<Integer> path) {
        int rounds = Math.max(0, path.size() - 2);
        for (int index = 1; index < path.size(); index++) { rounds += state.travelRounds(path.get(index - 1), path.get(index)); }
        double range = state.navigationSettings().map(settings -> settings.range()).orElse(0.0);
        return new RoutePreview(path, path.stream().map(id -> state.getSystem(id).name()).toList(), rounds, range);
    }
    public static int remainingRounds(GameState state, List<Integer> stations, int legIndex) {
        return preview(state, stations.subList(legIndex, stations.size())).rounds();
    }
}
