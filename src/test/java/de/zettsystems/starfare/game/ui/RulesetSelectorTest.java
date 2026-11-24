package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.RulesetDefinition;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.gamesplatformapi.LifecycleStage;
import java.util.ArrayList;
import java.util.Optional;
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

    @Test void variantsTheAccountMayNotUseAreNotOfferedAndTheRemainingOneIsPreselected() {
        var catalog = RulesetCatalog.builtIn();
        var selector = new RulesetSelector(catalog, new RulesetOffer(ruleset -> !ruleset.isSpaceward(), _ -> Optional.empty()));
        RadioButtonGroup<RulesetDefinition> variants = field(selector, "ruleset-selection");
        assertThat(variants.getListDataView().getItems().map(RulesetDefinition::defaultRef))
                .containsExactly(RulesetRef.SECTOR_FORCES);
        assertThat(selector.selectedRuleset()).isEqualTo(RulesetRef.SECTOR_FORCES);
    }

    @Test void marksVariantsInAlphaOrBetaAndLeavesProductionUnmarked() {
        var catalog = RulesetCatalog.builtIn();
        var selector = new RulesetSelector(catalog, new RulesetOffer(_ -> true,
                ruleset -> ruleset.isSpaceward() ? Optional.of(LifecycleStage.ALPHA) : Optional.empty()));
        RadioButtonGroup<RulesetDefinition> variants = field(selector, "ruleset-selection");
        var renderer = variants.getItemRenderer();
        java.util.function.Function<String, String> statusOf = variant -> {
            var card = ((com.vaadin.flow.data.renderer.ComponentRenderer<?, RulesetDefinition>) renderer)
                    .createComponent(catalog.find(variant).orElseThrow());
            return card.getChildren().map(child -> (com.vaadin.flow.component.html.Span) child)
                    .filter(span -> span.hasClassName("ruleset-card-status")).findFirst().orElseThrow().getText();
        };
        assertThat(statusOf.apply("spaceward")).endsWith("Alpha");
        assertThat(statusOf.apply("classic")).doesNotContain("Alpha").doesNotContain("Beta");
    }

    @SuppressWarnings("unchecked")
    private static <T> RadioButtonGroup<T> field(RulesetSelector selector, String id) {
        return (RadioButtonGroup<T>) selector.getChildren().filter(component -> component.getId().orElse("").equals(id))
                .findFirst().orElseThrow();
    }
}
