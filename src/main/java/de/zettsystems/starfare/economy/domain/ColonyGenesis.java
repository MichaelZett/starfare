package de.zettsystems.starfare.economy.domain;

import de.zettsystems.starfare.economy.values.SystemQuality;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.game.values.SystemComposition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Frozen W1 generation; home colonies share economics despite different stellar appearances. */
public final class ColonyGenesis {
    private ColonyGenesis() { }
    public static Map<Integer, ColonyEconomy> forGalaxy(List<StarSystem> systems, long seed) {
        Map<Integer, ColonyEconomy> result = new HashMap<>();
        for (StarSystem system : systems) {
            Random random = new Random(seed ^ (0x9e3779b97f4a7c15L * system.id()));
            boolean home = system.ownerId() != null && !system.neutral();
            SystemQuality quality = environment(random, home);
            int population = system.productionPerTurn() * Colony.POPULATION_PER_LABOR;
            result.put(system.id(), new ColonyEconomy(quality, new Colony(population)));
        }
        return Map.copyOf(result);
    }
    private static SystemQuality environment(Random random, boolean home) {
        var stars = List.of(SystemComposition.StarType.RED, SystemComposition.StarType.ORANGE,
                SystemComposition.StarType.YELLOW, SystemComposition.StarType.YELLOW_WHITE, SystemComposition.StarType.WHITE);
        var star = stars.get(random.nextInt(stars.size()));
        double luminosity = switch (star) {
            case RED -> 0.04; case ORANGE -> 0.4; case YELLOW -> 1.0;
            case YELLOW_WHITE -> 3.0; case WHITE -> 10.0;
        };
        int richness = home ? 3 : 1 + random.nextInt(5);
        double relativeOrbit = home ? 1.0 : 0.5 + random.nextInt(16) / 10.0;
        var atmospheres = List.of(SystemQuality.Atmosphere.NONE, SystemQuality.Atmosphere.THIN,
                SystemQuality.Atmosphere.TEMPERATE, SystemQuality.Atmosphere.DENSE);
        var atmosphere = home ? SystemQuality.Atmosphere.TEMPERATE : atmospheres.get(random.nextInt(atmospheres.size()));
        boolean moon = !home && random.nextInt(4) == 0;
        var body = moon ? SystemComposition.BodyType.GAS_GIANT : SystemComposition.BodyType.ROCKY;
        return new SystemQuality(star, luminosity, Math.sqrt(luminosity) * relativeOrbit,
                body, moon, atmosphere, richness);
    }
}
