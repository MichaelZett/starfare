package de.zettsystems.starfare.fleet.domain;

import de.zettsystems.starfare.game.values.StandingOrder;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RelocationTrimTest {
    private static StandingOrder route(int id, int to, int ships) { return new StandingOrder(id, 1, 1, to, ships); }

    @Test void routesThatFitStayUntouched() {
        assertThat(RelocationTrim.trim(List.of(route(1, 2, 6), route(2, 3, 4)), 10)).isEmpty();
        assertThat(RelocationTrim.trim(List.of(), 0)).isEmpty();
    }

    @Test void singleRouteIsCutToTheLimit() {
        assertThat(RelocationTrim.trim(List.of(route(1, 2, 6)), 4)).containsExactlyEntriesOf(java.util.Map.of(1, 4));
    }

    @Test void severalRoutesShrinkProportionallyWithLargestRemainder() {
        assertThat(RelocationTrim.trim(List.of(route(1, 2, 6), route(2, 3, 4)), 6))
                .containsEntry(1, 4).containsEntry(2, 2).hasSize(2);
    }

    @Test void equalRemaindersPreferTheSmallerRouteId() {
        assertThat(RelocationTrim.trim(List.of(route(7, 2, 3), route(4, 3, 3)), 3))
                .containsEntry(4, 2).containsEntry(7, 1);
    }

    @Test void aCoveredSmallRouteStaysAndZeroMeansRemoval() {
        var trimmed = RelocationTrim.trim(List.of(route(1, 2, 9), route(2, 3, 1)), 9);
        assertThat(trimmed).containsEntry(1, 8).doesNotContainKey(2);
        assertThat(RelocationTrim.trim(List.of(route(1, 2, 5), route(2, 3, 1)), 0))
                .containsEntry(1, 0).containsEntry(2, 0);
        assertThat(RelocationTrim.trim(List.of(route(1, 2, 5)), -3)).containsEntry(1, 0);
    }
}
