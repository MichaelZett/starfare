package de.zettsystems.starfare.game.values;

import org.jspecify.annotations.Nullable;

import java.util.List;
import de.zettsystems.starfare.economy.values.IndustryView;

/**
 * Client-facing view of a star system with fog-of-war data.
 * Ownership/garrison/production/color/lastSeen are null when unknown due to fog-of-war.
 * Sensor-range garrison and production values are estimates when {@code approximate} is true.
 * {@code availableShips} is only known for the viewer's own systems: the garrison minus ships already
 * committed to queued orders.
 */
public record VisibleSystem(
        int id, String name, double x, double y,
        @Nullable Integer ownerId, @Nullable Integer garrison, @Nullable Integer productionPerTurn,
        boolean fullyVisible, @Nullable String colorHex, @Nullable Integer lastSeenTurn, boolean approximate,
        @Nullable Integer routedProduction, @Nullable Integer availableShips,
        List<SystemOwnership> ownershipHistory, @Nullable IndustryView industry
) {
    public VisibleSystem(int id, String name, double x, double y, @Nullable Integer ownerId,
                         @Nullable Integer garrison, @Nullable Integer productionPerTurn, boolean fullyVisible,
                         @Nullable String colorHex, @Nullable Integer lastSeenTurn, boolean approximate,
                         @Nullable Integer routedProduction, @Nullable Integer availableShips,
                         List<SystemOwnership> ownershipHistory) {
        this(id, name, x, y, ownerId, garrison, productionPerTurn, fullyVisible, colorHex, lastSeenTurn,
                approximate, routedProduction, availableShips, ownershipHistory, null);
    }

    /** A system that is not the viewer's own: no sendable ships are known. */
    public VisibleSystem(int id, String name, double x, double y, @Nullable Integer ownerId,
                         @Nullable Integer garrison, @Nullable Integer productionPerTurn, boolean fullyVisible,
                         @Nullable String colorHex, @Nullable Integer lastSeenTurn, boolean approximate,
                         @Nullable Integer routedProduction, List<SystemOwnership> ownershipHistory) {
        this(id, name, x, y, ownerId, garrison, productionPerTurn, fullyVisible, colorHex, lastSeenTurn, approximate,
                routedProduction, null, ownershipHistory);
    }

    public VisibleSystem {
        ownershipHistory = List.copyOf(ownershipHistory);
    }
}
