package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.economy.values.IndustryView;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.i18n.I18n;
import java.util.Objects;
import java.util.function.BiConsumer;

/** Edits mining and expansion together; forecast is refreshed after the accepted command. */
public final class MetalEconomyPanel extends Div {
    private static final long serialVersionUID = 1L;
    public static final String MINING_ID = "industry-mining";
    public MetalEconomyPanel(IndustryView industry, boolean editable, BiConsumer<Integer, Integer> apply) {
        this(industry, editable, apply, _ -> { });
    }
    public MetalEconomyPanel(IndustryView industry, boolean editable, BiConsumer<Integer, Integer> apply,
                             java.util.function.IntConsumer recycle) {
        addClassName("metal-details");
        var metal = industry.metal();
        if (metal == null) { return; }
        Div output = new Div(new Span(I18n.t(UiTexts.METAL_SHIPS, metal.actualShips(), metal.plannedShips())));
        if (metal.actualShips() < metal.plannedShips()) { output.addClassName("industry-bottleneck"); }
        add(new Div(new Span(I18n.t(UiTexts.METAL_DEPOSIT, metal.remaining(), metal.yieldPerPoint()))),
                new Div(new Span(I18n.t(UiTexts.METAL_STOCK, metal.stock(), metal.extraction()))),
                output,
                new Div(new Span(I18n.t(UiTexts.METAL_ASSIGNMENT, metal.miningPoints(), industry.expansionAllocation()))));
        if (editable) { addEditor(industry, apply); }
        var recycling = metal.recycling();
        if (recycling != null) { add(new RecyclingPanel(recycling, editable, recycle)); }
        Span hint = new Span(I18n.t(UiTexts.METAL_FORECAST_HINT));
        hint.addClassName("industry-hint"); add(hint);
    }
    private void addEditor(IndustryView industry, BiConsumer<Integer, Integer> apply) {
        IntegerField mining = field(UiTexts.METAL_MINING, MINING_ID, industry.usableCapacity(),
                Objects.requireNonNull(industry.metal()).miningPoints());
        IntegerField expansion = field(UiTexts.ECONOMY_ALLOCATION, IndustryPanel.ALLOCATION_ID,
                industry.atMaximum() ? 0 : industry.usableCapacity(), industry.expansionAllocation());
        Span capacity = new Span();
        Runnable validate = () -> {
            Integer mine = mining.getValue();
            Integer grow = expansion.getValue();
            boolean invalid = mine == null || grow == null || mine < 0 || grow < 0
                    || (long) mine + grow > industry.usableCapacity();
            mining.setInvalid(invalid); expansion.setInvalid(invalid);
            capacity.setText(I18n.t(UiTexts.METAL_CAPACITY, industry.usableCapacity()));
        };
        mining.addValueChangeListener(_ -> validate.run());
        expansion.addValueChangeListener(_ -> validate.run());
        Button save = new Button(I18n.t(UiTexts.ECONOMY_APPLY), _ -> {
            validate.run();
            Integer mine = mining.getValue();
            Integer grow = expansion.getValue();
            if (!mining.isInvalid() && mine != null && grow != null) { apply.accept(grow, mine); }
        });
        save.setId(IndustryPanel.APPLY_ID);
        save.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.SMALL);
        validate.run();
        add(mining, expansion, capacity, save);
    }
    private static IntegerField field(String key, String id, int maximum, int value) {
        var field = new IntegerField(I18n.t(key));
        field.setId(id); field.setMin(0); field.setMax(maximum);
        field.setStepButtonsVisible(true); field.setValue(value);
        return field;
    }
}
