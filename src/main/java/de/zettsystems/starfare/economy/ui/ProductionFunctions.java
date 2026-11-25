package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import de.zettsystems.starfare.economy.values.AllocationSplit;
import de.zettsystems.starfare.economy.values.ProductionOutcome;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.i18n.I18n;
import java.util.function.IntConsumer;

/** The two production shares: distinct silhouettes, invested points and their result per round. */
final class ProductionFunctions extends Div {
    private final IntConsumer select;

    ProductionFunctions(ProductionOutcome outcome, IntConsumer select) {
        this.select = select;
        addClassName("production-functions");
        show(outcome);
    }

    void show(ProductionOutcome outcome) {
        removeAll();
        addFunction(AllocationSplit.SHIPS, "shipyard", I18n.t(UiTexts.UX_BUILD_SHIPS),
                I18n.t(ProductionKeys.OUTCOME_SHIPS, outcome.shipPoints(), outcome.ships()), outcome.ships() == 0);
        String growth = outcome.atMaximum() ? I18n.t(UiTexts.ECONOMY_MAXIMUM)
                : I18n.t(ProductionKeys.OUTCOME_EXPANSION, outcome.expansionPoints(), outcome.progress(),
                        outcome.cost(), outcome.nextProgress());
        addFunction(AllocationSplit.EXPANSION, "crane", I18n.t(UiTexts.UX_EXPAND_INDUSTRY), growth,
                outcome.expansionPoints() == 0);
    }

    private void addFunction(int index, String shape, String label, String result, boolean idle) {
        Button button = new Button();
        button.addClassNames("production-function", "production-function-" + shape);
        if (idle) { button.addClassName("production-function-idle"); }
        button.setId("production-function-" + shape);
        button.setAriaLabel(label + ": " + result);
        Span silhouette = new Span(); silhouette.addClassName("production-silhouette");
        Span outcome = new Span(result); outcome.addClassName("production-function-result");
        Div caption = new Div(new Span(label), outcome);
        button.getElement().appendChild(silhouette.getElement(), caption.getElement());
        button.addClickListener(_ -> select.accept(index));
        add(button);
    }
}
