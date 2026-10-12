package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.economy.values.*;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.HtmlAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/** Inline editing reuses authorized forecasting; drafts belong to the map workspace. */
public final class ProductionEditor extends Div {
    private static final String[] LABELS = {UiTexts.UX_BUILD_SHIPS, UiTexts.UX_MINE_METAL, UiTexts.UX_EXPAND_INDUSTRY};
    private static final String[] IDS = {"industry-shipbuilding", MetalEconomyPanel.MINING_ID, IndustryPanel.ALLOCATION_ID};
    private final ProductionDrafts.Draft draft;
    private final int turn;
    private final Function<AllocationSplit, Optional<ProductionPlanPreview>> preview;
    private final Predicate<AllocationSplit> commit;
    private final Runnable changed;
    private final Div editor = new Div();
    private final Div results = new Div();
    private final Div presets = new Div();
    private final Span state = new Span();
    private final Button save = new Button(I18n.t(UiTexts.ECONOMY_APPLY));
    private final Button recheck = new Button(I18n.t(ProductionKeys.RECHECK));
    private final List<IntegerField> amounts = new ArrayList<>();
    private final List<Input> sliders = new ArrayList<>();
    private final List<Checkbox> locks = new ArrayList<>();
    private boolean updating;

    public ProductionEditor(ProductionDrafts.Draft draft, int turn,
                            Function<AllocationSplit, Optional<ProductionPlanPreview>> preview,
                            Predicate<AllocationSplit> commit, Runnable changed) {
        this.draft = draft; this.turn = turn; this.preview = preview; this.commit = commit; this.changed = changed;
        addClassName("production-editor");
        editor.addClassName("production-inline-fields");
        presets.addClassName("production-presets");
        add(new ProductionFunctions(draft.industry(), this::focus));
        add(presets);
        Button open = new Button(I18n.t(UiTexts.UX_PLAN_PRODUCTION), _ -> focus(0));
        open.setId("production-plan-open");
        add(open, editor);
        for (int index = 0; index < 3; index++) { addRow(index); }
        results.addClassName("production-plan-results");
        results.getElement().setAttribute(HtmlAttributes.ARIA_LIVE, "polite");
        recheck.addClickListener(_ -> { draft.recheck(turn); sync(); changed.run(); });
        save.setId(IndustryPanel.APPLY_ID); save.addThemeVariants(ButtonVariant.PRIMARY);
        save.addClickListener(_ -> apply());
        Button discard = new Button(I18n.t(UiTexts.UX_DISCARD), _ -> { draft.discard(); draft.collapse(); sync(); changed.run(); });
        Button reset = new Button(I18n.t(UiTexts.UX_RESET), _ -> { draft.discard(); sync(); changed.run(); });
        editor.add(new Span(I18n.t(UiTexts.UX_CAPACITY_BUDGET, draft.industry().usableCapacity())));
        editor.add(state, results, new Div(reset, discard, recheck, save), new Span(I18n.t(ProductionKeys.LOCAL)));
        sync();
    }

    public void focus(int index) {
        draft.expand(); editor.setVisible(true);
        amounts.get(index).focus();
        editor.getElement().executeJs("this.scrollIntoView({block:'nearest'})");
    }

    private void apply() {
        if (draft.stale() || !draft.valid() || preview.apply(draft.allocation()).isEmpty()) { return; }
        if (commit.test(draft.allocation())) { draft.accepted(); }
        else { draft.observe(turn + 1, draft.industry()); }
        sync(); changed.run();
    }

    private void addRow(int index) {
        IntegerField amount = new IntegerField();
        amount.setId(IDS[index]); amount.setMin(0); amount.setStepButtonsVisible(true);
        amount.setAriaLabel(I18n.t(LABELS[index]));
        Input slider = new Input(); slider.setType("range");
        slider.getElement().setAttribute(HtmlAttributes.MIN, "0");
        slider.getElement().setAttribute(HtmlAttributes.STEP, "1");
        slider.getElement().setAttribute(HtmlAttributes.ARIA_LABEL, I18n.t(LABELS[index]));
        Checkbox pin = new Checkbox(I18n.t(UiTexts.UX_PIN), draft.locks().contains(index));
        pin.setId(IDS[index] + "-pin");
        pin.addValueChangeListener(event -> { if (!updating) { draft.pin(index, event.getValue()); sync(); changed.run(); } });
        amount.addValueChangeListener(event -> {
            if (!updating && event.getValue() != null) { adjust(index, event.getValue()); }
        });
        slider.getElement().addEventListener("input", event -> {
            if (!updating) { adjust(index, event.getEventData().path("element.value").asInt()); }
        }).addEventData("element.value");
        amounts.add(amount); sliders.add(slider); locks.add(pin);
        Div row = new Div(new Span(I18n.t(LABELS[index])), amount, slider, pin);
        row.addClassName("production-allocation-row");
        row.setVisible(index != 1 || draft.industry().metal() != null);
        editor.add(row);
    }
    private void adjust(int index, int value) { draft.adjust(index, value); sync(); changed.run(); }

