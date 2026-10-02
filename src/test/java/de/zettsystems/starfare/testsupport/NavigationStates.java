package de.zettsystems.starfare.testsupport;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.navigation.values.NavigationSettings;
import java.util.List;

public final class NavigationStates {
    private NavigationStates() { }
    public static GameState running() {
        GameState state = new GameState(); state.resetForNewGame();
        state.rememberSetup(GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD));
        state.players().addAll(List.of(new Player(1, "Navigator", false, "#56b4e9"), new Player(2, "Opponent", false, "#d55e00")));
        state.systems().addAll(List.of(
                new StarSystem(1, "Alpha", 100, 100, 1, 50, 4, false),
                new StarSystem(2, "Beta", 200, 100, 1, 5, 4, false),
                new StarSystem(3, "Gamma", 300, 100, 1, 5, 4, false),
                new StarSystem(4, "Delta", 400, 100, 2, 5, 4, false),
                new StarSystem(5, "Remote", 3000, 1800, null, 50, 4, true),
                new StarSystem(6, "Far", 2800, 1700, null, 50, 4, true),
                new StarSystem(7, "Distant", 2600, 1600, null, 50, 4, true),
                new StarSystem(8, "Other", 2400, 1500, null, 50, 4, true)));
        state.joinedHumanPlayerIds().addAll(List.of(1, 2)); state.originalHumanPlayerIds().addAll(List.of(1, 2));
        state.intel().put(1, new java.util.HashMap<>()); state.intel().put(2, new java.util.HashMap<>());
        state.configureNavigation(new NavigationSettings(100)); state.start(); return state;
    }
}
