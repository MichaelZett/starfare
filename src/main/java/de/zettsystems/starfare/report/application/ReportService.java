package de.zettsystems.starfare.report.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.report.values.TurnEvent;

public interface ReportService {

    void appendEvent(GameState state, int playerId, TurnEvent event);

    void appendReport(GameState state, int playerId, String line);

    boolean hasSignificantEventsFor(GameState state, int playerId);
}
