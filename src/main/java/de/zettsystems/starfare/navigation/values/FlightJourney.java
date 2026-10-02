package de.zettsystems.starfare.navigation.values;

import java.util.List;

/** The complete itinerary and the actual phase of an independently owned fleet. */
public record FlightJourney(List<Integer> stations, int legIndex, Phase phase, int readyTurn, int finalArrivalTurn) {
    public enum Phase { FLYING, STATION, BLOCKED }
    public FlightJourney {
        stations = List.copyOf(stations);
        if (stations.size() < 2 || stations.stream().distinct().count() != stations.size() || legIndex < 1 || legIndex >= stations.size() || readyTurn < 0 || finalArrivalTurn < 1 || (phase != Phase.FLYING && legIndex == stations.size() - 1)) {
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
    public FlightJourney land(int turn) { return new FlightJourney(stations, legIndex, Phase.STATION, turn + 1, finalArrivalTurn); }
    public FlightJourney block() { return new FlightJourney(stations, legIndex, Phase.BLOCKED, readyTurn, finalArrivalTurn); }
    public FlightJourney resume(int ready, int arrival) { return new FlightJourney(stations, legIndex, Phase.STATION, ready, arrival); }
    public FlightJourney depart(int arrival) { return new FlightJourney(stations, legIndex + 1, Phase.FLYING, 0, arrival); }
    public FlightJourney delayed() { return new FlightJourney(stations, legIndex, phase, readyTurn == 0 ? 0 : readyTurn + 1, finalArrivalTurn + 1); }
}
