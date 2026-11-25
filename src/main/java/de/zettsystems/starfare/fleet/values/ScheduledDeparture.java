package de.zettsystems.starfare.fleet.values;

import org.jspecify.annotations.Nullable;

/**
 * Part of a joint attack that starts later so that it arrives together with the slower sources. Until the
 * departure round the ships stay in the source garrison and defend it, but they are reserved and cannot be sent
 * a second time. In the departure round the order becomes a fleet at the same step as a queued send.
 *
 * @param ownerId       the player who planned it
 * @param fromSystemId  the source system
 * @param toSystemId    the target system
 * @param ships         the planned amount; fewer are sent if fewer are left
 * @param departureTurn the round in which the fleet is launched
 * @param arrivalTurn   the common arrival planned for the joint attack, for display only
 * @param beneficiaryId the ally who takes a conquest in extended games, or {@code null} for the owner
 */
public record ScheduledDeparture(int ownerId, int fromSystemId, int toSystemId, int ships, int departureTurn,
                                 int arrivalTurn, @Nullable Integer beneficiaryId) {
}
