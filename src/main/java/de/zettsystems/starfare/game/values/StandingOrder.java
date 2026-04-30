package de.zettsystems.starfare.game.values;

/**
 * Production-routed standing order: each turn the source system's production
 * is shipped directly to {@code toSystemId} instead of being added to the
 * source's garrison. Exactly one order per source system; re-adding replaces
 * the previous target. Auto-cancelled when the source changes owner.
 */
public record StandingOrder(int id, int ownerId, int fromSystemId, int toSystemId) {
}
