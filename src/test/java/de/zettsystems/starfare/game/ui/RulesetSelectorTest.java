package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.RulesetDefinition;
import de.zettsystems.starfare.game.values.RulesetRef;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RulesetSelectorTest {
    @Test void variantChangesUseOnlyCurrentRulesAndNotifyTheWizard() {
        var catalog = RulesetCatalog.builtIn();
        var selector = new RulesetSelector(catalog);
        var seen = new ArrayList<RulesetRef>();
        selector.onRulesetChange(seen::add);
        RadioButtonGroup<RulesetDefinition> variants = field(selector, "ruleset-selection");
        assertThat(selector.getChildren().filter(component ->
                component.getId().orElse("").equals("ruleset-version-selection"))).isEmpty();
        assertThat(selector.selectedRuleset()).isEqualTo(RulesetRef.SECTOR_FORCES);
        variants.setValue(catalog.find("spaceward").orElseThrow());
        assertThat(selector.selectedRuleset()).isEqualTo(RulesetRef.SPACEWARD_SHIP_RESEARCH);
        variants.setValue(catalog.find("classic").orElseThrow());
        assertThat(selector.selectedRuleset()).isEqualTo(RulesetRef.SECTOR_FORCES);
        variants.setValue(catalog.find("spaceward").orElseThrow());
        assertThat(selector.selectedRuleset()).isEqualTo(RulesetRef.SPACEWARD_SHIP_RESEARCH);
        assertThat(seen).containsExactly(RulesetRef.SECTOR_FORCES, RulesetRef.SPACEWARD_SHIP_RESEARCH,
                RulesetRef.SECTOR_FORCES, RulesetRef.SPACEWARD_SHIP_RESEARCH);
    }

    @SuppressWarnings("unchecked")
    private static <T> RadioButtonGroup<T> field(RulesetSelector selector, String id) {
        return (RadioButtonGroup<T>) selector.getChildren().filter(component -> component.getId().orElse("").equals(id))
                .findFirst().orElseThrow();
    }
}
