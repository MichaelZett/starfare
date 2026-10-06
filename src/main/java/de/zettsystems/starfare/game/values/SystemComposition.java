package de.zettsystems.starfare.game.values;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/** Stable, decorative system illustration. Never used by the game simulation. */
public record SystemComposition(List<StarType> stars, List<Body> bodies) {
    public enum StarType { RED, ORANGE, YELLOW, YELLOW_WHITE, WHITE }
    public enum BodyType { ROCKY, SUPER_EARTH, GAS_GIANT, ICE_GIANT, DWARF, ASTEROIDS }
    public record Body(int orbit, BodyType type, int majorMoons, boolean rings, int appearance) { }

    public SystemComposition {
        stars = List.copyOf(stars);
        bodies = List.copyOf(bodies);
    }

    /** Fog-of-war boundary: unknown/estimated systems receive no decorative detail. */
    public static Optional<SystemComposition> forVisible(GameId gameId, VisibleSystem system) {
        if (!system.fullyVisible()) {
            return Optional.empty();
        }
        var industry = system.industry();
        var colony = industry == null ? null : industry.colony();
        if (colony != null) {
            var quality = colony.quality();
            return Optional.of(new SystemComposition(List.of(quality.star()),
                    List.of(new Body(1, quality.body(), quality.moon() ? 1 : 0, false, 0))));
        }
        return Optional.of(illustrate(gameId, system.id()));
    }

    private static SystemComposition illustrate(GameId gameId, int systemId) {
        // FNV-1a over stable IDs, with a frozen presentation version. No state, time or locale.
        long seed = 0xcbf29ce484222325L;
        String identity = "system-illustration-v1/" + gameId.value() + "/" + systemId;
        for (int index = 0; index < identity.length(); index++) {
            seed = (seed ^ identity.charAt(index)) * 0x100000001b3L;
        }
        Random random = new Random(seed);
        StarType primary = primaryStar(random);
        List<StarType> stars = new ArrayList<>(List.of(primary));
        // Approximate mass-dependent multiplicity, simplified to at most two stars.
        int companions = switch (primary) {
            case RED -> 26;
            case ORANGE, YELLOW -> 44;
            case YELLOW_WHITE, WHITE -> 50;
        };
        if (random.nextInt(100) < companions) {
            stars.add(companionStar(primary, random));
        }
        int planets = 2 + random.nextInt(4);
        List<Body> bodies = new ArrayList<>();
        for (int index = 0; index < planets; index++) {
            BodyType type = planetType(random, index, planets);
            boolean giant = type == BodyType.GAS_GIANT || type == BodyType.ICE_GIANT;
            int moons = giant ? 1 + random.nextInt(3) : random.nextInt(3);
            bodies.add(new Body(bodies.size() + 1, type, moons,
                    giant && random.nextBoolean(), random.nextInt(4)));
            if (index == 0 && random.nextInt(100) < 55) {
                bodies.add(new Body(bodies.size() + 1, BodyType.ASTEROIDS, 0, false, 0));
            }
        }
        if (random.nextBoolean()) {
            bodies.add(new Body(bodies.size() + 1, BodyType.DWARF, random.nextInt(2), false,
                    random.nextInt(4)));
        }
        return new SystemComposition(stars, bodies);
    }

    private static StarType companionStar(StarType primary, Random random) {
        List<StarType> candidates = switch (primary) {
            case RED -> List.of(StarType.RED);
            case ORANGE -> List.of(StarType.RED, StarType.ORANGE);
            case YELLOW -> List.of(StarType.RED, StarType.ORANGE, StarType.YELLOW);
            case YELLOW_WHITE -> List.of(StarType.RED, StarType.ORANGE, StarType.YELLOW, StarType.YELLOW_WHITE);
            case WHITE -> List.of(StarType.RED, StarType.ORANGE, StarType.YELLOW, StarType.YELLOW_WHITE, StarType.WHITE);
        };
        return candidates.get(random.nextInt(candidates.size()));
    }

    private static StarType primaryStar(Random random) {
        int pick = random.nextInt(100);
        // Rounded illustrative mix; not a survey of all Galactic stars or planetary systems.
        if (pick < 73) { return StarType.RED; }
        if (pick < 86) { return StarType.ORANGE; }
        if (pick < 92) { return StarType.YELLOW; }
        return pick < 97 ? StarType.YELLOW_WHITE : StarType.WHITE;
    }

    private static BodyType planetType(Random random, int index, int planets) {
        // Usually rocky inside, giants outside; exceptions include hot Jupiters.
        int pick = random.nextInt(100);
        if (index == 0) {
            return pick < 12 ? BodyType.GAS_GIANT : BodyType.ROCKY;
        }
        if (index < planets / 2) {
            return pick < 50 ? BodyType.ROCKY : BodyType.SUPER_EARTH;
        }
        if (pick < 25) { return BodyType.ROCKY; }
        if (pick < 45) { return BodyType.SUPER_EARTH; }
        return pick < 75 ? BodyType.GAS_GIANT : BodyType.ICE_GIANT;
    }
}
