package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.economy.values.*;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.game.values.RelocationAdjustment;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.HtmlAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Inline production editing without a separate apply step: numbers, step buttons and presets
 * save immediately, a slider saves when released. Dragging only previews on a copy.
 */
public final class ProductionEditor extends Div {
    private static final String[] LABELS = {UiTexts.UX_BUILD_SHIPS, UiTexts.UX_MINE_METAL, UiTexts.UX_EXPAND_INDUSTRY};
    public static final String MINING_ID = "industry-mining";
    private static final String[] IDS = {"industry-shipbuilding", MINING_ID, IndustryPanel.ALLOCATION_ID};
    private final ProductionInputs.Input input;
    private final Function<AllocationSplit, Optional<ProductionPlanPreview>> preview;
    private final Function<AllocationSplit, ProductionCommit> commit;
    private final ProductionFunctions functions;
    private final Div editor = new Div();
    private final Div results = new Div();
    private final Div presets = new Div();
    private final Span status = new Span();
    private final List<IntegerField> amounts = new ArrayList<>();
    private final List<Input> sliders = new ArrayList<>();
    private final List<Checkbox> locks = new ArrayList<>();
    private boolean updating;

    public ProductionEditor(ProductionInputs.Input input,
                            Function<AllocationSplit, Optional<ProductionPlanPreview>> preview,
                            Function<AllocationSplit, ProductionCommit> commit) {
        this.input = input; this.preview = preview; this.commit = commit;
        addClassName("production-editor");
        editor.addClassName("production-inline-fields");
        presets.addClassName("production-presets");
        functions = new ProductionFunctions(ProductionOutcome.of(input.allocation(), input.industry()),
                input.industry().metal() != null, this::focus);
        add(functions, presets);
        Button open = new Button(I18n.t(UiTexts.UX_PLAN_PRODUCTION), _ -> focus(0));
        open.setId("production-plan-open");
        add(open, editor);
        editor.add(new Span(I18n.t(UiTexts.UX_CAPACITY_BUDGET, input.industry().usableCapacity())));
        for (int index = 0; index < 3; index++) { addRow(index); }
        results.addClassName("production-plan-results");
        status.addClassName("production-save-status");
        status.getElement().setAttribute(HtmlAttributes.ARIA_LIVE, "polite");
        editor.add(results);
        add(status);
        sync();
        restoreFocus();
    }

    public void focus(int index) {
        input.expand(); editor.setVisible(true);
        amounts.get(index).focus();
        editor.getElement().executeJs("this.scrollIntoView({block:'nearest'})");
    }

    private void restoreFocus() {
        Integer index = input.focus();
        if (index == null || !input.expanded()) { return; }
        if (index < 3) { amounts.get(index).focus(); }
        else { sliders.get(index - 3).getElement().callJsFunction("focus"); }
    }

    /** Saves the complete current allocation once; unchanged or refused input shows the stored plan. */
    private void save() {
        if (!input.changed()) { sync(); return; }
        if (!input.valid()) { input.rejected(); sync(); return; }
        ProductionCommit result = commit.apply(input.allocation());
        if (result.accepted()) { input.accepted(result.relocations()); } else { input.rejected(); }
        sync();
    }

    private void addRow(int index) {
        IntegerField amount = new IntegerField();
        amount.setId(IDS[index]); amount.setMin(0); amount.setStepButtonsVisible(true);
        amount.setAriaLabel(I18n.t(LABELS[index]));
        Input slider = new Input(); slider.setType("range");
        slider.getElement().setAttribute(HtmlAttributes.MIN, "0");
        slider.getElement().setAttribute(HtmlAttributes.STEP, "1");
        slider.getElement().setAttribute(HtmlAttributes.ARIA_LABEL, I18n.t(LABELS[index]));
        Checkbox pin = new Checkbox(I18n.t(UiTexts.UX_PIN), input.locks().contains(index));
        pin.setId(IDS[index] + "-pin");
        pin.addValueChangeListener(event -> { if (!updating) { input.pin(index, event.getValue()); sync(); } });
        amount.addValueChangeListener(event -> {
            if (!updating && event.isFromClient() && event.getValue() != null) { input.adjust(index, event.getValue()); save(); }
        });
        slider.getElement().addEventListener("input", event -> {
            if (!updating) { input.adjust(index, event.getEventData().path("element.value").asInt()); sync(); }
        }).addEventData("element.value");
        slider.getElement().addEventListener("change", event -> {
            if (!updating) { input.adjust(index, event.getEventData().path("element.value").asInt()); save(); }
        }).addEventData("element.value");
        amount.getElement().addEventListener("focusin", _ -> input.focus(index));
        amount.getElement().addEventListener("focusout", _ -> input.focus(null));
        slider.getElement().addEventListener("focus", _ -> input.focus(index + 3));
        slider.getElement().addEventListener("blur", _ -> input.focus(null));
        amounts.add(amount); sliders.add(slider); locks.add(pin);
        Div row = new Div(new Span(I18n.t(LABELS[index])), amount, slider, pin);
        row.addClassName("production-allocation-row");
        row.setVisible(index != 1 || input.industry().metal() != null);
        editor.add(row);
    }

