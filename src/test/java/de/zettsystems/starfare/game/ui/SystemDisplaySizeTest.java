package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.game.values.VisibleSystem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SystemDisplaySizeTest {

    @Test
    void fullyVisibleSystemsGrowWithProduction() {
        VisibleSystem low = system(1, 2, true, false, 0);
        VisibleSystem high = system(2, 12, true, false, 0);

        assertThat(SystemDisplaySize.pixelsFor(high)).isGreaterThan(SystemDisplaySize.pixelsFor(low));
    }

    @Test
    void estimatedSystemsGrowWithEstimatedProduction() {
        VisibleSystem weak = system(1, 2, false, true, 4);
        VisibleSystem strong = system(2, 12, false, true, 4);

        assertThat(SystemDisplaySize.pixelsFor(strong)).isGreaterThan(SystemDisplaySize.pixelsFor(weak));
    }

    @Test
    void unknownSystemsAlwaysUseTheSameSize() {
        VisibleSystem unknown = system(1, null, false, false, 0);
        VisibleSystem lastSeen = system(2, null, false, false, 500);

        assertThat(SystemDisplaySize.pixelsFor(unknown)).isEqualTo(SystemDisplaySize.UNKNOWN_SIZE);
        assertThat(SystemDisplaySize.pixelsFor(lastSeen)).isEqualTo(SystemDisplaySize.UNKNOWN_SIZE);
    }

    private static VisibleSystem system(int id, Integer production, boolean fullyVisible, boolean approximate,
                                        Integer garrison) {
        return new VisibleSystem(id, "S" + id, 0, 0, null, garrison, production, fullyVisible,
                null, null, approximate, null, List.of());
    }
}
