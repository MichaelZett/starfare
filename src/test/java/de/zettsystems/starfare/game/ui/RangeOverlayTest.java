package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.game.values.VisibleSystem;
import de.zettsystems.starfare.navigation.values.RangeView;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RangeOverlayTest {
    private static final double RANGE = 300;
    private static final int LARGEST_DRAWN_SIZE = 118;
    private static final VisibleSystem SOURCE = system(1, 0, 0, 0);

    @Test
    void keepsTheRealRangeWhenNoSystemComesClose() {
        VisibleSystem farAway = system(2, 500, 0, 0);
        RangeView range = range(Map.of(2, target(RangeView.Reach.NONE)));

        assertThat(RangeOverlay.drawnRadius(SOURCE, range, List.of(SOURCE, farAway))).isEqualTo(RANGE);
    }

    @Test
    void endsBeforeASystemThatCannotBeReached() {
        VisibleSystem justOutOfRange = system(2, 310, 0, 16);
        RangeView range = range(Map.of(2, target(RangeView.Reach.NONE)));

        double radius = RangeOverlay.drawnRadius(SOURCE, range, List.of(SOURCE, justOutOfRange));

        assertThat(radius).isLessThan(310 - LARGEST_DRAWN_SIZE / 2.0);
    }

    @Test
    void aTargetReachableOnlyOverStationsCountsAsNotDirectlyReachable() {
        VisibleSystem overStations = system(2, 0, 320, 16);
        RangeView range = range(Map.of(2, target(RangeView.Reach.STATIONS)));

        double radius = RangeOverlay.drawnRadius(SOURCE, range, List.of(SOURCE, overStations));

        assertThat(radius).isLessThan(320 - LARGEST_DRAWN_SIZE / 2.0);
    }

    @Test
    void theNearestUnreachableSystemDecides() {
        VisibleSystem near = system(2, 280, 0, 0);
        VisibleSystem nearer = system(3, 0, 260, 0);
        RangeView range = range(Map.of(
                2, target(RangeView.Reach.NONE),
                3, target(RangeView.Reach.NONE)));

        double nearestAlone = RangeOverlay.drawnRadius(SOURCE, range, List.of(SOURCE, nearer));

        assertThat(RangeOverlay.drawnRadius(SOURCE, range, List.of(SOURCE, near, nearer))).isEqualTo(nearestAlone);
    }

    @Test
    void aSystemWithHiddenProductionNeverCountsSmallerThanItCouldBeDrawn() {
        VisibleSystem smallButHiddenByBattle = system(2, 310, 0, 0);
        RangeView range = range(Map.of(2, target(RangeView.Reach.NONE)));

        double radius = RangeOverlay.drawnRadius(SOURCE, range, List.of(SOURCE, smallButHiddenByBattle));

        assertThat(radius).isLessThan(310 - SystemDisplaySize.UNKNOWN_SIZE / 2.0);
    }

    @Test
    void directlyReachableSystemsDoNotShrinkTheCircle() {
        VisibleSystem direct = system(2, 280, 0, 16);
        VisibleSystem unlistedSource = system(3, 1, 0, 0);
        RangeView range = range(Map.of(2, target(RangeView.Reach.DIRECT)));

        assertThat(RangeOverlay.drawnRadius(SOURCE, range, List.of(SOURCE, direct, unlistedSource))).isEqualTo(RANGE);
    }

    @Test
    void neverBecomesNegative() {
        VisibleSystem touching = system(2, 10, 0, 16);
        RangeView range = range(Map.of(2, target(RangeView.Reach.NONE)));

        assertThat(RangeOverlay.drawnRadius(SOURCE, range, List.of(SOURCE, touching))).isZero();
    }

    private static RangeView range(Map<Integer, RangeView.Target> targets) {
        return new RangeView(SOURCE.id(), RANGE, targets);
    }

    private static RangeView.Target target(RangeView.Reach reach) {
        return new RangeView.Target(reach, 1.1);
    }

    private static VisibleSystem system(int id, double x, double y, int production) {
        return new VisibleSystem(id, "System " + id, x, y, null, 0, production, true, "#abc", null, false, 0,
                List.of());
    }
}
