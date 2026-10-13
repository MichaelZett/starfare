package de.zettsystems.starfare.game.values;

import java.util.Objects;

/** Stable game identity, independent from display names and application releases. */
public record RulesetRef(String variant, String version) {
    private static final String SPACEWARD_VARIANT = "spaceward";
    public static final RulesetRef SECTOR_FORCES = new RulesetRef("classic", "1.0.0");
    public static final RulesetRef SPACEWARD = new RulesetRef(SPACEWARD_VARIANT, "1.0.0");

    public static final RulesetRef SPACEWARD_ALLIANCE = new RulesetRef(SPACEWARD_VARIANT, "1.1.0");
    public static final RulesetRef SPACEWARD_COLONIES = new RulesetRef(SPACEWARD_VARIANT, "1.2.0-w1");

    public static final RulesetRef SPACEWARD_METALS = new RulesetRef(SPACEWARD_VARIANT, "1.2.0-w2");
    public static final RulesetRef SPACEWARD_RECYCLING = new RulesetRef(SPACEWARD_VARIANT, "1.2.0-w3");
    public static final RulesetRef SPACEWARD_ECONOMY_PREVIEW = new RulesetRef(SPACEWARD_VARIANT, "1.2.0-w4");
    /** W4 economy with population as atmosphere only: installed industry is fully usable. */
    public static final RulesetRef SPACEWARD_DIRECT_ECONOMY = new RulesetRef(SPACEWARD_VARIANT, "1.2.0-w5");
    public boolean usesRecycling() {
        return SPACEWARD_RECYCLING.equals(this) || SPACEWARD_ECONOMY_PREVIEW.equals(this) || SPACEWARD_DIRECT_ECONOMY.equals(this);
    }
    /** W1–W4 cap usable industry by colony labor; W5 keeps population as atmosphere only. */
    public boolean limitsIndustryByLabor() { return usesColonies() && !SPACEWARD_DIRECT_ECONOMY.equals(this); }
    /** W5 shortens a system's outgoing relocations when its planned shipbuilding no longer covers them. */
    public boolean adjustsRelocationsToShipbuilding() { return SPACEWARD_DIRECT_ECONOMY.equals(this); }
    public boolean usesMetals() { return SPACEWARD_METALS.equals(this) || usesRecycling(); }
    public boolean usesColonies() { return SPACEWARD_COLONIES.equals(this) || usesMetals(); }
    public boolean usesConfigurableVictory() { return SPACEWARD_ALLIANCE.equals(this) || usesColonies(); }

    public boolean isSpaceward() { return variant.equals(SPACEWARD.variant()); }

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
