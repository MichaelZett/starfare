package de.zettsystems.starfare.economy.values;

import de.zettsystems.starfare.game.values.SystemComposition;

/** Persisted physical inputs; suitability thresholds are deliberately simplified game rules. */
public record SystemQuality(SystemComposition.StarType star, double luminosity,
                            double orbitAu, SystemComposition.BodyType body, boolean moon,
                            Atmosphere atmosphere, int metalRichness) {
    public enum Atmosphere { NONE, THIN, TEMPERATE, DENSE }
    public enum Suitability {
        HARSH(25, 1), HABITABLE(35, 2), FERTILE(50, 3);
        private final int laborLimit;
        private final int growthPercent;
        Suitability(int laborLimit, int growthPercent) {
            this.laborLimit = laborLimit; this.growthPercent = growthPercent;
        }
        public int laborLimit() { return laborLimit; }
        public int growthPercent() { return growthPercent; }
    }

    public SystemQuality {
        java.util.Objects.requireNonNull(star, "star");
        java.util.Objects.requireNonNull(body, "body");
        java.util.Objects.requireNonNull(atmosphere, "atmosphere");
        if (!Double.isFinite(luminosity) || luminosity <= 0 || !Double.isFinite(orbitAu)
                || orbitAu <= 0 || metalRichness < 1 || metalRichness > 5
                || (!moon && body != SystemComposition.BodyType.ROCKY
                    && body != SystemComposition.BodyType.SUPER_EARTH)) {
            throw new IllegalArgumentException("Invalid colony environment");
        }
    }
    /** Radiation relative to Earth, rather than a decorative orbit index or star color. */
    public double stellarFlux() { return luminosity / (orbitAu * orbitAu); }
    public Suitability suitability() {
        double flux = stellarFlux();
        if (atmosphere == Atmosphere.TEMPERATE && flux >= 0.75 && flux <= 1.5) {
            return Suitability.FERTILE;
        }
        if (atmosphere != Atmosphere.NONE && atmosphere != Atmosphere.DENSE && flux >= 0.4 && flux <= 2.0) {
            return Suitability.HABITABLE;
        }
        return Suitability.HARSH;
    }
}
