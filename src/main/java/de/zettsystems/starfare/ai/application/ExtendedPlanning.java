package de.zettsystems.starfare.ai.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.fleet.application.FleetService;

public interface ExtendedPlanning {
    void plan(GameState state, FleetService fleets);
}
