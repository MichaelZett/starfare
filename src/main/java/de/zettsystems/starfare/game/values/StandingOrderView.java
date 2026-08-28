package de.zettsystems.starfare.game.values;

/**
 * UI-friendly snapshot of a {@link StandingOrder} with resolved system names,
 * the source system's production-per-turn and the routed amount.
 */
public record StandingOrderView(int id, int fromSystemId, int toSystemId,
                                String fromSystem, String toSystem, int productionPerTurn, int ships) {
}
