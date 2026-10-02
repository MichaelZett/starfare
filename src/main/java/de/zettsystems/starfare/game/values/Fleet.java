package de.zettsystems.starfare.game.values;

import de.zettsystems.starfare.navigation.values.RoutePreview;
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
        @Nullable FlightJourney journey,
        @Nullable Integer beneficiaryId
) {
    public Fleet(int globalId, int ownerId, int localNo, int fromSystemId, int toSystemId, int ships, int launchTurn, int arrivalTurn) {
        this(globalId, ownerId, localNo, fromSystemId, toSystemId, ships, launchTurn, arrivalTurn, null);
    }
    public Fleet(int globalId, int ownerId, int localNo, int fromSystemId, int toSystemId, int ships,
                 int launchTurn, int arrivalTurn, @Nullable FlightJourney journey) {
        this(globalId, ownerId, localNo, fromSystemId, toSystemId, ships, launchTurn, arrivalTurn, journey, null);
    }
    public int conquestOwner() { return beneficiaryId == null ? ownerId : beneficiaryId; }
    public Fleet designateConquest(int beneficiary) {
        return new Fleet(globalId, ownerId, localNo, fromSystemId, toSystemId, ships, launchTurn, arrivalTurn, journey, beneficiary);
    }
    public Fleet surviveBattle(int survivors) {
        if (survivors < 0 || survivors > ships) { throw new IllegalArgumentException("Combat cannot create ships"); }
        return new Fleet(globalId, ownerId, localNo, fromSystemId, toSystemId, survivors, launchTurn, arrivalTurn, journey, beneficiaryId);
    }
    public boolean inFlight() { return journey == null || journey.flying(); }
    /** A hostile intermediate arrival ends the itinerary at the site of combat. */
    public Fleet endJourneyAfterBattle() {
        return new Fleet(globalId, ownerId, localNo, fromSystemId, toSystemId, ships, launchTurn, arrivalTurn, null, beneficiaryId);
    }
    public Fleet landForRefueling() {
        FlightJourney route = java.util.Objects.requireNonNull(journey);
        return new Fleet(globalId, ownerId, localNo, fromSystemId, toSystemId, ships, launchTurn, arrivalTurn, route.land(arrivalTurn), beneficiaryId);
    }
    public Fleet holdAtBlockedStation() {
        FlightJourney route = java.util.Objects.requireNonNull(journey);
        return new Fleet(globalId, ownerId, localNo, fromSystemId, toSystemId, ships, launchTurn, arrivalTurn, route.block(), beneficiaryId);
    }
    public Fleet resumeAtStation(int ready, int finalArrival) {
        FlightJourney route = java.util.Objects.requireNonNull(journey);
        return new Fleet(globalId, ownerId, localNo, fromSystemId, toSystemId, ships, launchTurn, arrivalTurn, route.resume(ready, finalArrival), beneficiaryId);
    }
    public Fleet continueFlight(int from, int to, int departure, int arrival, int finalArrival) {
        FlightJourney route = java.util.Objects.requireNonNull(journey);
        return new Fleet(globalId, ownerId, localNo, from, to, ships, departure, arrival, route.depart(finalArrival), beneficiaryId);
    }
    public boolean evacuating() { return journey != null && journey.evacuating(); }
    public Fleet prepareReturn(RoutePreview route, int turn, int firstArrival) {
        return new Fleet(globalId, ownerId, localNo, route.systems().getFirst(), route.systems().get(1), ships,
                turn, firstArrival, new FlightJourney(route.systems(), 1, FlightJourney.Phase.FLYING, 0, turn + route.rounds(), true), beneficiaryId);
    }
    public Fleet beginEvacuation() {
        var route = java.util.Objects.requireNonNull(journey);
        return new Fleet(globalId, ownerId, localNo, fromSystemId, toSystemId, ships, launchTurn, arrivalTurn,
                new FlightJourney(route.stations(), route.legIndex(), route.phase(), route.readyTurn(), route.finalArrivalTurn(), true), beneficiaryId);
    }
    public Fleet dockAtPartner() {
        var route = journey == null ? new FlightJourney(java.util.List.of(fromSystemId, toSystemId), 1, FlightJourney.Phase.FLYING, 0, arrivalTurn) : journey;
        return new Fleet(globalId, ownerId, localNo, fromSystemId, toSystemId, ships, launchTurn, arrivalTurn, route.land(arrivalTurn), beneficiaryId);
    }
    public Fleet requireReturn() {
        var route = java.util.Objects.requireNonNull(journey);
        return new Fleet(globalId, ownerId, localNo, fromSystemId, toSystemId, ships, launchTurn, arrivalTurn,
                new FlightJourney(route.stations(), route.legIndex(), FlightJourney.Phase.BLOCKED, Math.max(1, route.readyTurn()), route.finalArrivalTurn(), true), beneficiaryId);
    }
    /**
     * Slips the fleet by one turn (used by the "wait this turn" order).
     */
    public Fleet delayedByOneTurn() {
        return new Fleet(globalId, ownerId, localNo, fromSystemId, toSystemId, ships, launchTurn,
                inFlight() ? arrivalTurn + 1 : arrivalTurn, journey == null ? null : journey.delayed(), beneficiaryId);
    }
}
