package de.zettsystems.starfare.testsupport;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import java.util.List;

public final class SpacewardStates {
    private SpacewardStates() { }
    public static GameState running() {
        GameState state = new GameState();
        state.resetForNewGame();
        state.rememberSetup(GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD));
        state.players().addAll(List.of(new Player(1, "Human", false, "#111111"), new Player(2, "Opponent", false, "#222222")));
        state.systems().add(new StarSystem(1, "Alpha", 100, 100, 1, 12, 10, false, 10));
        state.systems().add(new StarSystem(2, "Beta", 200, 100, 1, 3, 4, false));
        state.systems().add(new StarSystem(3, "Enemy", 3000, 1800, 2, 30, 8, false));
        state.systems().add(new StarSystem(4, "Neutral", 2600, 1500, null, 4, 5, true));
        state.joinedHumanPlayerIds().addAll(List.of(1, 2));
        state.originalHumanPlayerIds().addAll(List.of(1, 2));
        state.configureNavigation(new de.zettsystems.starfare.navigation.values.NavigationSettings(5000));
        state.start();
        return state;
    }
}
