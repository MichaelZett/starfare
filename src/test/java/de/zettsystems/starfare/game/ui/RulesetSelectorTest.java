package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.RulesetDefinition;
import de.zettsystems.starfare.game.values.RulesetRef;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RulesetSelectorTest {
    @Test void versionChoiceNotifiesTheWizardAndResetsWhenChangingVariants() {
        var catalog = RulesetCatalog.builtIn();
        var selector = new RulesetSelector(catalog);
        var seen = new ArrayList<RulesetRef>();
        selector.onRulesetChange(seen::add);
        RadioButtonGroup<RulesetDefinition> variants = field(selector, "ruleset-selection");
        RadioButtonGroup<RulesetRef> versions = field(selector, "ruleset-version-selection");
        assertThat(selector.selectedRuleset()).isEqualTo(RulesetRef.SECTOR_FORCES);
        assertThat(versions.isVisible()).isFalse();
        variants.setValue(catalog.find("spaceward").orElseThrow());
        assertThat(versions.isVisible()).isTrue();
        assertThat(versions.getListDataView().getItems()).containsExactly(
                RulesetRef.SPACEWARD_EARLY_INDUSTRY, RulesetRef.SPACEWARD_EQUAL_METAL);
        assertThat(selector.selectedRuleset()).isEqualTo(RulesetRef.SPACEWARD_EARLY_INDUSTRY);
        versions.setValue(RulesetRef.SPACEWARD_EQUAL_METAL);
        assertThat(selector.selectedRuleset()).isEqualTo(RulesetRef.SPACEWARD_EQUAL_METAL);
        variants.setValue(catalog.find("classic").orElseThrow());
        assertThat(selector.selectedRuleset()).isEqualTo(RulesetRef.SECTOR_FORCES);
        assertThat(versions.isVisible()).isFalse();
        variants.setValue(catalog.find("spaceward").orElseThrow());
        assertThat(selector.selectedRuleset()).isEqualTo(RulesetRef.SPACEWARD_EARLY_INDUSTRY);
        assertThat(seen).containsExactly(RulesetRef.SECTOR_FORCES, RulesetRef.SPACEWARD_EARLY_INDUSTRY,
                RulesetRef.SPACEWARD_EQUAL_METAL, RulesetRef.SECTOR_FORCES, RulesetRef.SPACEWARD_EARLY_INDUSTRY);
    }

    @SuppressWarnings("unchecked")
    private static <T> RadioButtonGroup<T> field(RulesetSelector selector, String id) {
        return (RadioButtonGroup<T>) selector.getChildren().filter(component -> component.getId().orElse("").equals(id))
                .findFirst().orElseThrow();
    }
}
