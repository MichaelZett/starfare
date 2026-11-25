package de.zettsystems.starfare.game.values;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.Nullable;

/**
 * Immutable star system on the map. Neutral systems have no owner.
 * <p>
 * Mutating operations are exposed as fachliche Domain-Methoden (z.B.
 * {@link #produce()}, {@link #captureBy(int, int)}), nicht als generische
 * {@code withX}-Setter. Older snapshots may still carry the removed garrison reserve
 * ({@code garrisonReserve}) or the ship generations of the removed research ({@code composition}); both are ignored.
 */
@JsonIgnoreProperties({"composition", "garrisonReserve"})
public record StarSystem(int id, String name, double x, double y,
                         @Nullable Integer ownerId, int garrison, int productionPerTurn, boolean neutral) {

    /** Plans next round's ship output without changing ships already stationed here. */
    public StarSystem planShipbuilding(int shipsPerTurn) {
        if (shipsPerTurn < 0) { throw new IllegalArgumentException("Ship output must not be negative"); }
        return new StarSystem(id, name, x, y, ownerId, garrison, shipsPerTurn, neutral);
    }

    /** Produces using this round's capacity; expanded output becomes available next round. */
    public StarSystem completeShipbuilding(int produced, int routed, int nextOutput) {
        if (produced < 0 || routed < 0 || routed > garrison + produced || nextOutput < 0) {
            throw new IllegalArgumentException("Industrial production cannot route more ships than it has");
        }
        return new StarSystem(id, name, x, y, ownerId, garrison + produced - routed, nextOutput, neutral);
    }

    /** Adds this system's production output to its garrison. */
    public StarSystem produce() {
        return new StarSystem(id, name, x, y, ownerId, garrison + productionPerTurn, productionPerTurn, neutral);
    }

    /**
     * Produziert und gibt im selben Zug {@code routed} Schiffe an Produktions-
     * verlegungen ab. Reicht die Produktion nicht, zehrt der Rest an der Garnison —
     * mehr als vorhanden verlässt das System aber nie.
     */
    public StarSystem produceAndRoute(int routed) {
        int available = garrison + productionPerTurn;
        int shipped = Math.clamp(routed, 0, available);
        return new StarSystem(id, name, x, y, ownerId, available - shipped, productionPerTurn, neutral);
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

    /** A spaceport quake destroys part of the garrison; the owner stays. */
    public StarSystem loseShips(int lost) {
        return afterDefense(Math.max(0, garrison - Math.max(0, lost)));
    }

    /** A supernova: the system keeps its place on the map but has no owner, ships or production any more. */
    public StarSystem explode() {
        return new StarSystem(id, name, x, y, null, 0, 0, true);
    }

    /** Pirates won: the system turns neutral and the surviving pirates stay as its garrison. */
    public StarSystem fallToPirates(int pirates) {
        if (pirates < 0) { throw new IllegalArgumentException("pirates must be >= 0"); }
        return new StarSystem(id, name, x, y, null, pirates, productionPerTurn, true);
    }

    /** Repositions the system on the map (used by the layout space-out pass). */
    public StarSystem relocateTo(double nx, double ny) {
        return new StarSystem(id, name, nx, ny, ownerId, garrison, productionPerTurn, neutral);
    }
}
