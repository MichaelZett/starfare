package de.zettsystems.starfare.economy.domain;

import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.economy.values.SystemQuality;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class ColonyGenesisTest {
    @Test void generationIsRepeatableIndependentOfIterationOrderAndFairForHomes() {
        List<StarSystem> systems = new ArrayList<>();
        for (int id = 1; id <= 120; id++) { systems.add(new StarSystem(id, "S" + id, id, 0, id <= 8 ? id : null, 5, 4, id > 8)); }
        var first = ColonyGenesis.forGalaxy(systems, 42);
        Collections.reverse(systems);
        assertThat(ColonyGenesis.forGalaxy(systems, 42)).isEqualTo(first);
        assertThat(ColonyGenesis.forGalaxy(systems, 43)).isNotEqualTo(first);
        for (int id = 1; id <= 8; id++) {
            var home = first.get(id);
            assertThat(home.quality().suitability()).isEqualTo(SystemQuality.Suitability.FERTILE);
            assertThat(home.quality().metalRichness()).isEqualTo(3);
            assertThat(home.colony().population()).isEqualTo(400);
        }
        Set<SystemQuality.Suitability> suitability = new HashSet<>();
        Set<Integer> richness = new HashSet<>();
        first.values().forEach(colony -> { suitability.add(colony.quality().suitability()); richness.add(colony.quality().metalRichness()); });
        assertThat(suitability).containsExactlyInAnyOrder(SystemQuality.Suitability.values());
        assertThat(richness).containsExactlyInAnyOrder(1, 2, 3, 4, 5);
        assertThat(first.values()).anyMatch(colony -> colony.quality().moon());
    }
    @Test void homesAreMoonlessRockyWorldsAndAboutAQuarterOfOtherWorldsAreMoonsOnAGridOfOrbits() {
        List<StarSystem> systems = new ArrayList<>();
        for (int id = 1; id <= 400; id++) { systems.add(new StarSystem(id, "S" + id, id, 0, id <= 40 ? id : null, 5, 4, id > 40)); }
        var colonies = ColonyGenesis.forGalaxy(systems, 7);
        for (int id = 1; id <= 40; id++) {
            assertThat(colonies.get(id).quality().moon()).isFalse();
            assertThat(colonies.get(id).quality().body()).isEqualTo(SystemQuality.BodyType.ROCKY);
        }
        long moons = colonies.entrySet().stream().filter(entry -> entry.getKey() > 40 && entry.getValue().quality().moon()).count();
        assertThat(moons).isBetween(60L, 120L);
        colonies.values().forEach(colony -> {
            double tenths = colony.quality().orbitAu() / Math.sqrt(colony.quality().luminosity()) * 10;
            assertThat(tenths).isBetween(5.0 - 1e-9, 20.0 + 1e-9);
            assertThat(tenths).isCloseTo(Math.rint(tenths), within(1e-9));
        });
    }
}
