package de.zettsystems.starfare.game.values;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Immutable, extensible catalog used by creation, restoration and rule dispatch. */
public record RulesetCatalog(List<RulesetDefinition> definitions) {
    /** Default for new Spaceward games; W6 remains selectable for comparison. */
    public static final RulesetRef ECONOMY_CANDIDATE = RulesetRef.SPACEWARD_EARLY_INDUSTRY;

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
                new RulesetDefinition(ECONOMY_CANDIDATE, RulesetDefinition.SPACEWARD_NAME,
                        RulesetDefinition.SPACEWARD_DESCRIPTION, Set.of(RulesetRef.SPACEWARD.version(), RulesetRef.SPACEWARD_ALLIANCE.version(), RulesetRef.SPACEWARD_COLONIES.version(), RulesetRef.SPACEWARD_METALS.version(), RulesetRef.SPACEWARD_RECYCLING.version(), RulesetRef.SPACEWARD_ECONOMY_PREVIEW.version(), RulesetRef.SPACEWARD_DIRECT_ECONOMY.version(), RulesetRef.SPACEWARD_EQUAL_METAL.version(), RulesetRef.SPACEWARD_EARLY_INDUSTRY.version(), RulesetRef.SPACEWARD_SHIP_RESEARCH.version()), true,
                        Set.of(RulesetRef.SPACEWARD_EQUAL_METAL.version(), RulesetRef.SPACEWARD_EARLY_INDUSTRY.version()))));
    }

    /** Research preview is explicit; ordinary W6/W7 creation remains unchanged. */
    public static RulesetCatalog withShipResearch() {
        return new RulesetCatalog(builtIn().definitions().stream().map(entry -> {
            if (!entry.defaultRef().isSpaceward()) { return entry; }
            return new RulesetDefinition(RulesetRef.SPACEWARD_SHIP_RESEARCH, entry.nameKey(), entry.descriptionKey(),
                    entry.supportedVersions(), true, Set.of(RulesetRef.SPACEWARD_EQUAL_METAL.version(),
                    RulesetRef.SPACEWARD_EARLY_INDUSTRY.version(), RulesetRef.SPACEWARD_SHIP_RESEARCH.version()));
        }).toList());
    }

    /** Compatibility for retained launch profiles: selection is no longer gated by this switch. */
    public static RulesetCatalog builtIn(boolean economyPreview) { return builtIn(); }

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
        if (!find(ref.variant()).orElseThrow().creationChoices().contains(ref)) {
            throw new IllegalArgumentException("Ruleset is not available for new games: " + ref.variant() + " / " + ref.version());
        }
    }
}
