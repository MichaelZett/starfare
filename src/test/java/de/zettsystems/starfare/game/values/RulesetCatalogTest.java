package de.zettsystems.starfare.game.values;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class RulesetCatalogTest {
    private final RulesetCatalog catalog = RulesetCatalog.builtIn();

    @Test
    void sectorForcesIsReleasedWhileSpacewardHasNoImplementedRules() {
        assertThat(catalog.definitions()).hasSize(2);
        assertThatCode(() -> catalog.requireCreatable(RulesetRef.SECTOR_FORCES)).doesNotThrowAnyException();
        assertThat(catalog.find(RulesetRef.SPACEWARD.variant()).orElseThrow().creationEnabled()).isFalse();
        assertThatThrownBy(() -> catalog.requireSupported(RulesetRef.SPACEWARD))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unknownVersionsAndVariantsNeverFallBackToClassic() {
        assertThatThrownBy(() -> catalog.requireCreatable(new RulesetRef("classic", "2.0.0")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalog.requireSupported(new RulesetRef("unknown", "1.0.0")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RulesetRef("classic", " ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void disablingCreationStillAllowsAnImplementedStoredVersion() {
        var definition = new RulesetDefinition(RulesetRef.SECTOR_FORCES, "test.name", "test.description",
                Set.of("1.0.0"), false);
        var disabled = new RulesetCatalog(List.of(definition));
        assertThatCode(() -> disabled.requireSupported(RulesetRef.SECTOR_FORCES)).doesNotThrowAnyException();
        assertThatThrownBy(() -> disabled.requireCreatable(RulesetRef.SECTOR_FORCES))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void additionalCatalogEntriesAndVersionsDoNotChangeExistingIdentity() {
        RulesetRef additional = new RulesetRef("test-third", "3.2.1");
        var entries = new java.util.ArrayList<>(catalog.definitions());
        entries.add(new RulesetDefinition(additional, "test.name", "test.description", Set.of("3.2.1"), true));
        var extended = new RulesetCatalog(entries);
        entries.clear();
        assertThat(extended.definitions()).hasSize(3);
        assertThatCode(() -> extended.requireCreatable(additional)).doesNotThrowAnyException();
        assertThatCode(() -> extended.requireCreatable(RulesetRef.SECTOR_FORCES)).doesNotThrowAnyException();
        assertThat(GameSetup.defaults().selectRuleset(additional).normalized().ruleset()).isEqualTo(additional);
    }

    @Test
    void duplicateIdentitiesAndReleasedUnimplementedDefaultsAreRejected() {
        var entry = catalog.definitions().getFirst();
        assertThatThrownBy(() -> new RulesetCatalog(List.of(entry, entry))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RulesetDefinition(RulesetRef.SPACEWARD,
                "test.name", "test.description", Set.of(), true)).isInstanceOf(IllegalArgumentException.class);
    }
}
