package de.zettsystems.starfare.game.values;

import org.jspecify.annotations.Nullable;

/**
 * Immutable star system on the map. Neutral systems have no owner.
 * <p>
 * Mutating operations are exposed as fachliche Domain-Methoden (z.B.
 * {@link #produce()}, {@link #captureBy(int, int)}), nicht als generische
 * {@code withX}-Setter.
 */
public record StarSystem(int id, String name, double x, double y,
                         @Nullable Integer ownerId, int garrison, int productionPerTurn, boolean neutral) {

    /**
     * Adds this system's production output to its garrison.
     */
    public StarSystem produce() {
        return new StarSystem(id, name, x, y, ownerId, garrison + productionPerTurn, productionPerTurn, neutral);
    }

    /**
     * Adds incoming friendly ships to the garrison.
     */
    public StarSystem reinforce(int ships) {
        if (ships < 0) {
            throw new IllegalArgumentException("ships must be >= 0");
        }
        return new StarSystem(id, name, x, y, ownerId, garrison + ships, productionPerTurn, neutral);
    }

    /**
     * Sends ships out from this system; reduces the garrison accordingly.
     */
    public StarSystem launchFleet(int ships) {
        if (ships < 0) {
            throw new IllegalArgumentException("ships must be >= 0");
        }
        if (ships > garrison) {
            throw new IllegalStateException("cannot launch more ships than garrison");
        }
        return new StarSystem(id, name, x, y, ownerId, garrison - ships, productionPerTurn, neutral);
    }

    /**
     * Records a successful conquest: new owner takes the system with the surviving attackers as garrison.
     */
    public StarSystem captureBy(int newOwner, int remainingShips) {
        if (remainingShips < 0) {
            throw new IllegalArgumentException("remainingShips must be >= 0");
        }
        return new StarSystem(id, name, x, y, newOwner, remainingShips, productionPerTurn, false);
    }

    /**
     * Records a successful defense: garrison shrinks to the surviving defenders, owner stays.
     */
    public StarSystem afterDefense(int defendersLeft) {
        if (defendersLeft < 0) {
            throw new IllegalArgumentException("defendersLeft must be >= 0");
        }
        return new StarSystem(id, name, x, y, ownerId, defendersLeft, productionPerTurn, neutral);
    }

    /**
     * Initial colonization at game start: assigns owner, starting garrison, and production capacity.
     */
    public StarSystem colonize(int newOwner, int startGarrison, int startProduction) {
        if (startGarrison < 0) {
            throw new IllegalArgumentException("startGarrison must be >= 0");
        }
        if (startProduction < 0) {
            throw new IllegalArgumentException("startProduction must be >= 0");
        }
        return new StarSystem(id, name, x, y, newOwner, startGarrison, startProduction, false);
    }

    /** Repositions the system on the map (used by the layout space-out pass). */
    public StarSystem relocateTo(double nx, double ny) {
        return new StarSystem(id, name, nx, ny, ownerId, garrison, productionPerTurn, neutral);
    }
}
