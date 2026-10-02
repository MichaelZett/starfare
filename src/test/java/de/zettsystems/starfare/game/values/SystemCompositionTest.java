package de.zettsystems.starfare.game.values;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class SystemCompositionTest {
    private static final GameId GAME = GameId.of("composition-fixture");

    private static VisibleSystem visible(int id, boolean full, int ships) {
        return new VisibleSystem(id, "Test", 0, 0, 1, ships, 4, full, "#ffffff", 1,
                !full, null, List.of());
    }

    @Test
    void illustrationIsStableAndIndependentOfGameplayValues() {
        var original = SystemComposition.forVisible(GAME, visible(1, true, 8)).orElseThrow();
        assertThat(SystemComposition.forVisible(GameId.of(GAME.value()), visible(1, true, 200)))
                .contains(original);
        assertThat(SystemComposition.forVisible(GameId.of("another-game"), visible(1, true, 8)).orElseThrow())
                .isNotEqualTo(original);
        assertThat(SystemComposition.forVisible(GAME, visible(2, true, 8)).orElseThrow()).isNotEqualTo(original);
    }

    @Test
    void incompleteVisibilityNeverRevealsComposition() {
        assertThat(SystemComposition.forVisible(GAME, visible(1, false, 8))).isEmpty();
        var hidden = new VisibleSystem(1, "Hidden", 0, 0, null, null, null,
                false, null, null, false, null, List.of());
        assertThat(SystemComposition.forVisible(GAME, hidden)).isEmpty();
    }

    @Test
    void variedSystemsStayCompactAndUseContiguousOrbits() {
        Set<SystemComposition.StarType> stars = new HashSet<>();
        Set<SystemComposition.BodyType> bodies = new HashSet<>();
        Set<Integer> starCounts = new HashSet<>();
        int binaries = 0;
        for (int id = 0; id < 1000; id++) {
            var composition = SystemComposition.forVisible(GAME, visible(id, true, 8)).orElseThrow();
            stars.addAll(composition.stars());
            starCounts.add(composition.stars().size());
            if (composition.stars().size() == 2) { binaries++; }
            assertThat(composition.stars()).hasSizeBetween(1, 2);
            assertThat(composition.bodies()).hasSizeBetween(2, 7);
            long planets = composition.bodies().stream().filter(body -> body.type() != SystemComposition.BodyType.DWARF
                    && body.type() != SystemComposition.BodyType.ASTEROIDS).count();
            assertThat(planets).isBetween(2L, 5L);
            for (int index = 0; index < composition.bodies().size(); index++) {
                var body = composition.bodies().get(index);
                bodies.add(body.type());
                assertThat(body.orbit()).isEqualTo(index + 1);
                assertThat(body.majorMoons()).isBetween(0, 3);
                if (body.rings()) {
                    assertThat(body.type()).isIn(SystemComposition.BodyType.GAS_GIANT, SystemComposition.BodyType.ICE_GIANT);
                }
            }
        }
        assertThat(stars).containsExactlyInAnyOrder(SystemComposition.StarType.values());
        assertThat(bodies).containsExactlyInAnyOrder(SystemComposition.BodyType.values());
        assertThat(starCounts).containsExactlyInAnyOrder(1, 2);
        assertThat(binaries).isBetween(230, 410);
    }
}
