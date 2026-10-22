package de.zettsystems.starfare.report.values;

/** Momentary crew condition derived only from stored opening combat facts. */
public record CombatReadiness(int level, int variant) {
    public static CombatReadiness from(int ships, double strength, double baseStrength, int sideId) {
        double crewStrength = baseStrength > 0 && Double.compare(baseStrength, ships) != 0 ? strength * ships / baseStrength : strength;
        return from(ships, crewStrength, sideId);
    }
    public static CombatReadiness from(int ships, double strength, int sideId) {
        double deviation = ships > 0 && Double.isFinite(strength) ? strength / ships - 1 : 0;
        int level;
        if (deviation < -0.05 - 1e-9) { level = 0; }
        else if (deviation < -0.01 - 1e-9) { level = 1; }
        else if (deviation <= 0.01 + 1e-9) { level = 2; }
        else if (deviation <= 0.05 + 1e-9) { level = 3; }
        else { level = 4; }
        int hash = java.util.Objects.hash(ships, Double.doubleToLongBits(strength), sideId);
        return new CombatReadiness(level, Math.floorMod(hash, 3));
    }
}
