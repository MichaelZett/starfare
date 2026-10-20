package de.zettsystems.starfare.research.values;

import java.util.Map;
import java.util.TreeMap;

/** Exact immutable ship counts by build-time technology; no automatic upgrades. */
public record ShipCohorts(Map<Integer, Integer> levels) {
    public ShipCohorts {
        levels = Map.copyOf(levels);
        if (levels.entrySet().stream().anyMatch(e -> e.getKey() < 0 || e.getKey() > ShipResearch.MAX_LEVEL || e.getValue() <= 0)) {
            throw new IllegalArgumentException("Invalid ship generations");
        }
        if (levels.values().stream().mapToLong(Integer::longValue).sum() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Ship count exceeds supported size");
        }
    }
    public static ShipCohorts of(int level, int count) {
        if (count < 0) { throw new IllegalArgumentException("Negative ship count"); }
        return new ShipCohorts(count == 0 ? Map.of() : Map.of(level, count));
    }
    public int count() { return Math.toIntExact(levels.values().stream().mapToLong(Integer::longValue).sum()); }
    public long strengthTenths() { return levels.entrySet().stream().mapToLong(e -> e.getValue() * (10L + e.getKey())).sum(); }
    public double strength() { return strengthTenths() / 10.0; }
    public ShipCohorts merge(ShipCohorts other) {
        var next = new TreeMap<>(levels);
        other.levels.forEach((level, count) -> next.merge(level, count, Math::addExact));
        return new ShipCohorts(next);
    }
    /** Launch newest ships first; recycling removes oldest ships first. */
    public ShipCohorts take(int count, boolean newestFirst) {
        if (count < 0 || count > count()) { throw new IllegalArgumentException("Insufficient ships"); }
        var sorted = new TreeMap<>(levels);
        var chosen = new TreeMap<Integer, Integer>();
        int remaining = count;
        for (var entry : (newestFirst ? sorted.descendingMap() : sorted).entrySet()) {
            int take = Math.min(remaining, entry.getValue());
            if (take > 0) { chosen.put(entry.getKey(), take); remaining -= take; }
        }
        return new ShipCohorts(chosen);
    }
    public ShipCohorts minus(ShipCohorts other) {
        var next = new TreeMap<>(levels);
        other.levels.forEach((level, count) -> {
            int left = next.getOrDefault(level, 0) - count;
            if (left < 0) { throw new IllegalArgumentException("Cannot create ship generations"); }
            if (left == 0) { next.remove(level); } else { next.put(level, left); }
        });
        return new ShipCohorts(next);
    }
    /** Proportional casualties; largest remainders, then lower level, break integer ties. */
    public ShipCohorts survive(int survivors) {
        if (survivors < 0 || survivors > count()) { throw new IllegalArgumentException("Combat cannot create ships"); }
        if (survivors == 0) { return of(0, 0); }
        int total = count();
        var next = new TreeMap<Integer, Integer>();
        levels.forEach((level, count) -> next.put(level, (int) ((long) survivors * count / total)));
        int remainder = survivors - next.values().stream().mapToInt(Integer::intValue).sum();
        var ranked = levels.keySet().stream().sorted(java.util.Comparator
                .comparingLong((Integer level) -> (long) survivors * java.util.Objects.requireNonNull(levels.get(level)) % total).reversed()
                .thenComparingInt(Integer::intValue)).toList();
        for (int index = 0; index < remainder; index++) { next.merge(ranked.get(index), 1, Integer::sum); }
        next.values().removeIf(count -> count == 0);
        return new ShipCohorts(next);
    }
}
