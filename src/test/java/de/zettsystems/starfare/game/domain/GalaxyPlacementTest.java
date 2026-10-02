package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.navigation.domain.RangeCalibration;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import static org.assertj.core.api.Assertions.*;

class GalaxyPlacementTest {
    @Test void calibratedRangeConnectsBothLayoutsAcrossAllSupportedGalaxySizes() throws IOException {
        StringBuilder measurements = new StringBuilder("layout,systems,seeds,min,mean,max\n");
        for (GalaxyLayout layout : GalaxyLayout.values()) {
            for (int count : List.of(8, 24, 60, 120)) {
                DoubleSummaryStatistics stats = new DoubleSummaryStatistics();
                for (int seed = 0; seed < 20; seed++) {
                    GameSetup base = GameSetup.defaults();
                    GameSetup setup = new GameSetup(count, 2, 0, List.of(10, 10), 1, 20, 10,
                            base.observersAllowed(), base.reentryAllowed(), base.seatColorHexes(), base.productionDistribution(), layout);
                    List<double[]> points = GalaxyPlacement.positionsFor(setup, new Random(seed));
                    assertThat(points).hasSize(count);
                    List<StarSystem> systems = new ArrayList<>();
                    for (int i = 0; i < points.size(); i++) {
                        double[] point = points.get(i);
                        assertThat(point[0]).isBetween(0.0, (double) GameConfig.MAX_X);
                        assertThat(point[1]).isBetween(0.0, (double) GameConfig.MAX_Y);
                        systems.add(new StarSystem(i, "S" + i, point[0], point[1], null, 1, 1, true));
                    }
                    double range = RangeCalibration.forGalaxy(systems).range(); stats.accept(range);
                    assertThat(reachable(systems, range)).as("%s / %s / seed %s", layout, count, seed).hasSize(count);
                }
                measurements.append(String.format(Locale.ROOT, "%s,%d,%d,%.0f,%.1f,%.0f%n", layout, count, stats.getCount(), stats.getMin(), stats.getAverage(), stats.getMax()));
            }
        }
        Path target = Path.of("build", "navigation", "calibration.csv"); Files.createDirectories(target.getParent());
        Files.writeString(target, measurements);
    }
    @Test void calibrationIsTheSmallestWholeRangeAndHandlesSmallGalaxies() {
        var systems = List.of(new StarSystem(1, "A", 0, 0, null, 1, 1, true),
                new StarSystem(2, "B", 3, 4, null, 1, 1, true), new StarSystem(3, "C", 9, 4, null, 1, 1, true));
        assertThat(RangeCalibration.forGalaxy(systems).range()).isEqualTo(6);
        assertThat(reachable(systems, 5)).hasSize(2);
        assertThat(RangeCalibration.forGalaxy(List.of()).range()).isEqualTo(1);
        assertThat(RangeCalibration.forGalaxy(systems.subList(0, 1)).range()).isEqualTo(1);
    }
    private static Set<Integer> reachable(List<StarSystem> systems, double range) {
        Set<Integer> visited = new HashSet<>(); ArrayDeque<StarSystem> queue = new ArrayDeque<>();
        queue.add(systems.getFirst()); visited.add(systems.getFirst().id());
        while (!queue.isEmpty()) {
            StarSystem source = queue.remove();
            for (StarSystem target : systems) {
                if (!visited.contains(target.id()) && Math.hypot(source.x() - target.x(), source.y() - target.y()) <= range) {
                    visited.add(target.id()); queue.add(target);
                }
            }
        }
        return visited;
    }
}
