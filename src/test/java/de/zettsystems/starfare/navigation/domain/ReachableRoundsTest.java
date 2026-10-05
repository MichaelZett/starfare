package de.zettsystems.starfare.navigation.domain;

import de.zettsystems.starfare.diplomacy.values.AllianceGroup;
import de.zettsystems.starfare.diplomacy.values.DiplomacyState;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.testsupport.NavigationStates;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ReachableRoundsTest {
    @Test void bulkDurationsMatchNormalRoutesIncludingRefuellingAndBlockedDestinations() {
        var state = NavigationStates.running();
        assertMatchesIndividualQueries(state);
        assertThat(Routes.reachableRounds(state, 1, 1)).containsEntry(4, 8).doesNotContainKeys(1, 5);
        state.updateSystem(2, system -> system.captureBy(2, 5));
        assertMatchesIndividualQueries(state);
        assertThat(Routes.reachableRounds(state, 1, 1)).containsOnlyKeys(2);
        assertThat(Routes.reachableRounds(state, 1, 2)).isEmpty();
    }

    @Test void alliedStationsAndLostAccessUseTheSameRulesAsFleetCommands() {
        var state = NavigationStates.running();
        state.updateSystem(2, system -> system.captureBy(2, 5));
        state.agreeTreaties(new DiplomacyState(2, List.of(new AllianceGroup(1, Set.of(1, 2), 3, Map.of())), List.of()));
        assertMatchesIndividualQueries(state);
        assertThat(Routes.reachableRounds(state, 1, 1)).containsEntry(4, 8);
        state.agreeTreaties(DiplomacyState.EMPTY);
        assertMatchesIndividualQueries(state);
    }

    @Test void unlimitedClassicTravelAlwaysMatchesDirectFlight() {
        var state = new GameState();
        state.systems().addAll(List.of(new StarSystem(1, "A", 0, 0, 1, 10, 5, false),
                new StarSystem(2, "B", 100, 0, 1, 10, 5, false),
                new StarSystem(3, "C", 3000, 0, null, 2, 2, true)));
        assertMatchesIndividualQueries(state);
        assertThat(Routes.reachableRounds(state, 1, 1)).containsEntry(3, state.travelRounds(1, 3));
        assertThat(Routes.reachableRounds(state, 1, 99)).isEmpty();
    }

    private static void assertMatchesIndividualQueries(GameState state) {
        for (var source : state.systems()) {
            var all = Routes.reachableRounds(state, 1, source.id());
            for (var target : state.systems()) {
                var route = Routes.plan(state, 1, source.id(), target.id());
                if (route.isEmpty()) { assertThat(all).doesNotContainKey(target.id()); }
                else { assertThat(all).containsEntry(target.id(), route.orElseThrow().rounds()); }
            }
        }
    }
}
