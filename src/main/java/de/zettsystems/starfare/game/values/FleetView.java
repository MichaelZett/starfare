package de.zettsystems.starfare.game.values;

/**
 * UI projection for a fleet row in the grid.
 */
public record FleetView(
        int localNo,
        String fromName,
        String toName,
        int ships,
        int eta,
        int fleetId, // globalId, nur intern für Aktionen, nicht anzeigen
        int fromSystemId,
        int toSystemId,
        boolean standing
) { }
