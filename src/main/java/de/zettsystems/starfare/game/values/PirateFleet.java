package de.zettsystems.starfare.game.values;

/**
 * Pirates without a seat: the neutral garrison of an exploded sun, on its way to a random system.
 * Like a {@link Fleet}, it is resolved in the round before {@code arrivalTurn}.
 */
public record PirateFleet(int id, int fromSystemId, int toSystemId, int ships, int arrivalTurn) {
    public PirateFleet {
        if (ships <= 0) { throw new IllegalArgumentException("Pirates need ships"); }
    }

    /** Storms and quakes move the arrival like that of every other fleet in hyperspace. */
    public PirateFleet rescheduled(int arrival) {
        return new PirateFleet(id, fromSystemId, toSystemId, ships, arrival);
    }

    /** The navigation computer panics at a destroyed target and jumps to another system. */
    public PirateFleet panicJump(int newTarget, int newArrival) {
        return new PirateFleet(id, toSystemId, newTarget, ships, newArrival);
    }
}
