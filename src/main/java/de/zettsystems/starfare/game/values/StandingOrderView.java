package de.zettsystems.starfare.game.values;

/**
 * UI-friendly snapshot of a {@link StandingOrder} with resolved system names
 * and the source system's current production-per-turn.
 */
public record StandingOrderView(int id, int fromSystemId, int toSystemId,
                                String fromSystem, String toSystem, int productionPerTurn) {
}
