package de.zettsystems.starfare.navigation.values;

/**
 * Display percentage of a distance relative to the direct range. Close to the limit one decimal
 * is kept, rounded down inside the range and up beyond it, so 100 % always means "still
 * reachable" and a target just outside never reads as exactly 100 %.
 */
public final class RangeShare {
    private RangeShare() { }

    public static double percent(double share) {
        if (!Double.isFinite(share) || share < 0) { throw new IllegalArgumentException("Invalid range share"); }
        double percent = share * 100;
        if (percent < 99 || percent > 101) { return Math.round(percent); }
        if (share <= 1) { return Math.floor(percent * 10 + 1e-9) / 10; }
        return Math.max(100.1, Math.ceil(percent * 10 - 1e-9) / 10);
    }
}
