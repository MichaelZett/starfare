package de.zettsystems.starfare.game.values;

import de.zettsystems.starfare.report.values.TurnReport;

import java.util.List;
import java.util.Map;

/** Immutable map state after one resolved turn, retained for game review. */
public record ReplayFrame(int turn, List<StarSystem> systems, List<Fleet> fleets,
                          Map<Integer, TurnReport> reports) {
    public ReplayFrame {
        systems = List.copyOf(systems);
        fleets = List.copyOf(fleets);
        reports = Map.copyOf(reports);
    }
}
