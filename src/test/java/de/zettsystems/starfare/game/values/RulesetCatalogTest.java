package de.zettsystems.starfare.game.values;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class RulesetCatalogTest {
    private final RulesetCatalog catalog = RulesetCatalog.builtIn();

    @Test
    void sectorForcesAndSpacewardAreReleased() {
        assertThat(catalog.definitions()).hasSize(2);
        assertThat(catalog.definitions().getFirst().defaultRef()).isEqualTo(RulesetRef.SECTOR_FORCES);
        assertThatCode(() -> catalog.requireCreatable(RulesetRef.SECTOR_FORCES)).doesNotThrowAnyException();
        assertThat(catalog.find(RulesetRef.SPACEWARD.variant()).orElseThrow().creationEnabled()).isTrue();
        assertThat(catalog.find(RulesetRef.SPACEWARD.variant()).orElseThrow().defaultRef())
                .isEqualTo(RulesetRef.SPACEWARD_ALLIANCE);
        assertThatCode(() -> catalog.requireCreatable(RulesetRef.SPACEWARD)).doesNotThrowAnyException();
        assertThatCode(() -> catalog.requireCreatable(RulesetRef.SPACEWARD_ALLIANCE)).doesNotThrowAnyException();
        assertThatThrownBy(() -> catalog.requireCreatable(new RulesetRef("spaceward", "2.0.0")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void onlySpacewardReplacesTheAttackOrderWithCoalitionBattles() {
        assertThat(RulesetRef.SPACEWARD.resolvesCoalitionBattles()).isTrue();
        assertThat(RulesetRef.SPACEWARD_ALLIANCE.resolvesCoalitionBattles()).isTrue();
        assertThat(RulesetRef.SECTOR_FORCES.resolvesCoalitionBattles()).isFalse();
        assertThat(new RulesetRef("spaceward", "2.0.0").resolvesCoalitionBattles()).isFalse();
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
