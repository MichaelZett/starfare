package de.zettsystems.starfare.research;

import de.zettsystems.starfare.game.config.RulesetConfiguration;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.RulesetRef;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.*;

class ResearchAvailabilityTest {
    @Test void currentRulesAreAvailableRegardlessOfRetainedPreviewSettings() {
        for (boolean preview : new boolean[] {false, true}) {
            new ApplicationContextRunner().withUserConfiguration(RulesetConfiguration.class)
                    .withPropertyValues("starfare.rules.research-preview=" + preview,
                            "starfare.rules.economy-preview=" + preview).run(context -> {
                        var catalog = context.getBean(RulesetCatalog.class);
                        for (var entry : catalog.definitions()) {
                            assertThat(entry.creationChoices()).containsExactly(entry.defaultRef());
                        }
                        assertThat(catalog.find("spaceward").orElseThrow().defaultRef())
                                .isEqualTo(RulesetRef.SPACEWARD_SHIP_RESEARCH);
                        assertThatCode(() -> catalog.requireCreatable(RulesetRef.SPACEWARD_SHIP_RESEARCH))
                                .doesNotThrowAnyException();
                        assertThatThrownBy(() -> catalog.requireCreatable(RulesetRef.SPACEWARD_EARLY_INDUSTRY))
                                .isInstanceOf(IllegalArgumentException.class);
                    });
        }
        assertThat(RulesetCatalog.withShipResearch()).isEqualTo(RulesetCatalog.builtIn());
    }
}
