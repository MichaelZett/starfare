package de.zettsystems.starfare.game.values;

import org.jspecify.annotations.Nullable;

/** Personal totals shown when a game ends; the economy is present only for Spaceward. */
public record GameOutcomeStatistics(int rounds, int systems, int shipsBuilt, int shipsDestroyed, int shipsLost,
                                    @Nullable EconomyTotals economy) {
    public GameOutcomeStatistics(int rounds, int systems, int shipsBuilt, int shipsDestroyed, int shipsLost) {
        this(rounds, systems, shipsBuilt, shipsDestroyed, shipsLost, null);
    }
}
