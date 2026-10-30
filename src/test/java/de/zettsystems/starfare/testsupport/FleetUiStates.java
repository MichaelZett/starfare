package de.zettsystems.starfare.testsupport;

import de.zettsystems.starfare.diplomacy.values.AllianceGroup;
import de.zettsystems.starfare.diplomacy.values.DiplomacyState;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.navigation.values.NavigationSettings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Stable navigation and coalition games for server-side journeys and browser rendering. */
public final class FleetUiStates {
    private FleetUiStates() { }

    public static GameState navigation() {
        GameState state = spaceward();
        state.players().addAll(List.of(new Player(1, "Navigator", false, "#56b4e9", "Navigation Empire"),
                new Player(2, "Opponent", false, "#d55e00")));
        state.systems().addAll(List.of(new StarSystem(1, "Alpha", 400, 350, 1, 50, 4, false),
                new StarSystem(2, "Beta", 850, 350, 1, 5, 4, false),
                new StarSystem(3, "Gamma", 1300, 350, 1, 5, 4, false),
                new StarSystem(4, "Delta", 1750, 350, 2, 100, 4, false)));
        remoteSystems(state, false);
        initializeHumans(state, List.of(1, 2), 450);
        assertThat(new DefaultFleetService().addStandingOrder(state, 1, 1, 4, 2)).isPositive();
        return state;
    }

    public static GameState coalition() {
        GameState state = spaceward();
        state.configureLobby(true, true, true, 0);
        state.players().addAll(List.of(new Player(1, "Commander", false, "#56b4e9"),
                new Player(2, "Partner", false, "#d55e00"), new Player(3, "Opponent", false, "#009e73")));
        state.systems().addAll(List.of(new StarSystem(1, "Alpha", 400, 350, 1, 200, 4, false),
                new StarSystem(2, "Beta", 850, 350, 2, 100, 4, false),
                new StarSystem(3, "Gamma", 1300, 350, 3, 20, 4, false),
                new StarSystem(4, "Delta", 1750, 350, null, 30, 4, true)));
        remoteSystems(state, true);
        initializeHumans(state, List.of(1, 2, 3), 1500);
        state.agreeTreaties(new DiplomacyState(2,
                List.of(new AllianceGroup(1, Set.of(1, 2), 3, Map.of())), List.of()));
        int arrival = state.turn() + state.travelRounds(1, 3);
        state.fleets().addAll(List.of(new Fleet(100, 2, 1, 2, 3, 40, 1, arrival),
                new Fleet(101, 1, 2, 1, 4, 40, 1, arrival),
                new Fleet(102, 2, 2, 2, 4, 20, 1, arrival),
                new Fleet(103, 3, 1, 3, 4, 10, 1, arrival)));
        return state;
    }

    private static GameState spaceward() {
        GameState state = new GameState();
        state.resetForNewGame();
        state.rememberSetup(GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD));
        return state;
    }

    private static void remoteSystems(GameState state, boolean owned) {
        for (int index = 5; index <= 8; index++) {
            state.systems().add(new StarSystem(index, "Remote " + index, 2500 + index * 60,
                    1300 + index * 50, owned ? 1 : null, owned ? 20 : 200, 4, !owned));
        }
    }

    private static void initializeHumans(GameState state, List<Integer> players, int range) {
        state.joinedHumanPlayerIds().addAll(players);
        state.originalHumanPlayerIds().addAll(players);
        players.forEach(player -> state.intel().put(player, new HashMap<>()));
        state.configureNavigation(new NavigationSettings(range));
        state.start();
    }
}
