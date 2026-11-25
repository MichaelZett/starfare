package de.zettsystems.starfare.game.values;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;
import de.zettsystems.starfare.research.values.ShipCohorts;

/**
 * Immutable star system on the map. Neutral systems have no owner.
 * <p>
 * Mutating operations are exposed as fachliche Domain-Methoden (z.B.
 * {@link #produce()}, {@link #captureBy(int, int)}), nicht als generische
 * {@code withX}-Setter.
 * <p>
 * Snapshots written before the garrison reserve was removed still carry {@code garrisonReserve}; it is ignored.
 */
@JsonIgnoreProperties({"garrisonReserve"})
public record StarSystem(int id, String name, double x, double y,
                         @Nullable Integer ownerId, int garrison, int productionPerTurn, boolean neutral,
                         ShipCohorts composition) {

    public StarSystem {
        if (composition == null) { composition = ShipCohorts.of(0, garrison); }
        if (composition.count() != garrison) { throw new IllegalArgumentException("Garrison generations disagree with ship count"); }
    }
    /** A system whose garrison consists of ships without research technology. */
    public StarSystem(int id, String name, double x, double y, @Nullable Integer ownerId,
                      int garrison, int productionPerTurn, boolean neutral) {
        this(id, name, x, y, ownerId, garrison, productionPerTurn, neutral, ShipCohorts.of(0, garrison));
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
            @JsonProperty("composition") @Nullable ShipCohorts composition) {
        return new StarSystem(id, name, x, y, ownerId, garrison, productionPerTurn, neutral,
                composition == null ? ShipCohorts.of(0, garrison) : composition);
    }

    /** Plans next round's ship output without changing ships already stationed here. */
    public StarSystem planShipbuilding(int shipsPerTurn) {
        if (shipsPerTurn < 0) { throw new IllegalArgumentException("Ship output must not be negative"); }
        return new StarSystem(id, name, x, y, ownerId, garrison, shipsPerTurn, neutral, composition);
    }
    public StarSystem scrapShips(int ships) {
        if (ships < 0 || ships > garrison) { throw new IllegalArgumentException("Recycling exceeds the garrison"); }
        return new StarSystem(id, name, x, y, ownerId, garrison - ships, productionPerTurn, neutral, composition.minus(composition.take(ships, false)));
    }

    /** Produces using this round's capacity; expanded output becomes available next round. */
    public StarSystem completeShipbuilding(int produced, int routed, int nextOutput) {
        return completeShipbuilding(produced, routed, nextOutput, 0);
    }
    public StarSystem completeShipbuilding(int produced, int routed, int nextOutput, int technology) {
        if (produced < 0 || routed < 0 || routed > garrison + produced || nextOutput < 0) {
            throw new IllegalArgumentException("Industrial production cannot route more ships than it has");
        }
        var built = composition.merge(ShipCohorts.of(technology, produced));
        return new StarSystem(id, name, x, y, ownerId, garrison + produced - routed, nextOutput, neutral,
                built.minus(built.take(routed, true)));
    }

    /** Adds this system's production output to its garrison. */
    public StarSystem produce() {
        return new StarSystem(id, name, x, y, ownerId, garrison + productionPerTurn, productionPerTurn, neutral, composition.merge(ShipCohorts.of(0, productionPerTurn)));
    }

    /**
     * Produziert und gibt im selben Zug {@code routed} Schiffe an Produktions-
     * verlegungen ab. Reicht die Produktion nicht, zehrt der Rest an der Garnison —
     * mehr als vorhanden verlässt das System aber nie.
     */
    public StarSystem produceAndRoute(int routed) {
        int available = garrison + productionPerTurn;
        int shipped = Math.clamp(routed, 0, available);
        return new StarSystem(id, name, x, y, ownerId, available - shipped, productionPerTurn, neutral,
                composition.merge(ShipCohorts.of(0, productionPerTurn)).minus(composition.merge(ShipCohorts.of(0, productionPerTurn)).take(shipped, true)));
    }

    /**
     * Adds incoming friendly ships to the garrison.
     */
    public StarSystem reinforce(int ships) {
        if (ships < 0) {
            throw new IllegalArgumentException("ships must be >= 0");
        }
        return new StarSystem(id, name, x, y, ownerId, garrison + ships, productionPerTurn, neutral, composition.merge(ShipCohorts.of(0, ships)));
    }

    public StarSystem reinforce(ShipCohorts incoming) {
        return new StarSystem(id, name, x, y, ownerId, Math.addExact(garrison, incoming.count()), productionPerTurn,
                neutral, composition.merge(incoming));
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
        return new StarSystem(id, name, x, y, ownerId, garrison - ships, productionPerTurn, neutral, composition.minus(composition.take(ships, true)));
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
        return new StarSystem(id, name, x, y, ownerId, defendersLeft, productionPerTurn, neutral, composition.survive(defendersLeft));
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
        return new StarSystem(id, name, nx, ny, ownerId, garrison, productionPerTurn, neutral, composition);
    }
}
