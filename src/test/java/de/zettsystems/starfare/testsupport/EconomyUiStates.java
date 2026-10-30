package de.zettsystems.starfare.testsupport;

import de.zettsystems.starfare.economy.application.DefaultEconomyService;
import de.zettsystems.starfare.economy.domain.MetalEconomy;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.navigation.values.NavigationSettings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Deterministic saved games shared by Browserless journeys and browser layout checks. */
public final class EconomyUiStates {
    private EconomyUiStates() { }

    public static GameState industry() {
        GameState state = new GameState();
        state.resetForNewGame();
        state.rememberSetup(GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD));
        state.players().addAll(List.of(new Player(1, "Industrialist", false, "#56b4e9"),
                new Player(2, "Opponent", false, "#d55e00")));
        state.systems().add(new StarSystem(1, "Alpha", 400, 350, 1, 12, 10, false, 10));
        state.systems().add(new StarSystem(2, "Beta", 800, 350, 1, 3, 4, false));
        state.systems().add(new StarSystem(3, "Enemy", 2800, 1600, 2, 100, 8, false));
        for (int system = 4; system <= 8; system++) {
            state.systems().add(new StarSystem(system, "Remote " + system,
                    2000 + system * 80, 1300 + system * 50, null, 200, 2, true));
        }
        state.joinedHumanPlayerIds().addAll(List.of(1, 2));
        state.originalHumanPlayerIds().addAll(List.of(1, 2));
        state.configureNavigation(new NavigationSettings(5000));
        state.start();
        var fleets = new DefaultFleetService();
        assertThat(fleets.addStandingOrder(state, 1, 1, 3, 7)).isPositive();
        assertThat(fleets.addStandingOrder(state, 1, 1, 4, 3)).isPositive();
        return state;
    }

    public static GameState directProduction() {
        var state = withStock(RulesetRef.SPACEWARD_DIRECT_ECONOMY);
        state.start();
        var fleets = new DefaultFleetService();
        assertThat(fleets.addStandingOrder(state, 1, 1, 2, 6)).isPositive();
        assertThat(fleets.addStandingOrder(state, 1, 1, 3, 4)).isPositive();
        return state;
    }

    public static GameState research() {
        var state = withStock(RulesetRef.SPACEWARD_SHIP_RESEARCH);
        state.start();
        state.planShipResearch(1, 100);
        var economy = new DefaultEconomyService();
        for (int round = 0; round < 7; round++) {
            economy.produce(state, Map.of());
            state.nextTurn();
        }
        state.planShipResearch(1, 0);
        return state;
    }

    private static GameState withStock(RulesetRef rules) {
        return ColonyStates.ready(rules, original -> {
            var stocks = new HashMap<>(original.stocks());
            stocks.replaceAll((_, _) -> 100L);
            return new MetalEconomy(original.deposits(), stocks, original.mining(), original.recycling());
        });
    }
}
