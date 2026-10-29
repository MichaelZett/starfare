package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.RulesetDefinition;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.i18n.I18n;

import java.util.function.Consumer;

/** Offers creatable game versions and explains planned variants before game-specific settings. */
final class RulesetSelector extends VerticalLayout {
    private final RadioButtonGroup<RulesetDefinition> variants = new RadioButtonGroup<>();
    private final RulesetCatalog catalog;

    RulesetSelector(RulesetCatalog catalog) {
        this.catalog = catalog;
        setPadding(false);
        addClassName("ruleset-selection");
        variants.setId("ruleset-selection");
        variants.setLabel(I18n.t(UiTexts.RULESET_CHOOSE));
        variants.setItems(catalog.definitions());
        variants.setItemLabelGenerator(entry -> I18n.t(entry.nameKey()));
        variants.setItemEnabledProvider(RulesetDefinition::creationEnabled);
        variants.setValue(catalog.definitions().stream().filter(RulesetDefinition::creationEnabled)
                .findFirst().orElseThrow());
        Div cards = new Div();
        cards.addClassName("ruleset-cards");
        add(variants, cards);
        for (RulesetDefinition entry : catalog.definitions()) {
            Span title = new Span(I18n.t(entry.nameKey()));
            title.addClassName("ruleset-card-title");
            Span status = new Span(entry.creationEnabled()
                    ? I18n.t(UiTexts.RULESET_VERSION, entry.defaultRef().version())
                    : I18n.t(UiTexts.RULESET_PLANNED));
            Div card = new Div(title, status, new Span(I18n.t(entry.descriptionKey())));
            card.addClassName("ruleset-card");
            card.setVisible(entry.equals(variants.getValue()));
            variants.addValueChangeListener(event -> card.setVisible(entry.equals(event.getValue())));
            cards.add(card);
        }
    }

    /** Notifies about the current choice immediately and after every change. */
    void onRulesetChange(Consumer<RulesetRef> listener) {
        variants.addValueChangeListener(event -> {
            if (event.getValue() != null) { listener.accept(event.getValue().defaultRef()); }
        });
        listener.accept(selectedRuleset());
    }

    RulesetRef selectedRuleset() {
        var entry = variants.getValue();
        var selected = entry == null ? null : entry.defaultRef();
        if (selected == null) { throw new IllegalStateException("Select a released game variant"); }
        catalog.requireCreatable(selected);
        return selected;
    }
}
