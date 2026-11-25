package de.zettsystems.starfare.navigation.values;

import java.util.Map;

/**
 * Direct flight range around one source during target selection. Every other system is
 * classified as reachable directly, only over own or allied stations, or not at all.
 */
public record RangeView(int sourceId, double range, Map<Integer, Target> targets) {
    public enum Reach { DIRECT, STATIONS, NONE }

    /** {@code stations}: intermediate stops of the fastest route, only for {@link Reach#STATIONS}; otherwise 0. */
    public record Target(Reach reach, int stations) {
        public Target {
            if (stations < 0 || (reach == Reach.STATIONS) != (stations > 0)) {
                throw new IllegalArgumentException("Stations only belong to a route over stations");
            }
        }
    }

    public RangeView { targets = Map.copyOf(targets); }

    public Target target(int systemId) {
        Target target = targets.get(systemId);
        if (target == null) { throw new IllegalArgumentException("Unknown target " + systemId); }
        return target;
    }
}
