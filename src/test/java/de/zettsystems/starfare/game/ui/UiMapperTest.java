package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.game.values.VisibleSystem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UiMapperTest {

    @Test
    void systemLabelUsesVisibility() {
        VisibleSystem visible = new VisibleSystem(1, "S1", 0, 0, 1, 4, 2, true, "#fff", 1);
        VisibleSystem fog = new VisibleSystem(2, "S2", 0, 0, null, null, null, false, null, null);

        assertThat(UiMapper.systemLabel(visible)).isEqualTo("S1 G:4 P:2");
        assertThat(UiMapper.systemLabel(fog)).isEqualTo("S2");
    }

    @Test
    void systemTooltipUsesLastSeenOrNoSight() {
        VisibleSystem visible = new VisibleSystem(1, "S1", 0, 0, 1, 4, 2, true, "#fff", 3);
        VisibleSystem lastSeen = new VisibleSystem(2, "S2", 0, 0, null, null, null, false, null, 5);
        VisibleSystem noSight = new VisibleSystem(3, "S3", 0, 0, null, null, null, false, null, null);

        assertThat(UiMapper.systemTooltip(visible, 7)).isEqualTo("S1 | live in Runde 7");
        assertThat(UiMapper.systemTooltip(lastSeen, 7)).isEqualTo("S2 | zuletzt gesehen in Runde 5");
        assertThat(UiMapper.systemTooltip(noSight, 7)).isEqualTo("S3 | keine Sicht");
    }

    @Test
    void toFleetViewsMapsSystemNames() {
        VisibleSystem s1 = new VisibleSystem(1, "Alpha", 0, 0, 1, 4, 2, true, "#fff", 1);
        VisibleSystem s2 = new VisibleSystem(2, "Beta", 0, 0, null, null, null, false, null, 1);
        Fleet fleet = new Fleet(10, 1, 3, 1, 2, 5, 1, 4);
        PlayerViewState view = new PlayerViewState(2, List.of(), List.of(s1, s2), List.of(fleet), null, false, null, List.of(), List.of());

        var fleetViews = UiMapper.toFleetViews(view);

        assertThat(fleetViews).hasSize(1);
        assertThat(fleetViews.getFirst().fromName()).isEqualTo("Alpha");
        assertThat(fleetViews.getFirst().toName()).isEqualTo("Beta");
        assertThat(fleetViews.getFirst().eta()).isEqualTo(2);
    }
}
