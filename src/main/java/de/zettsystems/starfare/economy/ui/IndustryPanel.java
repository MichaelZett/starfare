package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import de.zettsystems.starfare.economy.values.EconomyRules;
import de.zettsystems.starfare.economy.values.IndustryView;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.CssProperties;
import de.zettsystems.starfare.style.HtmlAttributes;
import java.util.function.IntConsumer;

/** Explains the saved allocation and previews an edit before applying it. */
public final class IndustryPanel extends Div {
    private static final long serialVersionUID = 1L;
    public static final String ALLOCATION_ID = "industry-expansion";
    public static final String APPLY_ID = "industry-apply";
    private final Div colonyDetails = new Div();
    private final Div background = new Div();
    private @org.jspecify.annotations.Nullable ProductionEditor editor;
    public void focusProduction(int index) { if (editor != null) { editor.focus(index); } }
    public record Actions(IntConsumer recycling,
                          java.util.function.Function<de.zettsystems.starfare.economy.values.AllocationSplit,
                                  java.util.Optional<de.zettsystems.starfare.economy.values.ProductionPlanPreview>> preview,
                          java.util.function.Predicate<de.zettsystems.starfare.economy.values.AllocationSplit> commit) { }
    public IndustryPanel(IndustryView industry, boolean editable, Actions actions) {
        this(industry, editable, actions, new de.zettsystems.starfare.economy.values.ProductionDrafts()
                .forSystem(0, 0, industry), 0, () -> { });
    }

    public IndustryPanel(IndustryView industry, boolean editable, Actions actions,
                         de.zettsystems.starfare.economy.values.ProductionDrafts.Draft draft,
                         int turn, Runnable changed) {
        addClassName("industry-panel");
        add(new H3(I18n.t(UiTexts.ECONOMY_TITLE)),
                new Span(I18n.t(UiTexts.ECONOMY_CAPACITY, industry.capacity(), EconomyRules.MAX_CAPACITY)));
        var metal = industry.metal();
        if (metal != null) { addClassName("metal-details"); }
        int mining = metal == null ? 0 : metal.miningPoints();
        int ships = industry.usableCapacity() - mining - industry.expansionAllocation();
        String allocation = metal == null
                ? I18n.t(UiTexts.UX_ALLOCATION_WITHOUT_METAL, industry.expansionAllocation(), ships)
                : I18n.t(UiTexts.UX_ALLOCATION_SUMMARY, mining, industry.expansionAllocation(), ships);
        add(new Span(allocation),
                new Div(new Span(I18n.t(UiTexts.UX_SHIP_RESULT,
                        industry.usableCapacity() - mining - industry.expansionAllocation(), industry.shipbuilding()))),
                expansionProgress(industry));
        if (industry.usableCapacity() < industry.capacity()) {
            add(new Span(I18n.t(UiTexts.UX_LABOR_LIMIT, industry.usableCapacity(), industry.capacity())));
        }
        if (editable) {
            editor = new ProductionEditor(draft, turn, actions.preview(), actions.commit(), changed);
            add(editor);
        } else {
            add(new ProductionFunctions(industry, _ -> { }));
            getChildren().filter(ProductionFunctions.class::isInstance).forEach(component ->
                    component.getElement().getChildren().forEach(element -> element.setProperty("disabled", true)));
        }
        showColony(industry);
        if (metal != null) {
            background.add(new Div(new Span(I18n.t(UiTexts.METAL_DEPOSIT, metal.remaining(), metal.yieldPerPoint()))));
            var recycling = metal.recycling();
            if (recycling != null) { add(new RecyclingPanel(recycling, editable, actions.recycling())); }
        }
    }

    public Div colonyDetails() { return colonyDetails; }

    public Div background() { return background; }

    private void showColony(IndustryView industry) {
        var colony = industry.colony();
        if (colony == null) { return; }
        var quality = colony.quality();
        Div details = new Div();
        colonyDetails.addClassName("colony-details");
        String suitability = switch (quality.suitability()) {
            case HARSH -> UiTexts.COLONY_HARSH;
            case HABITABLE -> UiTexts.COLONY_HABITABLE;
            case FERTILE -> UiTexts.COLONY_FERTILE;
        };
        String atmosphere = switch (quality.atmosphere()) {
            case NONE -> UiTexts.COLONY_ATMOSPHERE_NONE;
            case THIN -> UiTexts.COLONY_ATMOSPHERE_THIN;
            case TEMPERATE -> UiTexts.COLONY_ATMOSPHERE_TEMPERATE;
            case DENSE -> UiTexts.COLONY_ATMOSPHERE_DENSE;
        };
        colonyDetails.add(new Div(new Span(I18n.t(UiTexts.COLONY_LOCATION,
                        I18n.t(quality.moon() ? UiTexts.COLONY_MOON : UiTexts.COLONY_PLANET), I18n.t(suitability)))),
                new Div(new Span(I18n.t(UiTexts.COLONY_POPULATION, colony.population() / 100.0,
                        colony.populationLimit() / 100.0, colony.nextPopulation() / 100.0))),
                new Div(new Span(I18n.t(UiTexts.COLONY_LABOR, industry.usableCapacity(), industry.capacity()))));
        details.add(new Div(new Span(I18n.t(UiTexts.COLONY_ENVIRONMENT, quality.orbitAu(), quality.luminosity(), I18n.t(atmosphere)))),
                new Div(new Span(I18n.t(UiTexts.COLONY_RICHNESS, quality.metalRichness()))));
        background.add(details);
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
