package de.zettsystems.starfare.game.values;

import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Personal economy over a game, summed only from recorded round summaries. Missing history is
 * never counted as zero: the coverage says whether the sums span the whole game, start later
 * or have gaps, or whether nothing was recorded at all.
 */
public record EconomyTotals(long metalMined, long metalConsumed, int industryBuilt,
                            Coverage coverage, int firstRound, boolean metal) {
    public enum Coverage { COMPLETE, SINCE, PARTIAL, NOT_RECORDED }

    public boolean recorded() { return coverage != Coverage.NOT_RECORDED; }

    /**
     * @param reports the player's report per resolved round; rounds without a report count as unrecorded
     * @param rounds  every resolved round of the game
     */
    public static EconomyTotals of(Map<Integer, TurnReport> reports, SortedSet<Integer> rounds, int playerId, boolean metal) {
        long mined = 0;
        long consumed = 0;
        int built = 0;
        SortedSet<Integer> recorded = new TreeSet<>();
        for (int round : rounds) {
            TurnReport report = reports.get(round);
            List<TurnEvent> events = report == null ? List.of() : report.events();
            for (TurnEvent event : events) {
                if (event instanceof TurnEvent.EconomySummary summary && summary.playerId() == playerId) {
                    mined += summary.metalMined(); consumed += summary.metalConsumed(); built += summary.capacityGained();
                    recorded.add(round);
                }
            }
        }
        if (recorded.isEmpty()) { return new EconomyTotals(0, 0, 0, Coverage.NOT_RECORDED, 0, metal); }
        int first = recorded.first();
        Coverage coverage;
        if (recorded.equals(rounds)) { coverage = Coverage.COMPLETE; }
        else if (recorded.equals(rounds.tailSet(first))) { coverage = Coverage.SINCE; }
        else { coverage = Coverage.PARTIAL; }
        return new EconomyTotals(mined, consumed, built, coverage, first, metal);
    }
}
