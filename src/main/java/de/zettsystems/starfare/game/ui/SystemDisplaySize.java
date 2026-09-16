package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.game.values.VisibleSystem;

/** Calculates the visible diameter of a system without revealing exact production. */
final class SystemDisplaySize {

    static final int UNKNOWN_SIZE = 86;
    private static final int MIN_EXACT_SIZE = 70;
    private static final int MAX_EXACT_SIZE = 118;

    private SystemDisplaySize() {
    }

    static int pixelsFor(VisibleSystem system) {
        Integer production = system.productionPerTurn();
        if (system.fullyVisible() && production != null) {
            return Math.clamp(MIN_EXACT_SIZE + production * 3, MIN_EXACT_SIZE, MAX_EXACT_SIZE);
        }
        if (system.approximate() && production != null) {
            return Math.clamp(MIN_EXACT_SIZE + production * 3, MIN_EXACT_SIZE, MAX_EXACT_SIZE);
        }
        return UNKNOWN_SIZE;
    }
}
