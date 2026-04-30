package de.zettsystems.starfare.game.values;

import org.jspecify.annotations.Nullable;

/**
 * UI-friendly representation of a queued fleet order for this turn.
 * {@code index} is the position in the player's pendingOrders list and is used for cancellation.
 * System ids, ship count and standing-order id are null for non-send/non-standing orders.
 */
public record PlannedOrder(int index, String type,
                           @Nullable Integer fromSystemId, @Nullable Integer toSystemId,
                           String fromSystem, String toSystem, @Nullable Integer ships,
                           boolean standing, @Nullable Integer standingOrderId) {
}
