package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.game.values.EmpireStats;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.PlannedOrder;
import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.game.values.VisibleSystem;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JointArrivalTest {
    private static final int TARGET = 9;

    private static VisibleSystem system(int id, @Nullable Integer owner, @Nullable Integer available) {
        return new VisibleSystem(id, "S" + id, 0, 0, owner, 10, 2, true, "#abc", null, false, 0, 0, available, List.of());
    }

    private static PlayerViewState view(List<Fleet> fleets, List<PlannedOrder> orders, Set<Integer> waiting) {
        var systems = List.of(system(1, 1, 5), system(2, 1, 3), system(3, 1, 0), system(4, 1, 8),
                system(TARGET, 2, null));
        return new PlayerViewState(11, List.of(), systems, fleets, null, false, null, orders, List.of(),
                EmpireStats.NONE, waiting);
    }

    @Test
    void aSelectedFleetSetsTheRoundToMeetAndAWaitDelaysIt() {
        var fleet = new Fleet(10, 1, 1, 1, TARGET, 12, 9, 14);
        assertThat(JointArrival.forFleet(view(List.of(fleet), List.of(), Set.of()), 10))
                .isEqualTo(new JointArrival.Aim(TARGET, 14));
        assertThat(JointArrival.forFleet(view(List.of(fleet), List.of(), Set.of(10)), 10))
                .isEqualTo(new JointArrival.Aim(TARGET, 15));
        assertThat(JointArrival.forFleet(view(List.of(fleet), List.of(), Set.of()), 99)).isNull();
    }

    @Test
    void aSelectedSystemMeetsTheEarliestOwnArrivalFlyingOrQueued() {
        var late = new Fleet(10, 1, 1, 1, TARGET, 12, 9, 16);
        var queued = new PlannedOrder(0, "map.orderType.send", 2, TARGET, "S2", "S9", 4, false, null, 14);
        assertThat(JointArrival.forSystem(view(List.of(late), List.of(queued), Set.of()), TARGET))
                .isEqualTo(new JointArrival.Aim(TARGET, 14));
        assertThat(JointArrival.forSystem(view(List.of(), List.of(), Set.of()), TARGET))
                .isEqualTo(new JointArrival.Aim(TARGET, null));
    }

    @Test
    void badgesOnlyForSystemsWithShipsThatReachTheTarget() {
        var view = view(List.of(), List.of(), Set.of());
        var joint = JointArrival.of(view, new JointArrival.Aim(TARGET, 14),
                Map.of(1, 14, 2, 13, 3, 14, 4, 16, TARGET, 11), true);

        assertThat(joint).isNotNull();
        assertThat(joint.arrivals()).as("system 3 has no ships to spare, the target is no source")
                .containsOnlyKeys(1, 2, 4);
        assertThat(joint.fit(14)).isEqualTo(JointArrival.Fit.MATCH);
        assertThat(joint.fit(13)).isEqualTo(JointArrival.Fit.EARLY);
        assertThat(joint.gap(13)).isEqualTo(1);
        assertThat(joint.fit(16)).isEqualTo(JointArrival.Fit.LATE);
        assertThat(joint.gap(16)).isEqualTo(2);
        assertThat(joint.matchingSources()).containsExactly(1);
        assertThat(joint.arrivalFrom(3)).isNull();
        assertThat(JointArrival.of(view, new JointArrival.Aim(TARGET, 14), Map.of(3, 14), true))
                .as("nothing to send, nothing to show").isNull();
    }

    @Test
    void withoutARoundToMeetTheBadgesShowPlainRoundsAndPreselectNothing() {
        var joint = JointArrival.of(view(List.of(), List.of(), Set.of()), new JointArrival.Aim(TARGET, null),
                Map.of(1, 14, 2, 13), false);

        assertThat(joint).isNotNull();
        assertThat(joint.fit(14)).isEqualTo(JointArrival.Fit.OPEN);
        assertThat(joint.gap(14)).isZero();
        assertThat(joint.matchingSources()).isEmpty();
        assertThat(joint.mayDispatch()).isFalse();
    }
}
