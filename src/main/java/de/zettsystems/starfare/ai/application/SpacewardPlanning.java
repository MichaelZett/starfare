package de.zettsystems.starfare.ai.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.fleet.application.FleetService;

public interface SpacewardPlanning {
    void plan(GameState state, FleetService fleets);
}
