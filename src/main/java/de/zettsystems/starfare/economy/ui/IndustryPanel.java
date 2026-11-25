package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import de.zettsystems.starfare.economy.values.AllocationSplit;
import de.zettsystems.starfare.economy.values.EconomyRules;
import de.zettsystems.starfare.economy.values.IndustryView;
import de.zettsystems.starfare.economy.values.ProductionCommit;
import de.zettsystems.starfare.economy.values.ProductionInputs;
import de.zettsystems.starfare.economy.values.ProductionOutcome;
import de.zettsystems.starfare.economy.values.ProductionPlanPreview;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.CssProperties;
import java.util.Optional;
import java.util.function.Function;

/** Shows what the saved split produces and edits it in place; every completed change is saved at once. */
public final class IndustryPanel extends Div {
    private static final long serialVersionUID = 1L;
    public static final String ALLOCATION_ID = "industry-expansion";
    private @org.jspecify.annotations.Nullable ProductionEditor editor;
    public void focusProduction(int index) { if (editor != null) { editor.focus(index); } }
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
            editor = new ProductionEditor(input, actions.preview(), actions.commit());
            add(editor);
        } else {
            var functions = new ProductionFunctions(ProductionOutcome.of(AllocationSplit.saved(industry), industry), _ -> { });
            functions.getElement().getChildren().forEach(element -> element.setProperty("disabled", true));
            add(functions);
        }
        add(expansionProgress(industry));
    }

    private static Div expansionProgress(IndustryView industry) {
        Div progress = new Div(); progress.addClassName("industry-progress");
        progress.add(new Span(I18n.t(industry.atMaximum() ? UiTexts.ECONOMY_MAXIMUM : UiTexts.ECONOMY_PROGRESS,
                industry.expansionProgress(), industry.expansionCost())));
        if (!industry.atMaximum()) {
            Div track = new Div(); track.addClassName("industry-progress-track");
            Div fill = new Div(); fill.addClassName("industry-progress-fill");
            fill.getStyle().set(CssProperties.WIDTH, (100.0 * industry.expansionProgress() / industry.expansionCost()) + "%");
            track.add(fill); progress.add(track);
        }
        return progress;
    }

}
