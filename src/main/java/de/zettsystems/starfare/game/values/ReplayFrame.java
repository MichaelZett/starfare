package de.zettsystems.starfare.game.values;

import de.zettsystems.starfare.report.values.TurnReport;

import java.util.List;
import de.zettsystems.starfare.economy.domain.Industry;
import org.jspecify.annotations.Nullable;
import java.util.Map;

/** Immutable map state after one resolved turn, retained for game review. */
public record ReplayFrame(int turn, List<StarSystem> systems, List<Fleet> fleets,
                          Map<Integer, TurnReport> reports, @Nullable Map<Integer, Industry> industries,
        @Nullable GameOutcome outcome) {
    public ReplayFrame(int turn, List<StarSystem> systems, List<Fleet> fleets,
                          Map<Integer, TurnReport> reports, @Nullable Map<Integer, Industry> industries) {
        this(turn, systems, fleets, reports, industries, null);
    }

    public ReplayFrame(int turn, List<StarSystem> systems, List<Fleet> fleets, Map<Integer, TurnReport> reports) {
        this(turn, systems, fleets, reports, Map.of());
    }
    public ReplayFrame {
        systems = List.copyOf(systems);
        fleets = List.copyOf(fleets);
        reports = Map.copyOf(reports);
        industries = industries == null ? Map.of() : Map.copyOf(industries);
    }
}
