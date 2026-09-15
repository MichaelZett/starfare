package de.zettsystems.starfare.game.values;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Immutable star system on the map. Neutral systems have no owner.
 * <p>
 * Mutating operations are exposed as fachliche Domain-Methoden (z.B.
 * {@link #produce()}, {@link #captureBy(int, int)}), nicht als generische
 * {@code withX}-Setter.
 */
public record StarSystem(int id, String name, double x, double y,
                         @Nullable Integer ownerId, int garrison, int productionPerTurn, boolean neutral,
                         int garrisonReserve) {

    /** Compatibility constructor for generated maps and older tests; reserves default to zero. */
    public StarSystem(int id, String name, double x, double y, @Nullable Integer ownerId,
                      int garrison, int productionPerTurn, boolean neutral) {
        this(id, name, x, y, ownerId, garrison, productionPerTurn, neutral, 0);
    }

    /** Restores systems persisted before permanent garrisons were introduced. */
    @JsonCreator
    public static StarSystem restore(
            @JsonProperty("id") int id,
            @JsonProperty("name") String name,
            @JsonProperty("x") double x,
            @JsonProperty("y") double y,
            @JsonProperty("ownerId") @Nullable Integer ownerId,
            @JsonProperty("garrison") int garrison,
            @JsonProperty("productionPerTurn") int productionPerTurn,
            @JsonProperty("neutral") boolean neutral,
            @JsonProperty("garrisonReserve") @Nullable Integer garrisonReserve) {
        return new StarSystem(id, name, x, y, ownerId, garrison, productionPerTurn, neutral,
                garrisonReserve == null ? 0 : garrisonReserve);
    }

    /**
     * Adds this system's production output to its garrison.
     */
    public StarSystem produce() {
        return new StarSystem(id, name, x, y, ownerId, garrison + productionPerTurn, productionPerTurn, neutral, garrisonReserve);
    }

    /**
     * Produziert und gibt im selben Zug {@code routed} Schiffe an Produktions-
     * verlegungen ab. Reicht die Produktion nicht, zehrt der Rest an der Garnison —
     * mehr als vorhanden verlässt das System aber nie.
     */
    public StarSystem produceAndRoute(int routed) {
        int available = garrison + productionPerTurn;
        int shipped = Math.clamp(routed, 0, available);
        return new StarSystem(id, name, x, y, ownerId, available - shipped, productionPerTurn, neutral, garrisonReserve);
    }

    /**
     * Adds incoming friendly ships to the garrison.
     */
    public StarSystem reinforce(int ships) {
        if (ships < 0) {
            throw new IllegalArgumentException("ships must be >= 0");
        }
        return new StarSystem(id, name, x, y, ownerId, garrison + ships, productionPerTurn, neutral, garrisonReserve);
    }

    /**
     * Sends ships out from this system; reduces the garrison accordingly.
     */
    public StarSystem launchFleet(int ships) {
        if (ships < 0) {
            throw new IllegalArgumentException("ships must be >= 0");
        }
        if (ships > availableShips()) {
            throw new IllegalStateException("cannot launch more ships than garrison");
        }
        return new StarSystem(id, name, x, y, ownerId, garrison - ships, productionPerTurn, neutral, garrisonReserve);
    }

    /**
     * Records a successful conquest: new owner takes the system with the surviving attackers as garrison.
     */
    public StarSystem captureBy(int newOwner, int remainingShips) {
        if (remainingShips < 0) {
            throw new IllegalArgumentException("remainingShips must be >= 0");
        }
        return new StarSystem(id, name, x, y, newOwner, remainingShips, productionPerTurn, false, 0);
    }

    /**
     * Records a successful defense: garrison shrinks to the surviving defenders, owner stays.
     */
    public StarSystem afterDefense(int defendersLeft) {
        if (defendersLeft < 0) {
            throw new IllegalArgumentException("defendersLeft must be >= 0");
        }
        return new StarSystem(id, name, x, y, ownerId, defendersLeft, productionPerTurn, neutral, garrisonReserve);
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
        return new StarSystem(id, name, x, y, newOwner, startGarrison, startProduction, false, 0);
    }

    /** Repositions the system on the map (used by the layout space-out pass). */
    public StarSystem relocateTo(double nx, double ny) {
        return new StarSystem(id, name, nx, ny, ownerId, garrison, productionPerTurn, neutral, garrisonReserve);
    }

    /** Ships available for orders after the configured permanent garrison has been retained. */
    public int availableShips() {
        return Math.max(0, garrison - garrisonReserve);
    }

    /** Sets the number of ships which may never be sent away from this system. */
    public StarSystem reserveGarrison(int reserve) {
        return new StarSystem(id, name, x, y, ownerId, garrison, productionPerTurn, neutral,
                Math.clamp(reserve, 0, garrison));
    }
}
