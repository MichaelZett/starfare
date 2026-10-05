package de.zettsystems.starfare.game.values;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Immutable, extensible catalog used by creation, restoration and rule dispatch. */
public record RulesetCatalog(List<RulesetDefinition> definitions) {
    public RulesetCatalog {
        definitions = List.copyOf(definitions);
        Set<String> variants = new HashSet<>();
        for (RulesetDefinition definition : definitions) {
            if (!variants.add(definition.defaultRef().variant())) {
                throw new IllegalArgumentException("Duplicate game variant: " + definition.defaultRef().variant());
            }
        }
    }

    public static RulesetCatalog builtIn() {
        return new RulesetCatalog(List.of(
                new RulesetDefinition(RulesetRef.SECTOR_FORCES, RulesetDefinition.SECTOR_FORCES_NAME,
                        RulesetDefinition.SECTOR_FORCES_DESCRIPTION, Set.of(RulesetRef.SECTOR_FORCES.version()), true),
                new RulesetDefinition(RulesetRef.SPACEWARD_ALLIANCE, RulesetDefinition.SPACEWARD_NAME,
                        RulesetDefinition.SPACEWARD_DESCRIPTION, Set.of(RulesetRef.SPACEWARD.version(), RulesetRef.SPACEWARD_ALLIANCE.version()), true)));
    }

    public Optional<RulesetDefinition> find(String variant) {
        return definitions.stream().filter(entry -> entry.defaultRef().variant().equals(variant)).findFirst();
    }

    public void requireSupported(RulesetRef ref) {
        if (find(ref.variant()).filter(entry -> entry.supports(ref)).isEmpty()) {
            throw new IllegalArgumentException("Unsupported ruleset: " + ref.variant() + " / " + ref.version());
        }
    }

    public void requireCreatable(RulesetRef ref) {
        requireSupported(ref);
        if (!find(ref.variant()).orElseThrow().creationEnabled()) {
            throw new IllegalArgumentException("Game variant is not released: " + ref.variant());
        }
    }
}
