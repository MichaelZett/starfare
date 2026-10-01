package de.zettsystems.starfare.game.values;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class ArrivalPreviewTest {
    @Test
    void mergesSendsWithFleetsAndCountsWaitOnlyOnce() {
        var view = new PlayerViewState(1, List.of(), List.of(),
                List.of(new Fleet(10, 1, 1, 1, 3, 10, 1, 3), new Fleet(11, 1, 2, 2, 3, 7, 1, 4)),
                null, false, null,
                List.of(new PlannedOrder(0, "map.orderType.send", 1, 3, "A", "C", 4, false, null, 4),
                        new PlannedOrder(1, "map.orderType.wait", 1, 3, "A", "C", 10, false, null, 4),
                        new PlannedOrder(2, "map.orderType.send", 1, 2, "A", "B", 2, false, null, 3)),
                List.of(), EmpireStats.NONE, Set.of(10));
        assertThat(ArrivalPreview.from(view)).containsExactly(new ArrivalPreview(2, "?", 3, 2),
                new ArrivalPreview(3, "?", 4, 21));
    }
}
