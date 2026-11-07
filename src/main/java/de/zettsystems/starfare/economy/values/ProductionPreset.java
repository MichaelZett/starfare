package de.zettsystems.starfare.economy.values;

import org.jspecify.annotations.Nullable;

import java.util.Set;

/** Whole-point presets preserve local locks and never redistribute on failure. */
public enum ProductionPreset {
    SHIPS, METAL, INDUSTRY, EVEN, AUTARK;

    public enum Rejection { NONE, LOCKED, NO_CAPACITY, LIMIT, EMPTY_DEPOSIT, TOO_SMALL }
    public record Result(AllocationSplit allocation, Rejection rejection) {
        public boolean applicable() { return rejection == Rejection.NONE; }
    }

    public Result apply(IndustryView industry, AllocationSplit current, Set<Integer> locks) {
        int[] limits = limits(industry);
        MetalView metal = industry.metal();
        int[] values = new int[3];
        int remaining = industry.usableCapacity();
        for (int index : locks) {
            if (current.at(index) > limits[index]) { return rejected(current, Rejection.LIMIT); }
            values[index] = current.at(index); remaining -= values[index];
        }
        if (remaining <= 0) { return rejected(current, Rejection.NO_CAPACITY); }
        if (this == AUTARK) { return autark(industry, current, locks, values, remaining); }
        Rejection rejection = this == EVEN ? spreadEvenly(metal, locks, limits, values, remaining)
                : single(metal, locks, limits, values, remaining);
        if (rejection != Rejection.NONE) { return rejected(current, rejection); }
        return new Result(new AllocationSplit(values[0], values[1], values[2]), Rejection.NONE);
    }

    private Rejection single(@Nullable MetalView metal, Set<Integer> locks, int[] limits, int[] values, int remaining) {
        int target = switch (this) { case SHIPS -> 0; case METAL -> 1; case INDUSTRY -> 2; default -> throw new IllegalStateException(); };
        if (locks.contains(target)) { return Rejection.LOCKED; }
        if (this == METAL && metal != null && metal.remaining() == 0) { return Rejection.EMPTY_DEPOSIT; }
        if (remaining > limits[target]) { return Rejection.LIMIT; }
        values[target] = remaining;
        return Rejection.NONE;
    }

    private static Rejection spreadEvenly(@Nullable MetalView metal, Set<Integer> locks, int[] limits, int[] values, int remaining) {
        if (metal != null && metal.remaining() == 0) { limits[1] = 0; }
        int left = remaining;
        while (left > 0) {
            int before = left;
            for (int index = 0; index < 3 && left > 0; index++) {
                if (!locks.contains(index) && values[index] < limits[index]) { values[index]++; left--; }
            }
            if (left == before) { return Rejection.LIMIT; }
        }
        return Rejection.NONE;
    }

    private static Result autark(IndustryView industry, AllocationSplit current, Set<Integer> locks,
                                 int[] values, int remaining) {
        if (locks.contains(0) || locks.contains(1)) { return rejected(current, Rejection.LOCKED); }
        MetalView metal = industry.metal();
        if (metal == null || metal.remaining() == 0) {
            return rejected(current, Rejection.EMPTY_DEPOSIT);
        }
        if (remaining < 2) { return rejected(current, Rejection.TOO_SMALL); }
        values[0] = remaining / 2;
        values[1] = remaining - values[0];
        return new Result(new AllocationSplit(values[0], values[1], values[2]), Rejection.NONE);
    }

    private static Result rejected(AllocationSplit current, Rejection rejection) { return new Result(current, rejection); }

    public static int[] limits(IndustryView industry) {
        int capacity = industry.usableCapacity();
        return new int[]{capacity, industry.metal() == null ? 0 : capacity, industry.atMaximum() ? 0 : capacity};
    }

    public static AllocationSplit saved(IndustryView industry) {
        MetalView metal = industry.metal();
        int mining = metal == null ? 0 : metal.miningPoints();
        return new AllocationSplit(industry.usableCapacity() - mining - industry.expansionAllocation(), mining,
                industry.expansionAllocation());
    }
}
