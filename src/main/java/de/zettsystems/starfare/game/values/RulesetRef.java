package de.zettsystems.starfare.game.values;

import java.util.Objects;

/** Stable game identity, independent from display names and application releases. */
public record RulesetRef(String variant, String version) {
    /** Classic rules ("Klassisch"), technically still SectorForces 1.0.0. */
    public static final RulesetRef SECTOR_FORCES = new RulesetRef("classic", "1.0.0");
    /**
     * Extended rules ("Erweitert"): industry expansion, limited range with stations, alliances and
     * coalition combat. The technical variant id stays {@code spaceward}.
     */
    public static final RulesetRef EXTENDED = new RulesetRef("spaceward", "2.0.0");

    public RulesetRef {
        Objects.requireNonNull(variant, "variant");
        Objects.requireNonNull(version, "version");
        if (variant.isBlank() || version.isBlank()) {
            throw new IllegalArgumentException("Ruleset identity must be complete");
        }
    }

    /** Random events, positional start balance and the round limit belong to the classic rules only. */
    public boolean usesSectorOptions() { return SECTOR_FORCES.equals(this); }

    /** Whether the game uses the extended rules (any stored version of the variant). */
    public boolean isSpaceward() { return variant.equals(EXTENDED.variant()); }
}