    private void sync() {
        updating = true;
        int[] limits = ProductionPreset.limits(input.industry());
        for (int index = 0; index < 3; index++) {
            amounts.get(index).setMax(limits[index]); amounts.get(index).setValue(input.allocation().at(index));
            sliders.get(index).getElement().setAttribute(HtmlAttributes.MAX, String.valueOf(limits[index]));
            sliders.get(index).setValue(String.valueOf(input.allocation().at(index)));
            boolean enabled = !input.locks().contains(index) && input.locks().size() < 2 && limits[index] > 0;
            amounts.get(index).setEnabled(enabled); sliders.get(index).setEnabled(enabled);
        }
        updating = false;
        editor.setVisible(input.expanded());
        renderPresets(); renderResults(); renderStatus();
    }

    private void renderStatus() {
        status.removeAll();
        switch (input.status()) {
            case SAVED -> {
                status.add(new Span(I18n.t(ProductionKeys.SAVED)));
                input.relocations().forEach(adjustment -> status.add(new Span(relocation(adjustment))));
            }
            case NOT_SAVED -> {
                Span warning = new Span(I18n.t(ProductionKeys.NOT_SAVED)); warning.addClassName("production-warning");
                status.add(warning);
            }
            case NONE -> { }
        }
        status.setVisible(input.status() != ProductionInputs.Status.NONE);
    }

    static String relocation(RelocationAdjustment adjustment) {
        return adjustment.removed()
                ? I18n.t(ProductionKeys.RELOCATION_REMOVED, adjustment.toSystem(), adjustment.before())
                : I18n.t(ProductionKeys.RELOCATION_SHORTENED, adjustment.toSystem(), adjustment.before(), adjustment.after());
    }

    private void renderPresets() {
        presets.removeAll();
        for (var preset : ProductionPreset.values()) {
            if (input.industry().metal() == null && (preset == ProductionPreset.METAL || preset == ProductionPreset.AUTARK)) { continue; }
            var result = preset.apply(input.industry(), input.allocation(), input.locks());
            Button button = new Button(I18n.t(ProductionKeys.preset(preset)), _ -> {
                input.choose(preset.apply(input.industry(), input.allocation(), input.locks()).allocation());
                save();
            });
            button.setId("production-preset-" + preset.name().toLowerCase(java.util.Locale.ROOT));
            button.setEnabled(result.applicable());
            Div item = new Div(button);
            if (!result.applicable()) { item.add(new Span(I18n.t(ProductionKeys.rejection(result.rejection())))); }
            presets.add(item);
        }
        for (int index : input.locks()) { presets.add(new Span(I18n.t(ProductionKeys.PINNED, I18n.t(LABELS[index]), input.allocation().at(index)))); }
    }

    private void renderResults() {
        results.removeAll();
        // The shown industry can lag behind a just accepted save until the server update arrives.
        boolean shown = input.allocation().equals(ProductionPreset.saved(input.industry()));
        Optional<ProductionPlanPreview> forecast = shown || !input.valid() ? Optional.empty() : preview.apply(input.allocation());
        if (!shown && forecast.isEmpty()) {
            results.add(new Span(I18n.t(ProductionKeys.INVALID)));
            functions.show(ProductionOutcome.of(input.saved(), input.industry()), input.industry().metal() != null);
            return;
        }
        IndustryView after = forecast.map(ProductionPlanPreview::industry).orElse(input.industry());
        functions.show(ProductionOutcome.of(input.allocation(), after), after.metal() != null);
        results.add(new Div(new Span(I18n.t(UiTexts.UX_DELIVERY_RESULT, after.nextDelivery()))));
        if (after.deliveryBottleneck()) { warn(UiTexts.UX_DELIVERY_SHORTAGE); }
        forecast.ifPresent(plan -> {
            plan.relocations().forEach(adjustment -> warn(relocation(adjustment), false));
            var balance = plan.empire().metal();
            if (balance != null) { results.add(new Div(new Span(I18n.t(UiTexts.UX_METAL_BALANCE,
                    balance.extraction(), balance.recycling(), balance.consumption(), balance.next())))); }
        });
    }
    private void warn(String key) { warn(I18n.t(key), true); }
    private void warn(String text, boolean warning) {
        Span label = new Span(text); label.addClassName(warning ? "production-warning" : "production-relocation"); results.add(label);
    }
}
