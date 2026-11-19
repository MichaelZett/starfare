package de.zettsystems.starfare.game.values;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Runtime state of the SectorForces random events: home systems (never explode), suns that explode
 * at the given round, destroyed systems and pirate fleets.
 *
 * @param unstableSystems system id → round whose resolution destroys it
 */
public record CosmicState(Set<Integer> homeSystems, Set<Integer> destroyedSystems,
                          Map<Integer, Integer> unstableSystems, List<PirateFleet> pirates, int nextPirateId) {
    public static final CosmicState EMPTY = new CosmicState(Set.of(), Set.of(), Map.of(), List.of(), 1);

    public CosmicState {
        homeSystems = homeSystems == null ? Set.of() : Set.copyOf(homeSystems);
        destroyedSystems = destroyedSystems == null ? Set.of() : Set.copyOf(destroyedSystems);
        unstableSystems = unstableSystems == null ? Map.of() : Map.copyOf(unstableSystems);
        pirates = pirates == null ? List.of() : List.copyOf(pirates);
        nextPirateId = Math.max(1, nextPirateId);
    }

    public static CosmicState orEmpty(@Nullable CosmicState state) {
        return state == null ? EMPTY : state;
    }

    public CosmicState rememberHomes(Collection<Integer> homes) {
        return new CosmicState(Set.copyOf(homes), destroyedSystems, unstableSystems, pirates, nextPirateId);
    }

    public boolean home(int systemId) { return homeSystems.contains(systemId); }
    public boolean destroyed(int systemId) { return destroyedSystems.contains(systemId); }
    public boolean unstable(int systemId) { return unstableSystems.containsKey(systemId); }

    /** The sun of the system turns unstable and explodes when {@code explosionTurn} is resolved. */
    public CosmicState destabilize(int systemId, int explosionTurn) {
        Map<Integer, Integer> next = new HashMap<>(unstableSystems);
        next.put(systemId, explosionTurn);
        return new CosmicState(homeSystems, destroyedSystems, next, pirates, nextPirateId);
    }

    /** Unstable systems whose explosion is due in the given round, in id order. */
    public List<Integer> dueToExplode(int turn) {
        return unstableSystems.entrySet().stream().filter(entry -> entry.getValue() <= turn)
                .map(Map.Entry::getKey).sorted().toList();
    }

    public CosmicState explode(int systemId) {
        Map<Integer, Integer> unstable = new HashMap<>(unstableSystems);
        unstable.remove(systemId);
        Set<Integer> destroyed = new HashSet<>(destroyedSystems);
        destroyed.add(systemId);
        return new CosmicState(homeSystems, destroyed, unstable, pirates, nextPirateId);
    }

    public CosmicState launchPirates(int from, int to, int ships, int arrivalTurn) {
        List<PirateFleet> next = new ArrayList<>(pirates);
        next.add(new PirateFleet(nextPirateId, from, to, ships, arrivalTurn));
        return new CosmicState(homeSystems, destroyedSystems, unstableSystems, next, nextPirateId + 1);
    }

    /** Applies a change to every pirate fleet. */
    public CosmicState movePirates(UnaryOperator<PirateFleet> change) {
        return new CosmicState(homeSystems, destroyedSystems, unstableSystems, pirates.stream().map(change).toList(),
                nextPirateId);
    }

    /** Removes the pirate fleets that landed or lost their way. */
    public CosmicState dropPirates(Predicate<PirateFleet> gone) {
        return new CosmicState(homeSystems, destroyedSystems, unstableSystems,
                pirates.stream().filter(gone.negate()).toList(), nextPirateId);
    }
}
