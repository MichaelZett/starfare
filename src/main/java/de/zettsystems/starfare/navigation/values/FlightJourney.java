package de.zettsystems.starfare.navigation.values;

import java.util.List;

/** The complete itinerary and the actual phase of an independently owned fleet. */
public record FlightJourney(List<Integer> stations, int legIndex, Phase phase, int readyTurn, int finalArrivalTurn, @org.jspecify.annotations.Nullable Boolean returningHome) {
    public FlightJourney(List<Integer> stations, int legIndex, Phase phase, int readyTurn, int finalArrivalTurn) {
        this(stations, legIndex, phase, readyTurn, finalArrivalTurn, null);
    }
    public boolean evacuating() { return Boolean.TRUE.equals(returningHome); }
    public boolean stationed() { return !flying() && !hasNextLeg(); }
    public enum Phase { FLYING, STATION, BLOCKED, PARTNER }
    public FlightJourney {
        stations = List.copyOf(stations);
        if (stations.size() < 2 || stations.stream().distinct().count() != stations.size() || legIndex < 1 || legIndex >= stations.size() || readyTurn < 0 || finalArrivalTurn < 1 || (legIndex == stations.size() - 1 && phase == Phase.STATION)
                || (phase == Phase.PARTNER && legIndex != stations.size() - 1)) {
            throw new IllegalArgumentException("Invalid flight itinerary");
        }
        java.util.Objects.requireNonNull(phase);
        if ((phase == Phase.FLYING && readyTurn != 0) || (phase != Phase.FLYING && readyTurn < 1)) {
            throw new IllegalArgumentException("Invalid station readiness");
        }
    }
    public int destination() { return stations.getLast(); }
    public boolean hasNextLeg() { return legIndex < stations.size() - 1; }
    public boolean flying() { return phase == Phase.FLYING; }
    public boolean blocked() { return phase == Phase.BLOCKED; }
    public FlightJourney land(int turn) { return new FlightJourney(stations, legIndex, hasNextLeg() ? Phase.STATION : Phase.PARTNER, turn + 1, finalArrivalTurn, returningHome); }
    public FlightJourney block() { return new FlightJourney(stations, legIndex, Phase.BLOCKED, readyTurn, finalArrivalTurn, returningHome); }
    public FlightJourney resume(int ready, int arrival) { return new FlightJourney(stations, legIndex, Phase.STATION, ready, arrival, returningHome); }
    public FlightJourney depart(int arrival) { return new FlightJourney(stations, legIndex + 1, Phase.FLYING, 0, arrival, returningHome); }
    public FlightJourney delayed() { return new FlightJourney(stations, legIndex, phase, readyTurn == 0 ? 0 : readyTurn + 1, finalArrivalTurn + 1, returningHome); }
}
