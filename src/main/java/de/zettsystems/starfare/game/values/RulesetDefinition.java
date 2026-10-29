package de.zettsystems.starfare.game.values;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Catalog entry; supported versions and creation availability are separate concerns. */
public record RulesetDefinition(RulesetRef defaultRef, String nameKey, String descriptionKey,
                                Set<String> supportedVersions, boolean creationEnabled, Set<String> creatableVersions) {
    public static final String SECTOR_FORCES_NAME = "ruleset.sectorForces.name";
    public static final String SECTOR_FORCES_DESCRIPTION = "ruleset.sectorForces.description";
    public static final String SPACEWARD_NAME = "ruleset.spaceward.name";
    public static final String SPACEWARD_DESCRIPTION = "ruleset.spaceward.description";

    public RulesetDefinition(RulesetRef defaultRef, String nameKey, String descriptionKey,
                             Set<String> supportedVersions, boolean creationEnabled) {
        this(defaultRef, nameKey, descriptionKey, supportedVersions, creationEnabled,
                creationEnabled ? Set.of(defaultRef.version()) : Set.of());
    }

    public RulesetDefinition {
        Objects.requireNonNull(defaultRef, "defaultRef");
        Objects.requireNonNull(nameKey, "nameKey");
        Objects.requireNonNull(descriptionKey, "descriptionKey");
        supportedVersions = Set.copyOf(supportedVersions);
        creatableVersions = Set.copyOf(creatableVersions);
        if (!supportedVersions.containsAll(creatableVersions)
                || (creationEnabled && !creatableVersions.contains(defaultRef.version()))) {
            throw new IllegalArgumentException("Creation requires a supported default version");
        }
    }

    /** Default first, then stable version order; stored-only versions never enter the wizard. */
    public List<RulesetRef> creationChoices() {
        if (!creationEnabled) { return List.of(); }
        return creatableVersions.stream().map(version -> new RulesetRef(defaultRef.variant(), version))
                .sorted(Comparator.comparing((RulesetRef ref) -> !ref.equals(defaultRef))
                        .thenComparing(RulesetRef::version)).toList();
    }

    public boolean supports(RulesetRef ref) {
        return defaultRef.variant().equals(ref.variant()) && supportedVersions.contains(ref.version());
    }
}
