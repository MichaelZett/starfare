package de.zettsystems.starfare.research.values;

/** First calibrated research rules: +10% combat strength per level, increasing deterministic cost. */
public record ShipResearch(int percent, int level, long progress) {
    public static final int MAX_LEVEL = 20;
    public static final ShipResearch INITIAL = new ShipResearch(0, 0, 0);
    public ShipResearch {
        if (percent < 0 || percent > 100 || level < 0 || level > MAX_LEVEL || progress < 0
                || (level == MAX_LEVEL ? progress != 0 : progress >= cost(level))) {
            throw new IllegalArgumentException("Invalid ship research");
        }
    }
    public static long cost(int level) { return 100L * (level + 1) * (level + 1); }
    public ShipResearch allocate(int percentage) { return new ShipResearch(percentage, level, progress); }
    public ShipResearch advance(long points) {
        if (points < 0) { throw new IllegalArgumentException("Negative research"); }
        long remaining = Math.addExact(progress, points);
        int next = level;
        while (next < MAX_LEVEL && remaining >= cost(next)) { remaining -= cost(next++); }
        return new ShipResearch(percent, next, next == MAX_LEVEL ? 0 : remaining);
    }
}
