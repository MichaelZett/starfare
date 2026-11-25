package de.zettsystems.starfare.game.values;

import org.jspecify.annotations.Nullable;

/**
 * UI-friendly view of one planned later start of a joint attack: the ships stay in the source garrison until the
 * departure round and then launch so that they arrive together with the rest of the attack.
 */
public record PlannedDeparture(int fromSystemId, int toSystemId, String fromSystem, String toSystem, int ships,
                               int departureTurn, int arrivalTurn, @Nullable Integer beneficiaryId) {
}
