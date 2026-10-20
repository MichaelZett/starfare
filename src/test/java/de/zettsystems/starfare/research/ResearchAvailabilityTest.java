package de.zettsystems.starfare.research;

import de.zettsystems.starfare.game.config.RulesetConfiguration;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.RulesetRef;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.*;

class ResearchAvailabilityTest {
    @Test void researchIsStoredButCreationRequiresTheExplicitPreview() {
        var ordinary = RulesetCatalog.builtIn();
        assertThatCode(() -> ordinary.requireSupported(RulesetRef.SPACEWARD_SHIP_RESEARCH)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ordinary.requireCreatable(RulesetRef.SPACEWARD_SHIP_RESEARCH)).isInstanceOf(IllegalArgumentException.class);
        assertThat(ordinary.find("spaceward").orElseThrow().creationChoices()).containsExactly(
                RulesetRef.SPACEWARD_EARLY_INDUSTRY, RulesetRef.SPACEWARD_EQUAL_METAL);
        new ApplicationContextRunner().withUserConfiguration(RulesetConfiguration.class)
                .withPropertyValues("starfare.rules.research-preview=true").run(context -> {
                    var preview = context.getBean(RulesetCatalog.class);
                    assertThat(preview.find("spaceward").orElseThrow().defaultRef()).isEqualTo(RulesetRef.SPACEWARD_SHIP_RESEARCH);
                    assertThat(preview.definitions().getFirst().defaultRef()).isEqualTo(RulesetRef.SECTOR_FORCES);
                    assertThatCode(() -> preview.requireCreatable(RulesetRef.SPACEWARD_SHIP_RESEARCH)).doesNotThrowAnyException();
                    assertThatCode(() -> preview.requireCreatable(RulesetRef.SPACEWARD_EARLY_INDUSTRY)).doesNotThrowAnyException();
                });
    }
}
