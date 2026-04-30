package de.zettsystems.starfare.report.values;

import java.util.List;

/**
 * Per-player report for a completed turn. {@code lines} contains human-readable
 * German text; {@code events} contains structured data for visual rendering.
 */
public record TurnReport(int turn, List<String> lines, List<TurnEvent> events) {
    public TurnReport(int turn, List<String> lines) {
        this(turn, lines, List.of());
    }
}
