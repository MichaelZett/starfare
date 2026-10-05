package de.zettsystems.starfare.game.values;

import java.util.Objects;

/** Stable game identity, independent from display names and application releases. */
public record RulesetRef(String variant, String version) {
    public static final RulesetRef SECTOR_FORCES = new RulesetRef("classic", "1.0.0");
    public static final RulesetRef SPACEWARD = new RulesetRef("spaceward", "1.0.0");

    public static final RulesetRef SPACEWARD_ALLIANCE = new RulesetRef("spaceward", "1.1.0");

    public boolean spaceward() { return variant.equals(SPACEWARD.variant()); }

    public RulesetRef {
        Objects.requireNonNull(variant, "variant");
        Objects.requireNonNull(version, "version");
        if (variant.isBlank() || version.isBlank()) {
            throw new IllegalArgumentException("Ruleset identity must be complete");
        }
    }
}
