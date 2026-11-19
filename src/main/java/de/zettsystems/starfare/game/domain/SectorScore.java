package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.StarSystem;

import java.util.List;
import java.util.Objects;

/**
 * Score of an empire when a SectorForces game reaches its round limit, after Galactic Empire (1983):
 * 400 per system, 25 per stationed ship, 20 per ship in flight. Destroyed systems no longer count.
 */
public final class SectorScore {
    public static final int POINTS_PER_SYSTEM = 400;
    public static final int POINTS_PER_STATIONED_SHIP = 25;
    public static final int POINTS_PER_SHIP_IN_FLIGHT = 20;

    private SectorScore() {
    }

    public record Line(int playerId, int systems, int stationed, int inFlight, int points) {
    }

    public static Line of(GameState state, int playerId) {
        List<StarSystem> owned = state.systems().stream()
                .filter(system -> Objects.equals(system.ownerId(), playerId) && !state.destroyed(system.id())).toList();
        int stationed = owned.stream().mapToInt(StarSystem::garrison).sum();
        int inFlight = state.fleets().stream().filter(fleet -> fleet.ownerId() == playerId).mapToInt(Fleet::ships).sum();
        int points = owned.size() * POINTS_PER_SYSTEM + stationed * POINTS_PER_STATIONED_SHIP
                + inFlight * POINTS_PER_SHIP_IN_FLIGHT;
        return new Line(playerId, owned.size(), stationed, inFlight, points);
    }
}
