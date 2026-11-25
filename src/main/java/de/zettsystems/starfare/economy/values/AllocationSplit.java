package de.zettsystems.starfare.economy.values;

/** Whole capacity points of one system, split between shipbuilding (index 0) and expansion (index 1). */
public record AllocationSplit(int ships, int expansion) {
    public static final int SHIPS_INDEX = 0;
    public static final int EXPANSION_INDEX = 1;

    public AllocationSplit {
        if (ships < 0 || expansion < 0) { throw new IllegalArgumentException("Negative allocation"); }
    }

    /** The saved split of an industry. */
    public static AllocationSplit saved(IndustryView industry) {
        return new AllocationSplit(industry.shipbuilding(), industry.expansionAllocation());
    }

    public int total() { return ships + expansion; }

    public int at(int index) {
        return switch (index) {
            case SHIPS_INDEX -> ships;
            case EXPANSION_INDEX -> expansion;
            default -> throw new IllegalArgumentException("Unknown allocation");
        };
    }

    /** Sets one share within its limit; the other share receives the rest of the same total. */
    public AllocationSplit adjust(int index, int requested, int[] limits) {
        int value = Math.clamp(requested, 0, Math.min(limits[index], total()));
        int other = total() - value;
        return index == SHIPS_INDEX ? new AllocationSplit(value, other) : new AllocationSplit(other, value);
    }

    /** Upper limits per share: shipbuilding may take everything, expansion nothing at the maximum. */
    public static int[] limits(IndustryView industry) {
        return new int[]{industry.capacity(), industry.atMaximum() ? 0 : industry.capacity()};
    }
}
