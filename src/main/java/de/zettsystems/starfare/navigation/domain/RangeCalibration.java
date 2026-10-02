package de.zettsystems.starfare.navigation.domain;

import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.navigation.values.NavigationSettings;
import java.util.List;

/** Longest edge of a minimum spanning tree: the smallest uniform range connecting this galaxy. */
public final class RangeCalibration {
    private RangeCalibration() { }
    public static NavigationSettings forGalaxy(List<StarSystem> systems) {
        if (systems.size() < 2) { return new NavigationSettings(1); }
        boolean[] visited = new boolean[systems.size()];
        double[] nearest = new double[systems.size()];
        java.util.Arrays.fill(nearest, Double.POSITIVE_INFINITY);
        nearest[0] = 0;
        double longest = 0;
        for (int step = 0; step < systems.size(); step++) {
            int next = nextUnvisited(visited, nearest);
            longest = Math.max(longest, nearest[next]); visited[next] = true;
            StarSystem from = systems.get(next);
            for (int target = 0; target < systems.size(); target++) {
                StarSystem to = systems.get(target);
                double distance = Math.hypot(from.x() - to.x(), from.y() - to.y());
                if (!visited[target]) { nearest[target] = Math.min(nearest[target], distance); }
            }
        }
        return new NavigationSettings(Math.max(1, Math.ceil(longest)));
    }
    private static int nextUnvisited(boolean[] visited, double[] nearest) {
        int best = -1;
        for (int index = 0; index < visited.length; index++) {
            if (!visited[index] && (best < 0 || nearest[index] < nearest[best])) { best = index; }
        }
        return best;
    }
}