    private void sync() {
        updating = true;
        int[] limits = ProductionPreset.limits(draft.industry());
        for (int index = 0; index < 3; index++) {
            amounts.get(index).setMax(limits[index]); amounts.get(index).setValue(draft.allocation().at(index));
            sliders.get(index).getElement().setAttribute(HtmlAttributes.MAX, String.valueOf(limits[index]));
            sliders.get(index).setValue(String.valueOf(draft.allocation().at(index)));
            boolean enabled = !draft.locks().contains(index) && draft.locks().size() < 2 && limits[index] > 0;
            amounts.get(index).setEnabled(enabled); sliders.get(index).setEnabled(enabled);
        }
        updating = false;
        editor.setVisible(draft.expanded());
        String key = UiTexts.UX_SAVED;
        if (draft.dirty()) { key = UiTexts.UX_UNSAVED; }
        if (draft.stale()) { key = ProductionKeys.STALE; }
        state.setText(I18n.t(key));
        recheck.setVisible(draft.stale());
        renderPresets(); renderResults();
    }

    private void renderPresets() {
        presets.removeAll();
        for (var preset : ProductionPreset.values()) {
            if (draft.industry().metal() == null && (preset == ProductionPreset.METAL || preset == ProductionPreset.AUTARK)) { continue; }
            var result = preset.apply(draft.industry(), draft.allocation(), draft.locks());
            Button button = new Button(I18n.t(ProductionKeys.preset(preset)), _ -> {
                draft.choose(preset.apply(draft.industry(), draft.allocation(), draft.locks()).allocation());
                sync(); changed.run();
            });
            button.setId("production-preset-" + preset.name().toLowerCase(java.util.Locale.ROOT));
            button.setEnabled(result.applicable());
            Div item = new Div(button);
            if (!result.applicable()) { item.add(new Span(I18n.t(ProductionKeys.rejection(result.rejection())))); }
            presets.add(item);
        }
        for (int index : draft.locks()) { presets.add(new Span(I18n.t(ProductionKeys.PINNED, I18n.t(LABELS[index]), draft.allocation().at(index)))); }
    }

    private void renderResults() {
        results.removeAll();
        var forecast = draft.valid() ? preview.apply(draft.allocation()) : Optional.<ProductionPlanPreview>empty();
        save.setEnabled(draft.dirty() && !draft.stale() && forecast.isPresent());
        if (forecast.isEmpty()) { results.add(new Span(I18n.t(ProductionKeys.INVALID))); return; }
        IndustryView before = draft.industry();
        IndustryView after = forecast.orElseThrow().industry();
        var beforeMetal = before.metal();
        var afterMetal = after.metal();
        long beforeMining = beforeMetal == null ? 0 : beforeMetal.extraction();
        long afterMining = afterMetal == null ? 0 : afterMetal.extraction();
        results.add(new Span(I18n.t(ProductionKeys.BEFORE_AFTER, before.shipbuilding(), after.shipbuilding(),
                beforeMining, afterMining, before.expansionAllocation(), after.expansionAllocation())),
                new Div(new Span(I18n.t(UiTexts.UX_SHIP_RESULT, draft.allocation().ships(), after.shipbuilding()))),
                new Div(new Span(I18n.t(UiTexts.UX_DELIVERY_RESULT, after.nextDelivery()))));
        results.add(new Div(new Span(I18n.t(UiTexts.UX_EXPANSION_RESULT, after.expansionAllocation(),
                Math.min(after.expansionCost(), after.expansionProgress() + after.expansionAllocation()), after.expansionCost()))));
        if (afterMetal != null) { results.add(new Div(new Span(I18n.t(UiTexts.UX_EXTRACTION_RESULT, afterMetal.extraction())))); }
        if (afterMetal != null && afterMetal.actualShips() < afterMetal.plannedShips()) { warn(UiTexts.UX_METAL_SHORTAGE); }
        if (afterMetal != null && afterMetal.remaining() == 0 && draft.allocation().mining() > 0) { warn(UiTexts.UX_DEPOSIT_EMPTY); }
        if (after.deliveryBottleneck()) { warn(UiTexts.UX_DELIVERY_SHORTAGE); }
        if (after.shipbuilding() == 0) {
            String reason = UiTexts.UX_METAL_SHORTAGE;
            if (draft.allocation().total() == 0) { reason = UiTexts.UX_NO_CAPACITY; }
            else if (draft.allocation().ships() == 0) { reason = UiTexts.UX_NO_SHIP_ALLOCATION; }
            warn(reason);
        }
        var balance = forecast.orElseThrow().empire().metal();
        if (balance != null) { results.add(new Div(new Span(I18n.t(UiTexts.UX_METAL_BALANCE,
                balance.extraction(), balance.recycling(), balance.consumption(), balance.next())))); }
    }
    private void warn(String key) { Span label = new Span(I18n.t(key)); label.addClassName("production-warning"); results.add(label); }
}
