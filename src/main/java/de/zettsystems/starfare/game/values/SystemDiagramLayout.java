package de.zettsystems.starfare.game.values;

/** Shared presentation scales; dimensions are type classes, never physical radii. */
public final class SystemDiagramLayout {
    private SystemDiagramLayout() { }

    public static double orbitPosition(SystemComposition composition, SystemComposition.Body body) {
        var quality = composition.colonyQuality();
        return quality == null ? 25 + 8.5 * body.orbit() : distancePosition(quality.orbitAu());
    }

    /** Bounded, strictly increasing compression, identical across systems and viewports. */
    public static double distancePosition(double orbitAu) {
        if (!Double.isFinite(orbitAu) || orbitAu <= 0) {
            throw new IllegalArgumentException("Orbit distance must be finite and positive");
        }
        return 25 + 62 * (1 - 1.5 / (orbitAu + 1.5));
    }

    public static double bodyHeight(SystemComposition composition, SystemComposition.Body body) {
        return composition.colonyQuality() == null ? (body.orbit() % 2 == 0 ? 36 : 72) : 48;
    }

    public static int starDiameter(SystemComposition.StarType type) {
        return switch (type) {
            case RED -> 72;
            case ORANGE -> 84;
            case YELLOW -> 96;
            case YELLOW_WHITE -> 108;
            case WHITE -> 120;
        };
    }

    public static int bodyDiameter(SystemComposition.BodyType type) {
        return switch (type) {
            case DWARF -> 12;
            case ROCKY -> 18;
            case SUPER_EARTH -> 25;
            case ICE_GIANT -> 30;
            case GAS_GIANT -> 36;
            case ASTEROIDS -> 20;
        };
    }
}
