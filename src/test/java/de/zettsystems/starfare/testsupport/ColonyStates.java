package de.zettsystems.starfare.testsupport;

import de.zettsystems.starfare.economy.domain.Colony;
import de.zettsystems.starfare.economy.domain.ColonyEconomy;
import de.zettsystems.starfare.economy.domain.ColonyGenesis;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.navigation.values.NavigationSettings;
import java.util.HashMap;
import java.util.List;

public final class ColonyStates {
    private ColonyStates() { }
    public static GameState ready() {
        GameState state = new GameState();
        state.rememberSetup(GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD_COLONIES));
        state.players().addAll(List.of(new Player(1, "Colonist", false, "#111111"),
                new Player(2, "Opponent", false, "#222222")));
        state.systems().addAll(List.of(new StarSystem(1, "Alpha", 100, 100, 1, 12, 10, false, 10),
                new StarSystem(2, "Beta", 200, 100, 1, 3, 4, false),
                new StarSystem(3, "Enemy", 3000, 1800, 2, 30, 8, false),
                new StarSystem(4, "Neutral", 2600, 1500, null, 4, 5, true)));
        state.initializeIndustry();
        var colonies = new HashMap<>(ColonyGenesis.forGalaxy(state.systems(), 42));
        colonies.put(1, new ColonyEconomy(colonies.get(1).quality(), new Colony(690)));
        state.establishColonies(colonies);
        state.configureNavigation(new NavigationSettings(5000));
        state.joinedHumanPlayerIds().addAll(List.of(1, 2));
        state.originalHumanPlayerIds().addAll(List.of(1, 2));
        return state;
    }
    public static GameState running() {
        var state = ready(); state.start(); return state;
    }
}
