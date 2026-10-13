package de.zettsystems.starfare.navigation.domain;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.navigation.values.RangeShare;
import de.zettsystems.starfare.navigation.values.RangeView;
import de.zettsystems.starfare.testsupport.NavigationStates;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class RangeViewTest {
    @Test void targetsAreDirectViaStationsOrUnreachableUsingTheCommandRules() {
        GameState state = NavigationStates.running();
        RangeView view = Routes.rangeFrom(state, 1, 1).orElseThrow();
        assertThat(view.range()).isEqualTo(100);
        assertThat(view.target(2).reach()).isEqualTo(RangeView.Reach.DIRECT);
        assertThat(view.target(2).percent()).isEqualTo(100.0);
        assertThat(view.target(4).reach()).isEqualTo(RangeView.Reach.STATIONS);
        assertThat(view.target(4).percent()).isEqualTo(300.0);
        assertThat(view.target(8).reach()).isEqualTo(RangeView.Reach.NONE);
        assertThat(view.targets()).doesNotContainKey(1);
        for (int target : view.targets().keySet()) {
            boolean planned = Routes.plan(state, 1, 1, target).isPresent();
            assertThat(view.target(target).reach() != RangeView.Reach.NONE).isEqualTo(planned);
            assertThat(view.target(target).reach() == RangeView.Reach.DIRECT).isEqualTo(Routes.withinRange(state, 1, target));
        }
    }

    @Test void justOutsideTheBoundaryNeverReadsAsExactlyOneHundredPercent() {
        GameState state = NavigationStates.running();
        state.updateSystem(2, system -> system.relocateTo(200.001, 100));
        var target = Routes.rangeFrom(state, 1, 1).orElseThrow().target(2);
        assertThat(target.reach()).isEqualTo(RangeView.Reach.NONE);
        assertThat(target.percent()).isEqualTo(100.1);
        state.updateSystem(2, system -> system.relocateTo(170.71, 170.71));
        var diagonal = Routes.rangeFrom(state, 1, 1).orElseThrow().target(2);
        assertThat(diagonal.reach()).isEqualTo(RangeView.Reach.DIRECT);
        assertThat(diagonal.percent()).isLessThanOrEqualTo(100.0).isGreaterThanOrEqualTo(99.9);
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

    @Test void displayPercentRoundsTowardsTheBoundaryCorrectly() {
        assertThat(RangeShare.percent(0.904)).isEqualTo(90.0);
        assertThat(RangeShare.percent(0.989)).isEqualTo(99.0);
        assertThat(RangeShare.percent(0.9899)).isEqualTo(99.0);
        assertThat(RangeShare.percent(0.995)).isEqualTo(99.5);
        assertThat(RangeShare.percent(1.0)).isEqualTo(100.0);
        assertThat(RangeShare.percent(1.0000001)).isEqualTo(100.1);
        assertThat(RangeShare.percent(1.0049)).isEqualTo(100.5);
        assertThat(RangeShare.percent(1.5)).isEqualTo(150.0);
        assertThat(RangeShare.percent(1.0123)).isEqualTo(101.0);
        assertThatThrownBy(() -> RangeShare.percent(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RangeShare.percent(Double.NaN)).isInstanceOf(IllegalArgumentException.class);
    }
}
