package de.zettsystems.starfare.navigation.domain;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.navigation.values.RoutePreview;
import java.util.*;

/** Pure planning used by commands, production routing, AI and previews. */
public final class Routes {
    private Routes() { }
    public static boolean limited(GameState state) { return RulesetRef.SPACEWARD.equals(state.ruleset()); }
    public static boolean stationAllowed(GameState state, int owner, int system) {
        StarSystem station = state.systems().stream().filter(candidate -> candidate.id() == system).findFirst().orElse(null);
        return station != null && Objects.equals(station.ownerId(), owner);
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
