package de.zettsystems.starfare.navigation.domain;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.navigation.values.RangeView;
import de.zettsystems.starfare.testsupport.NavigationStates;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class RangeViewTest {
    @Test void targetsAreDirectViaStationsOrUnreachableUsingTheCommandRules() {
        GameState state = NavigationStates.running();
        RangeView view = Routes.rangeFrom(state, 1, 1).orElseThrow();
        assertThat(view.range()).isEqualTo(100);
        assertThat(view.target(2)).isEqualTo(new RangeView.Target(RangeView.Reach.DIRECT, 0));
        assertThat(view.target(3)).isEqualTo(new RangeView.Target(RangeView.Reach.STATIONS, 1));
        assertThat(view.target(4)).isEqualTo(new RangeView.Target(RangeView.Reach.STATIONS, 2));
        assertThat(view.target(8)).isEqualTo(new RangeView.Target(RangeView.Reach.NONE, 0));
        assertThat(view.targets()).doesNotContainKey(1);
        for (int target : view.targets().keySet()) {
            var planned = Routes.plan(state, 1, 1, target);
            assertThat(view.target(target).reach() != RangeView.Reach.NONE).isEqualTo(planned.isPresent());
            assertThat(view.target(target).reach() == RangeView.Reach.DIRECT).isEqualTo(Routes.withinRange(state, 1, target));
            planned.filter(route -> route.systems().size() > 2).ifPresent(route ->
                    assertThat(view.target(target).stations()).isEqualTo(route.systems().size() - 2));
        }
    }

    @Test void justOutsideTheBoundaryIsNoLongerDirect() {
        GameState state = NavigationStates.running();
        state.updateSystem(2, system -> system.relocateTo(200.001, 100));
        assertThat(Routes.rangeFrom(state, 1, 1).orElseThrow().target(2).reach()).isEqualTo(RangeView.Reach.NONE);
        state.updateSystem(2, system -> system.relocateTo(170.71, 170.71));
        assertThat(Routes.rangeFrom(state, 1, 1).orElseThrow().target(2).reach()).isEqualTo(RangeView.Reach.DIRECT);
    }

    @Test void onlyRoutesOverStationsCountStations() {
        assertThatThrownBy(() -> new RangeView.Target(RangeView.Reach.STATIONS, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RangeView.Target(RangeView.Reach.DIRECT, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RangeView.Target(RangeView.Reach.NONE, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void foreignSourcesAndUnlimitedRulesHaveNoRange() {
        GameState state = NavigationStates.running();
        assertThat(Routes.rangeFrom(state, 1, 4)).isEmpty();
        GameState classic = new GameState();
        classic.players().add(new Player(1, "Human", false, "#111111"));
        classic.systems().add(new StarSystem(1, "Home", 0, 0, 1, 5, 4, false));
        classic.systems().add(new StarSystem(2, "Far", 5000, 0, null, 5, 4, true));
        assertThat(Routes.rangeFrom(classic, 1, 1)).isEmpty();
    }
}
