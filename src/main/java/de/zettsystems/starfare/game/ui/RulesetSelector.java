package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.RulesetDefinition;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.i18n.I18n;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Offers creatable game versions and explains planned variants before game-specific settings. */
final class RulesetSelector extends VerticalLayout {
    private final RadioButtonGroup<RulesetDefinition> variants = new RadioButtonGroup<>();
    private final RulesetCatalog catalog;

    RulesetSelector(RulesetCatalog catalog) {
        this(catalog, _ -> true);
    }

    /**
     * {@code usable} blendet Varianten aus, die das Konto nicht nutzen darf (Testvarianten im
     * Plattform-Host). Vorausgewählt ist die erste verbleibende spielbare Variante.
     */
    RulesetSelector(RulesetCatalog catalog, Predicate<RulesetRef> usable) {
        this.catalog = catalog;
        setPadding(false);
        addClassName("ruleset-selection");
        variants.setId("ruleset-selection");
        variants.setLabel(I18n.t(UiTexts.RULESET_CHOOSE));
        List<RulesetDefinition> offered = catalog.definitions().stream()
                .filter(entry -> usable.test(entry.defaultRef())).toList();
        variants.setItems(offered);
        variants.setRenderer(new ComponentRenderer<>(RulesetSelector::card));
        variants.setItemEnabledProvider(RulesetDefinition::creationEnabled);
        variants.setValue(offered.stream().filter(RulesetDefinition::creationEnabled)
                .findFirst().orElseThrow());
        add(variants);
    }

    /** CSS class that paints the artwork of a variant behind its card and the summary header. */
    static String artClass(RulesetRef ruleset) {
        return "wizard-art-" + ruleset.variant();
    }

    private static Div card(RulesetDefinition entry) {
        Span title = new Span(I18n.t(entry.nameKey()));
        title.addClassName("ruleset-card-title");
        Span status = new Span(status(entry));
        status.addClassName("ruleset-card-status");
        Span description = new Span(I18n.t(entry.descriptionKey()));
        description.addClassName("ruleset-card-description");
        Div card = new Div(title, status, description);
        card.addClassNames("ruleset-card", artClass(entry.defaultRef()));
        return card;
    }

    private static String status(RulesetDefinition entry) {
        if (!entry.creationEnabled()) { return I18n.t(UiTexts.RULESET_PLANNED); }
        // Im eigenen Start ist alles frei; der Hinweis gilt nur im Plattform-Host.
        boolean testersOnly = entry.testing() && !OwnFrame.active();
        return I18n.t(testersOnly ? UiTexts.RULESET_VERSION_TESTING : UiTexts.RULESET_VERSION, entry.defaultRef().version());
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
