package de.zettsystems.starfare.economy.values;


/** Whole capacity points; index order also defines the tie break. */
public record AllocationSplit(int ships, int mining, int expansion) {
    public AllocationSplit {
        if (ships < 0 || mining < 0 || expansion < 0) { throw new IllegalArgumentException("Negative allocation"); }
    }
    public int total() { return ships + mining + expansion; }
    public int at(int index) { return switch (index) { case 0 -> ships; case 1 -> mining; case 2 -> expansion;
        default -> throw new IllegalArgumentException("Unknown allocation"); }; }

    /** Keeps pinned values and distributes remaining points by largest remainder. */
    public AllocationSplit adjust(int index, int requested, boolean[] pinned, int[] limits) {
        if (pinned.length != 3 || limits.length != 3 || pinned[index]) { return this; }
        int[] result = {ships, mining, expansion};
        int fixed = 0;
        for (int i = 0; i < 3; i++) { if (pinned[i]) { fixed += result[i]; } }
        result[index] = Math.clamp(requested, 0, Math.min(limits[index], total() - fixed));
        int remaining = total() - fixed - result[index];
        boolean[] free = new boolean[3];
        for (int i = 0; i < 3; i++) {
            free[i] = i != index && !pinned[i];
            if (free[i]) { result[i] = 0; }
        }
        int budget = remaining;
        while (remaining > 0) {
            int chosen = choose(result, free, limits, budget);
            if (chosen < 0) { result[index] += remaining; break; }
            result[chosen]++;
            remaining--;
        }
        return new AllocationSplit(result[0], result[1], result[2]);
    }

    private int choose(int[] result, boolean[] free, int[] limits, int budget) {
        int chosen = -1;
        double best = -Double.MAX_VALUE;
        int weights = 0;
        for (int i = 0; i < 3; i++) { if (free[i] && result[i] < limits[i]) { weights += at(i); } }
        for (int i = 0; i < 3; i++) {
            if (!free[i] || result[i] >= limits[i]) { continue; }
            double score = weights == 0 ? -i : (double) at(i) * budget / weights - result[i];
            if (score > best) { best = score; chosen = i; }
        }
        return chosen;
    }
}
