package de.zettsystems.starfare.game.values;

/** A standing relocation shortened because its source builds fewer ships; zero means removed. */
public record RelocationAdjustment(int orderId, int fromSystemId, int toSystemId, String toSystem, int before, int after) {
    public boolean removed() { return after == 0; }
}
