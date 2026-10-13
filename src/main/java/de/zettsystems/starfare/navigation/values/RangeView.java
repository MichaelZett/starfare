package de.zettsystems.starfare.navigation.values;

import java.util.Map;

/**
 * Direct flight range around one source during target selection. Every other system is
 * classified as reachable directly, only over own or allied stations, or not at all, and
 * carries its straight-line distance as a share of the direct range.
 */
public record RangeView(int sourceId, double range, Map<Integer, Target> targets) {
    public enum Reach { DIRECT, STATIONS, NONE }

    public record Target(Reach reach, double share) {
        /** Percentage for display: never rounds a target beyond the range down to 100 %. */
        public double percent() { return RangeShare.percent(share); }
    }

    public RangeView { targets = Map.copyOf(targets); }

    public Target target(int systemId) {
        Target target = targets.get(systemId);
        if (target == null) { throw new IllegalArgumentException("Unknown target " + systemId); }
        return target;
    }
}
