package de.zettsystems.starfare.ai.application;

import de.zettsystems.starfare.fleet.application.FleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Player;

public interface ProfilePlanning {
    void plan(GameState state, FleetService fleets, Player player);
}
