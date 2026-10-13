package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import de.zettsystems.starfare.economy.values.ProductionOutcome;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.i18n.I18n;
import java.util.function.IntConsumer;

/** Three real production functions: distinct silhouettes, invested points and their result per round. */
final class ProductionFunctions extends Div {
    private final IntConsumer select;

    ProductionFunctions(ProductionOutcome outcome, boolean metal, IntConsumer select) {
        this.select = select;
        addClassName("production-functions");
        show(outcome, metal);
    }

    void show(ProductionOutcome outcome, boolean metal) {
        removeAll();
        String ships = outcome.shipsShort()
                ? I18n.t(ProductionKeys.OUTCOME_SHIPS_SHORT, outcome.shipPoints(), outcome.ships(), outcome.plannedShips())
                : I18n.t(ProductionKeys.OUTCOME_SHIPS, outcome.shipPoints(), outcome.ships());
        addFunction(0, "shipyard", I18n.t(UiTexts.UX_BUILD_SHIPS), ships, outcome.ships() == 0, outcome.shipsShort());
        if (metal) {
            String mined = outcome.depositLimited()
                    ? I18n.t(ProductionKeys.OUTCOME_METAL_LIMITED, outcome.miningPoints(), outcome.metal())
                    : I18n.t(ProductionKeys.OUTCOME_METAL, outcome.miningPoints(), outcome.yieldPerPoint(), outcome.metal());
            addFunction(1, "mine", I18n.t(ProductionKeys.MINING_YIELD, outcome.yieldPerPoint()), mined,
                    outcome.metal() == 0, outcome.depositLimited());
        }
        String growth = outcome.atMaximum() ? I18n.t(UiTexts.ECONOMY_MAXIMUM)
                : I18n.t(ProductionKeys.OUTCOME_EXPANSION, outcome.expansionPoints(), outcome.progress(),
                        outcome.cost(), outcome.nextProgress());
        addFunction(2, "crane", I18n.t(UiTexts.UX_EXPAND_INDUSTRY), growth, outcome.expansionPoints() == 0, false);
    }

    private void addFunction(int index, String shape, String label, String result, boolean idle, boolean limited) {
        Button button = new Button();
        button.addClassNames("production-function", "production-function-" + shape);
        if (idle) { button.addClassName("production-function-idle"); }
        if (limited) { button.addClassName("production-function-limited"); }
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
