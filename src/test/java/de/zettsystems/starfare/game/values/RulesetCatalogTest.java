package de.zettsystems.starfare.game.values;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class RulesetCatalogTest {
    private final RulesetCatalog catalog = RulesetCatalog.builtIn();

    @Test void acceptanceSwitchEnablesOnlyTheCurrentCandidateAndKeepsRestorationAvailable() {
        var preview = RulesetCatalog.builtIn(true);
        assertThat(preview.find("spaceward").orElseThrow().defaultRef()).isEqualTo(RulesetRef.SPACEWARD_DIRECT_ECONOMY);
        assertThatCode(() -> preview.requireCreatable(RulesetRef.SPACEWARD_DIRECT_ECONOMY)).doesNotThrowAnyException();
        for (var stored : List.of(RulesetRef.SPACEWARD_ECONOMY_PREVIEW, RulesetRef.SPACEWARD_DIRECT_ECONOMY)) {
            assertThatCode(() -> catalog.requireSupported(stored)).doesNotThrowAnyException();
            assertThatThrownBy(() -> catalog.requireCreatable(stored)).isInstanceOf(IllegalArgumentException.class);
        }
        for (var old : List.of(RulesetRef.SPACEWARD_COLONIES, RulesetRef.SPACEWARD_METALS, RulesetRef.SPACEWARD_RECYCLING,
                RulesetRef.SPACEWARD_ECONOMY_PREVIEW)) {
            assertThatThrownBy(() -> preview.requireCreatable(old)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(preview.definitions().getFirst().defaultRef()).isEqualTo(RulesetRef.SECTOR_FORCES);
    }

    @Test void onlyTheDirectCandidateDropsTheLaborLimit() {
        assertThat(RulesetRef.SPACEWARD_DIRECT_ECONOMY.limitsIndustryByLabor()).isFalse();
        assertThat(RulesetRef.SPACEWARD_DIRECT_ECONOMY.usesRecycling()).isTrue();
        assertThat(RulesetRef.SPACEWARD_DIRECT_ECONOMY.usesConfigurableVictory()).isTrue();
        for (var limited : List.of(RulesetRef.SPACEWARD_COLONIES, RulesetRef.SPACEWARD_METALS, RulesetRef.SPACEWARD_RECYCLING,
                RulesetRef.SPACEWARD_ECONOMY_PREVIEW)) {
            assertThat(limited.limitsIndustryByLabor()).isTrue();
        }
        assertThat(RulesetRef.SPACEWARD_ALLIANCE.limitsIndustryByLabor()).isFalse();
    }

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
        var input1 = new RulesetRef("spaceward", "2.0.0");
        assertThatThrownBy(() -> catalog.requireCreatable(input1)).isInstanceOf(IllegalArgumentException.class);
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
        Set<String> input5 = Set.of();
        assertThatThrownBy(() -> new RulesetDefinition(RulesetRef.SPACEWARD, "test.name", "test.description", input5, true)).isInstanceOf(IllegalArgumentException.class);
    }
}
