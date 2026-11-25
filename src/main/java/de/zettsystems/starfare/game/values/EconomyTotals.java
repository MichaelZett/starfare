package de.zettsystems.starfare.game.values;

import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;
import java.util.Collection;

/** Personal industry built over a game of the extended rules, summed from the player's round summaries. */
public record EconomyTotals(int industryBuilt) {

    /** Sums the capacity one player gained over the given reports. */
    public static EconomyTotals of(Collection<TurnReport> reports, int playerId) {
        int built = 0;
        for (TurnReport report : reports) {
            for (TurnEvent event : report.events()) {
                if (event instanceof TurnEvent.EconomySummary(int owner, int capacityGained) && owner == playerId) {
                    built += capacityGained;
                }
            }
        }
        return new EconomyTotals(built);
    }
}
