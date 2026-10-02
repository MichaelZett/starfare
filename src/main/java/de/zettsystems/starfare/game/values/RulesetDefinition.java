package de.zettsystems.starfare.game.values;

import java.util.Objects;
import java.util.Set;

/** Catalog entry; supported versions and creation availability are separate concerns. */
public record RulesetDefinition(RulesetRef defaultRef, String nameKey, String descriptionKey,
                                Set<String> supportedVersions, boolean creationEnabled) {
    public static final String SECTOR_FORCES_NAME = "ruleset.sectorForces.name";
    public static final String SECTOR_FORCES_DESCRIPTION = "ruleset.sectorForces.description";
    public static final String SPACEWARD_NAME = "ruleset.spaceward.name";
    public static final String SPACEWARD_DESCRIPTION = "ruleset.spaceward.description";

    public RulesetDefinition {
        Objects.requireNonNull(defaultRef, "defaultRef");
        Objects.requireNonNull(nameKey, "nameKey");
        Objects.requireNonNull(descriptionKey, "descriptionKey");
        supportedVersions = Set.copyOf(supportedVersions);
        if (creationEnabled && !supportedVersions.contains(defaultRef.version())) {
            throw new IllegalArgumentException("Creation requires a supported default version");
        }
    }

    public boolean supports(RulesetRef ref) {
        return defaultRef.variant().equals(ref.variant()) && supportedVersions.contains(ref.version());
    }
}
