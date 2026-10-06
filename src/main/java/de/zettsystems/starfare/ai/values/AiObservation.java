package de.zettsystems.starfare.ai.values;

import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.game.values.VisibleSystem;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Policies see player-visible facts and legal route durations, never the mutable game state. */
public record AiObservation(int player, PlayerViewState view, Map<Route, Integer> travel, Set<Integer> allies) {
    public record Route(int from, int to) { }
    public AiObservation {
        travel = Map.copyOf(travel);
        allies = Set.copyOf(allies);
    }

    public boolean owned(VisibleSystem system) { return system.fullyVisible() && Objects.equals(system.ownerId(), player); }
    public boolean hostile(VisibleSystem system) {
        return !owned(system) && (system.ownerId() == null || !allies.contains(system.ownerId()));
    }
    public int rounds(int from, int to) { return travel.getOrDefault(new Route(from, to), Integer.MAX_VALUE); }
}
