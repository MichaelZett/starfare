package de.zettsystems.starfare.game.values;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class RulesetCatalogTest {
    private final RulesetCatalog catalog = RulesetCatalog.builtIn();

    @Test void onlyW6AndW7AreCreatableWhileAllOlderSpacewardVersionsRemainSupported() {
        var entry = catalog.find("spaceward").orElseThrow();
        assertThat(entry.defaultRef()).isEqualTo(RulesetRef.SPACEWARD_EARLY_INDUSTRY);
        assertThat(entry.creationChoices()).containsExactly(RulesetRef.SPACEWARD_EARLY_INDUSTRY, RulesetRef.SPACEWARD_EQUAL_METAL);
        for (var version : entry.supportedVersions()) {
            var rules = new RulesetRef("spaceward", version);
            assertThatCode(() -> catalog.requireSupported(rules)).doesNotThrowAnyException();
            if (entry.creationChoices().contains(rules)) {
                assertThatCode(() -> catalog.requireCreatable(rules)).doesNotThrowAnyException();
            } else {
                assertThatThrownBy(() -> catalog.requireCreatable(rules)).isInstanceOf(IllegalArgumentException.class);
            }
        }
        assertThat(RulesetCatalog.builtIn(true)).isEqualTo(catalog);
        assertThat(RulesetCatalog.builtIn(false)).isEqualTo(catalog);
    }

    @Test void directEconomyCandidatesDropTheLaborLimitAndLaterCandidatesShareMetalEqually() {
        assertThat(RulesetRef.SPACEWARD_DIRECT_ECONOMY.limitsIndustryByLabor()).isFalse();
        assertThat(RulesetRef.SPACEWARD_EQUAL_METAL.limitsIndustryByLabor()).isFalse();
        assertThat(RulesetRef.SPACEWARD_EQUAL_METAL.sharesMetalEqually()).isTrue();
        assertThat(RulesetRef.SPACEWARD_EQUAL_METAL.usesCheapEarlyIndustry()).isFalse();
        assertThat(RulesetRef.SPACEWARD_EARLY_INDUSTRY.usesCheapEarlyIndustry()).isTrue();
        assertThat(RulesetRef.SPACEWARD_EARLY_INDUSTRY.limitsIndustryByLabor()).isFalse();
        assertThat(RulesetRef.SPACEWARD_EARLY_INDUSTRY.sharesMetalEqually()).isTrue();
        assertThat(RulesetRef.SPACEWARD_EARLY_INDUSTRY.adjustsRelocationsToShipbuilding()).isTrue();
        assertThat(RulesetRef.SPACEWARD_EARLY_INDUSTRY.usesRecycling()).isTrue();
        assertThat(RulesetRef.SPACEWARD_DIRECT_ECONOMY.sharesMetalEqually()).isFalse();
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
                .isEqualTo(RulesetRef.SPACEWARD_EARLY_INDUSTRY);
        assertThatCode(() -> catalog.requireCreatable(RulesetRef.SPACEWARD_EQUAL_METAL)).doesNotThrowAnyException();
        assertThatCode(() -> catalog.requireCreatable(RulesetRef.SPACEWARD_EARLY_INDUSTRY)).doesNotThrowAnyException();
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
        assertThatThrownBy(() -> new RulesetDefinition(RulesetRef.SPACEWARD, "test.name", "test.description",
                Set.of("1.0.0"), true, Set.of("1.0.0", "9.0.0"))).isInstanceOf(IllegalArgumentException.class);
        Set<String> input5 = Set.of();
        assertThatThrownBy(() -> new RulesetDefinition(RulesetRef.SPACEWARD, "test.name", "test.description", input5, true)).isInstanceOf(IllegalArgumentException.class);
    }
}
