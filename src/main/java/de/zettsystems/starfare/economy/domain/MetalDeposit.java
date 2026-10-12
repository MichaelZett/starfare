package de.zettsystems.starfare.economy.domain;

/** Finite geology survives conquest; extraction never recreates an exhausted deposit. */
public record MetalDeposit(long remaining, int yieldPerPoint) {
    public MetalDeposit {
        if (remaining < 0 || yieldPerPoint <= 0) { throw new IllegalArgumentException("Invalid metal deposit"); }
    }
    public long extraction(int points) {
        if (points < 0) { throw new IllegalArgumentException("Negative mining allocation"); }
        return Math.min(remaining, (long) points * yieldPerPoint);
    }
    public MetalDeposit extract(long amount) {
        if (amount < 0 || amount > remaining) { throw new IllegalArgumentException("Extraction exceeds deposit"); }
        return new MetalDeposit(remaining - amount, yieldPerPoint);
    }
}
