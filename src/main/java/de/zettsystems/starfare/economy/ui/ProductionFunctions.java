package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import de.zettsystems.starfare.economy.values.IndustryView;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.i18n.I18n;
import java.util.function.IntConsumer;

/** Three real production functions, distinct silhouettes, saved allocations and activity. */
final class ProductionFunctions extends Div {
    ProductionFunctions(IndustryView industry, IntConsumer select) {
        addClassName("production-functions");
        addFunction(0, "shipyard", UiTexts.UX_BUILD_SHIPS, industry.shipbuilding(), select);
        var metal = industry.metal();
        if (metal != null) { addFunction(1, "mine", UiTexts.UX_MINE_METAL, metal.extraction(), select); }
        addFunction(2, "crane", UiTexts.UX_EXPAND_INDUSTRY, industry.expansionAllocation(), select);
    }
    private void addFunction(int index, String shape, String key, long activity, IntConsumer select) {
        Button button = new Button();
        button.addClassNames("production-function", "production-function-" + shape);
        if (activity == 0) { button.addClassName("production-function-idle"); }
        button.setId("production-function-" + shape);
        button.setAriaLabel(I18n.t(key));
        Span silhouette = new Span(); silhouette.addClassName("production-silhouette");
        Div caption = new Div(new Span(I18n.t(key)), new Span(String.valueOf(activity)));
        button.getElement().appendChild(silhouette.getElement(), caption.getElement());
        button.addClickListener(_ -> select.accept(index));
        add(button);
    }
}
