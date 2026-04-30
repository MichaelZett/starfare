package de.zettsystems.starfare.ai.application;

import de.zettsystems.starfare.fleet.application.FleetService;
import de.zettsystems.starfare.game.domain.GameState;

public interface AiService {
    void doAiTurns(GameState state, FleetService fleetService);
}
