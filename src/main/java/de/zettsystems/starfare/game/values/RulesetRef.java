package de.zettsystems.starfare.game.values;

import java.util.Objects;

/** Stable game identity, independent from display names and application releases. */
public record RulesetRef(String variant, String version) {
    private static final String SPACEWARD_VARIANT = "spaceward";
    public static final RulesetRef SECTOR_FORCES = new RulesetRef("classic", "1.0.0");
    public static final RulesetRef SPACEWARD = new RulesetRef(SPACEWARD_VARIANT, "1.0.0");

    public static final RulesetRef SPACEWARD_ALLIANCE = new RulesetRef(SPACEWARD_VARIANT, "1.1.0");
    public static final RulesetRef SPACEWARD_COLONIES = new RulesetRef(SPACEWARD_VARIANT, "1.2.0-w1");

    public boolean usesColonies() { return SPACEWARD_COLONIES.equals(this); }
    public boolean usesConfigurableVictory() { return SPACEWARD_ALLIANCE.equals(this) || usesColonies(); }

    public boolean spaceward() { return variant.equals(SPACEWARD.variant()); }

    public RulesetRef {
        Objects.requireNonNull(variant, "variant");
        Objects.requireNonNull(version, "version");
        if (variant.isBlank() || version.isBlank()) {
            throw new IllegalArgumentException("Ruleset identity must be complete");
        }
    }

    /** Spaceward resolves all hostile sides simultaneously; the configured attack order does not apply. */
    public boolean resolvesCoalitionBattles() {
        return SPACEWARD.equals(this) || SPACEWARD_ALLIANCE.equals(this) || usesColonies();
    }
}
