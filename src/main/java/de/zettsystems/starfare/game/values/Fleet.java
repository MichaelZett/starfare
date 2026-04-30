package de.zettsystems.starfare.game.values;

/**
 * Immutable fleet transfer between star systems.
 */
public record Fleet(
        int globalId,      // spielweit eindeutig
        int ownerId,
        int localNo,       // fortlaufend je Spieler: 1,2,3,...
        int fromSystemId,
        int toSystemId,
        int ships,
        int launchTurn,
        int arrivalTurn
) {
    /**
     * Slips the fleet by one turn (used by the "wait this turn" order).
     */
    public Fleet delayedByOneTurn() {
        return new Fleet(globalId, ownerId, localNo, fromSystemId, toSystemId, ships, launchTurn, arrivalTurn + 1);
    }
}
