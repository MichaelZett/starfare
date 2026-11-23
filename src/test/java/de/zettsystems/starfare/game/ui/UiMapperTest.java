package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.game.values.EmpireStats;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.game.values.VisibleSystem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UiMapperTest {

    @Test
    void systemLabelUsesVisibility() {
        VisibleSystem visible = new VisibleSystem(1, "S1", 0, 0, 1, 4, 2, true, "#fff", 1, false, null, List.of());
        VisibleSystem fog = new VisibleSystem(2, "S2", 0, 0, null, null, null, false, null, null, false, null, List.of());

        assertThat(UiMapper.systemLabel(visible)).isEqualTo("S1 G:4 P:2");
        assertThat(UiMapper.systemLabel(fog)).isEqualTo("S2");
    }

    @Test
    void systemLabelShowsTheShipsLeftAfterRelocations() {
        assertThat(UiMapper.systemLabel(visible(5, 2))).isEqualTo("S1 G:4 P:5 (3 frei)");
        assertThat(UiMapper.systemLabel(visible(2, 3))).as("never below zero").isEqualTo("S1 G:4 P:2 (0 frei)");
    }

    @Test
    void systemLabelOmitsTheFreeShipsWithoutRelocationOrProduction() {
        assertThat(UiMapper.systemLabel(visible(5, 0))).isEqualTo("S1 G:4 P:5");
        assertThat(UiMapper.systemLabel(visible(5, null))).isEqualTo("S1 G:4 P:5");
        assertThat(UiMapper.systemLabel(visible(null, 2))).doesNotContain("frei");
    }

    @Test
    void systemLabelMarksEstimatesAndRemembersTheLastSeenGarrison() {
        VisibleSystem estimate = new VisibleSystem(3, "S3", 0, 0, 2, 7, null, false, null, 4, true, null, List.of());
        VisibleSystem remembered = new VisibleSystem(4, "S4", 0, 0, 2, 6, null, false, null, 4, false, null, List.of());

        assertThat(UiMapper.systemLabel(estimate)).isEqualTo("S3 ~G:7");
        assertThat(UiMapper.systemLabel(remembered)).isEqualTo("S4 G:6");
    }

    private static VisibleSystem visible(Integer production, Integer routed) {
        return new VisibleSystem(1, "S1", 0, 0, 1, 4, production, true, "#fff", 1, false, routed, List.of());
    }

    @Test
    void systemTooltipUsesLastSeenOrNoSight() {
        VisibleSystem visible = new VisibleSystem(1, "S1", 0, 0, 1, 4, 2, true, "#fff", 3, false, null, List.of());
        VisibleSystem lastSeen = new VisibleSystem(2, "S2", 0, 0, null, null, null, false, null, 5, false, null, List.of());
        VisibleSystem noSight = new VisibleSystem(3, "S3", 0, 0, null, null, null, false, null, null, false, null, List.of());

        assertThat(UiMapper.systemTooltip(visible, 7)).isEqualTo("S1 | live in Runde 7");
        assertThat(UiMapper.systemTooltip(lastSeen, 7)).isEqualTo("S2 | zuletzt gesehen in Runde 5");
        assertThat(UiMapper.systemTooltip(noSight, 7)).isEqualTo("S3 | keine Sicht");
    }

    @Test
    void toFleetViewsMapsSystemNames() {
        VisibleSystem s1 = new VisibleSystem(1, "Alpha", 0, 0, 1, 4, 2, true, "#fff", 1, false, null, List.of());
        VisibleSystem s2 = new VisibleSystem(2, "Beta", 0, 0, null, null, null, false, null, 1, false, null, List.of());
        Fleet fleet = new Fleet(10, 1, 3, 1, 2, 5, 1, 4);
        PlayerViewState view = new PlayerViewState(2, List.of(), List.of(s1, s2), List.of(fleet), null, false, null, List.of(), List.of(),
                EmpireStats.NONE, Set.of());

        var fleetViews = UiMapper.toFleetViews(view);

        assertThat(fleetViews).hasSize(1);
        assertThat(fleetViews.getFirst().fromName()).isEqualTo("Alpha");
        assertThat(fleetViews.getFirst().toName()).isEqualTo("Beta");
        assertThat(fleetViews.getFirst().eta()).isEqualTo(2);
    }

    @Test
    void pendingWaitDelaysDisplayedArrivalByOneRound() {
        Fleet fleet = new Fleet(10, 1, 3, 1, 2, 5, 1, 4);
        PlayerViewState view = new PlayerViewState(2, List.of(), List.of(), List.of(fleet), null,
                false, null, List.of(), List.of(), EmpireStats.NONE, Set.of(10));
        assertThat(UiMapper.toFleetViews(view).getFirst().eta()).isEqualTo(3);
    }
}
