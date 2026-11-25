package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.game.values.GalaxyExtent;
import de.zettsystems.starfare.game.values.GalaxyLayout;
import de.zettsystems.starfare.game.values.GameSetup;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Seeded placement, shared by generation and range calibration checks. */
public final class GalaxyPlacement {
    private GalaxyPlacement() { }

    /** Positions on the map that belongs to the setup's system count. */
    public static List<double[]> positionsFor(GameSetup setup, Random r) {
        return positionsFor(setup.systemCount(), setup.galaxyLayout(), GalaxyExtent.forSystems(setup.systemCount()), r);
    }

    public static List<double[]> positionsFor(int count, GalaxyLayout layout, GalaxyExtent extent, Random r) {
        var out = new ArrayList<double[]>(count);
        double marginX = extent.marginX();
        double marginY = extent.marginY();
        if (layout == GalaxyLayout.RANDOM) {
            for (int i = 0; i < count; i++) {
                out.add(new double[]{
                        marginX + r.nextDouble(extent.width() - 2 * marginX),
                        marginY + r.nextDouble(extent.height() - 2 * marginY)});
            }
            return out;
        }
        // Ein System je Rasterzelle, innerhalb der Zelle versetzt: verhindert die
        // Ballungen und Leerraeume, die rein zufaellige Punkte zwangslaeufig bilden.
        int cols = (int) Math.ceil(Math.sqrt(count * (double) extent.width() / extent.height()));
        int rows = (int) Math.ceil((double) count / cols);
        double cellWidth = (extent.width() - 2 * marginX) / cols;
        double cellHeight = (extent.height() - 2 * marginY) / rows;
        var cells = new ArrayList<int[]>(cols * rows);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                cells.add(new int[]{col, row});
            }
        }
        Collections.shuffle(cells, r);
        for (int i = 0; i < count; i++) {
            int[] cell = cells.get(i);
            out.add(new double[]{
                    marginX + cell[0] * cellWidth + (0.25 + r.nextDouble() * 0.5) * cellWidth,
                    marginY + cell[1] * cellHeight + (0.25 + r.nextDouble() * 0.5) * cellHeight});
        }
        return out;
    }

}
