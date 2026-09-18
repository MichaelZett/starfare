package de.zettsystems.starfare.ai.application;

import de.zettsystems.starfare.fleet.application.FleetService;
import de.zettsystems.starfare.game.domain.GameState;

public interface AiService {
    /** Legt die Befehle aller KI-Spieler fuer diese Runde in die Warteschlange, wie ein Mensch es tut. */
    void planAiTurns(GameState state, FleetService fleetService);
}
