package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import de.zettsystems.starfare.economy.values.AllocationSplit;
import de.zettsystems.starfare.economy.values.EconomyRules;
import de.zettsystems.starfare.economy.values.IndustryView;
import de.zettsystems.starfare.economy.values.ProductionCommit;
import de.zettsystems.starfare.economy.values.ProductionInputs;
import de.zettsystems.starfare.economy.values.ProductionPlanPreview;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.i18n.I18n;
import java.util.Optional;
import java.util.function.Function;

/**
 * Capacity, the expansion share and one line with its result. Editable before the round is
 * submitted, where every completed change is saved at once; afterwards only the result is shown.
 */
public final class IndustryPanel extends Div {
    private static final long serialVersionUID = 1L;
    public static final String ALLOCATION_ID = "industry-expansion";
    static final String RESULT_CLASS = "production-result";
    public record Actions(Function<AllocationSplit, Optional<ProductionPlanPreview>> preview,
                          Function<AllocationSplit, ProductionCommit> commit) { }
    public IndustryPanel(IndustryView industry, boolean editable, Actions actions) {
        this(industry, editable, actions, new ProductionInputs().forSystem(0, 0, industry));
    }

    public IndustryPanel(IndustryView industry, boolean editable, Actions actions, ProductionInputs.Input input) {
        addClassName("industry-panel");
        add(new H3(I18n.t(UiTexts.ECONOMY_TITLE)),
                new Span(I18n.t(UiTexts.ECONOMY_CAPACITY, industry.capacity(), EconomyRules.MAX_CAPACITY)));
        if (editable) {
            add(new ProductionEditor(input, actions.preview(), actions.commit()));
        } else {
            Span result = new Span(result(industry));
            result.addClassName(RESULT_CLASS);
            add(result);
        }
    }

    /** Ships per round, expansion progress after the next round and the rounds until the level completes. */
    static String result(IndustryView industry) {
        if (industry.atMaximum()) { return I18n.t(ProductionKeys.RESULT_MAXIMUM, industry.shipbuilding()); }
        int rounds = industry.turnsToExpansion();
        return rounds > 0
                ? I18n.t(ProductionKeys.RESULT_ROUNDS, industry.shipbuilding(), industry.nextProgress(), industry.expansionCost(), rounds)
                : I18n.t(ProductionKeys.RESULT, industry.shipbuilding(), industry.nextProgress(), industry.expansionCost());
    }
}
