package de.zettsystems.starfare.game.values;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class RulesetCatalogTest {
    private final RulesetCatalog catalog = RulesetCatalog.builtIn();

    @Test void eachVariantHasExactlyOneVersion() {
        var extended = catalog.find("spaceward").orElseThrow();
        assertThat(extended.defaultRef()).isEqualTo(RulesetRef.EXTENDED);
        assertThat(extended.supportedVersions()).containsExactly(RulesetRef.EXTENDED.version());
        assertThat(extended.creationEnabled()).isTrue();
        assertThatCode(() -> catalog.requireCreatable(RulesetRef.EXTENDED)).doesNotThrowAnyException();
        var classic = catalog.find("classic").orElseThrow();
        assertThat(classic.supportedVersions()).containsExactly(RulesetRef.SECTOR_FORCES.version());
        assertThatCode(() -> catalog.requireCreatable(RulesetRef.SECTOR_FORCES)).doesNotThrowAnyException();
        assertThat(catalog.definitions()).extracting(RulesetDefinition::defaultRef)
                .containsExactly(RulesetRef.SECTOR_FORCES, RulesetRef.EXTENDED);
    }

    @Test void removedExtendedVersionsAreNoLongerSupported() {
        for (var version : List.of("1.0.0", "1.1.0", "1.2.0-w7", "1.3.0-r1")) {
            var removed = new RulesetRef("spaceward", version);
            assertThat(removed.isSpaceward()).isTrue();
            assertThatThrownBy(() -> catalog.requireSupported(removed)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test void onlyTheClassicRulesUseEventsStartBalanceAndRoundLimit() {
        assertThat(RulesetRef.SECTOR_FORCES.usesSectorOptions()).isTrue();
        assertThat(RulesetRef.EXTENDED.usesSectorOptions()).isFalse();
        assertThat(RulesetRef.SECTOR_FORCES.isSpaceward()).isFalse();
    }

    @Test
    void unknownVersionsAndVariantsNeverFallBackToClassic() {
        var input2 = new RulesetRef("classic", "2.0.0");
        assertThatThrownBy(() -> catalog.requireCreatable(input2)).isInstanceOf(IllegalArgumentException.class);
        var input3 = new RulesetRef("unknown", "1.0.0");
        assertThatThrownBy(() -> catalog.requireSupported(input3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RulesetRef("classic", " ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void disablingCreationStillAllowsAnImplementedStoredVersion() {
        var definition = new RulesetDefinition(RulesetRef.SECTOR_FORCES, "test.name", "test.description",
                Set.of("1.0.0"), false);
        var disabled = new RulesetCatalog(List.of(definition));
        assertThatCode(() -> disabled.requireSupported(RulesetRef.SECTOR_FORCES)).doesNotThrowAnyException();
        assertThat(definition.creationChoices()).isEmpty();
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
        var input4 = List.of(entry, entry);
        assertThatThrownBy(() -> new RulesetCatalog(input4)).isInstanceOf(IllegalArgumentException.class);
        Set<String> released = Set.of("1.0.0");
        Set<String> withUnimplemented = Set.of("1.0.0", "9.0.0");
        assertThatThrownBy(() -> new RulesetDefinition(RulesetRef.EXTENDED, "test.name", "test.description",
                released, true, withUnimplemented)).isInstanceOf(IllegalArgumentException.class);
        Set<String> input5 = Set.of();
        assertThatThrownBy(() -> new RulesetDefinition(RulesetRef.EXTENDED, "test.name", "test.description", input5, true)).isInstanceOf(IllegalArgumentException.class);
    }
}
