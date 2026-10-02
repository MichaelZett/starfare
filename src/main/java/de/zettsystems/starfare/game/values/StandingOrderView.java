package de.zettsystems.starfare.game.values;

import de.zettsystems.starfare.navigation.values.RoutePreview;

/**
 * UI-friendly snapshot of a {@link StandingOrder} with resolved system names,
 * the source system's production-per-turn and the routed amount.
 */
public record StandingOrderView(int id, int fromSystemId, int toSystemId,
                                String fromSystem, String toSystem, int productionPerTurn, int ships,
                                @org.jspecify.annotations.Nullable RoutePreview route, boolean rangeLimited) {
    public StandingOrderView(int id, int fromSystemId, int toSystemId, String fromSystem, String toSystem, int productionPerTurn, int ships) {
        this(id, fromSystemId, toSystemId, fromSystem, toSystem, productionPerTurn, ships, null, false);
    }
}
