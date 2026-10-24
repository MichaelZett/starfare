package de.zettsystems.starfare.game.values;

/** Frozen fictional radius model v1; presentation only, independent of generation and simulation. */
public final class PhysicalSystemSize {
    private PhysicalSystemSize() { }

    public static int starRadius(GameId game, int system, SystemComposition.StarType type, int index) {
        int[] range = switch (type) {
            case RED -> new int[]{100_000, 400_000};
            case ORANGE -> new int[]{400_000, 600_000};
            case YELLOW -> new int[]{600_000, 850_000};
            case YELLOW_WHITE -> new int[]{850_000, 1_200_000};
            case WHITE -> new int[]{1_200_000, 1_800_000};
        };
        return radius(game, system, "star/" + index, range[0], range[1]);
    }

    public static int bodyRadius(GameId game, int system, SystemComposition.Body body) {
        int[] range = switch (body.type()) {
            case ROCKY -> new int[]{2_500, 8_000};
            case SUPER_EARTH -> new int[]{8_000, 15_000};
            case ICE_GIANT -> new int[]{18_000, 35_000};
            case GAS_GIANT -> new int[]{35_000, 80_000};
            case DWARF -> new int[]{200, 2_000};
            case ASTEROIDS -> throw new IllegalArgumentException("An asteroid belt has no individual radius");
        };
        return radius(game, system, "body/" + body.orbit(), range[0], range[1]);
    }

    public static int moonRadius(GameId game, int system, int orbit, int index) {
        return radius(game, system, "moon/" + orbit + "/" + index, 300, 3_500);
    }

    /** One compressed monotone scale for all planetary bodies, including moons. */
    public static int bodyDiameter(int radiusKm) {
        if (radiusKm <= 0 || radiusKm > 80_000) { throw new IllegalArgumentException("Invalid model radius"); }
        return (int) Math.round(8 + 32 * Math.log1p(radiusKm / 200.0) / Math.log1p(400));
    }

    /** Stars use their own common scale, separate from planetary bodies. */
    public static int starDiameter(int radiusKm) {
        if (radiusKm < 100_000 || radiusKm > 1_800_000) { throw new IllegalArgumentException("Invalid model star radius"); }
        return (int) Math.round(66 + 54 * Math.log(radiusKm / 100_000.0) / Math.log(18));
    }

    private static int radius(GameId game, int system, String body, int minimum, int maximum) {
        long hash = 0xcbf29ce484222325L;
        String key = "physical-size-v1/" + game.value() + "/" + system + "/" + body;
        for (int index = 0; index < key.length(); index++) { hash = (hash ^ key.charAt(index)) * 0x100000001b3L; }
        // Round to 100 km: these are model values, not measurements.
        return minimum + (int) Long.remainderUnsigned(hash, (maximum - minimum) / 100L + 1) * 100;
    }
}
