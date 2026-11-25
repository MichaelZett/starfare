package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.game.values.VisibleSystem;

import java.util.OptionalInt;

/**
 * Free ships worth a quiet hint on the map: more than the system builds in one round, because
 * freshly built ships are not idle yet. Planned flights are already deducted.
 */
final class IdleShips {

    private IdleShips() {
    }

    static OptionalInt of(VisibleSystem system) {
        Integer free = system.availableShips();
        Integer builtPerRound = system.productionPerTurn();
        if (free == null || builtPerRound == null || free <= builtPerRound) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(free);
    }
}
