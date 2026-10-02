package de.zettsystems.starfare.game.values;

import de.zettsystems.starfare.navigation.values.FlightJourney;
import org.jspecify.annotations.Nullable;

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
        int arrivalTurn,
        @Nullable FlightJourney journey
) {
    public Fleet(int globalId, int ownerId, int localNo, int fromSystemId, int toSystemId, int ships, int launchTurn, int arrivalTurn) {
        this(globalId, ownerId, localNo, fromSystemId, toSystemId, ships, launchTurn, arrivalTurn, null);
    }
    public boolean inFlight() { return journey == null || journey.flying(); }
    public Fleet landForRefueling() {
        FlightJourney route = java.util.Objects.requireNonNull(journey);
        return new Fleet(globalId, ownerId, localNo, fromSystemId, toSystemId, ships, launchTurn, arrivalTurn, route.land(arrivalTurn));
    }
    public Fleet holdAtBlockedStation() {
        FlightJourney route = java.util.Objects.requireNonNull(journey);
        return new Fleet(globalId, ownerId, localNo, fromSystemId, toSystemId, ships, launchTurn, arrivalTurn, route.block());
    }
    public Fleet resumeAtStation(int ready, int finalArrival) {
        FlightJourney route = java.util.Objects.requireNonNull(journey);
        return new Fleet(globalId, ownerId, localNo, fromSystemId, toSystemId, ships, launchTurn, arrivalTurn, route.resume(ready, finalArrival));
    }
    public Fleet continueFlight(int from, int to, int departure, int arrival, int finalArrival) {
        FlightJourney route = java.util.Objects.requireNonNull(journey);
        return new Fleet(globalId, ownerId, localNo, from, to, ships, departure, arrival, route.depart(finalArrival));
    }
    /**
     * Slips the fleet by one turn (used by the "wait this turn" order).
     */
    public Fleet delayedByOneTurn() {
        return new Fleet(globalId, ownerId, localNo, fromSystemId, toSystemId, ships, launchTurn,
                inFlight() ? arrivalTurn + 1 : arrivalTurn, journey == null ? null : journey.delayed());
    }
}
