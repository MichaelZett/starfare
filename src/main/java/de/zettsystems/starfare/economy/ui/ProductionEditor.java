package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.economy.values.AllocationSplit;
import de.zettsystems.starfare.economy.values.IndustryView;
import de.zettsystems.starfare.economy.values.ProductionCommit;
import de.zettsystems.starfare.economy.values.ProductionInputs;
import de.zettsystems.starfare.economy.values.ProductionPlanPreview;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.game.values.RelocationAdjustment;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.HtmlAttributes;
import java.util.Optional;
import java.util.function.Function;

/**
 * The expansion share of a system's capacity, without a separate apply step: the number and its step
 * buttons save immediately, the slider saves when released and only previews while dragged.
 * Shipbuilding always receives the rest of the capacity.
 */
public final class ProductionEditor extends Div {
    private static final String VALUE = "element.value";
    private static final int FIELD = 0;
    private static final int SLIDER = 1;
    private final ProductionInputs.Input input;
    private final Function<AllocationSplit, Optional<ProductionPlanPreview>> preview;
    private final Function<AllocationSplit, ProductionCommit> commit;
    private final IntegerField amount = new IntegerField();
    private final Input slider = new Input();
    private final Span result = new Span();
    private final Div notes = new Div();
    private final Span status = new Span();
    private boolean updating;

    public ProductionEditor(ProductionInputs.Input input,
                            Function<AllocationSplit, Optional<ProductionPlanPreview>> preview,
                            Function<AllocationSplit, ProductionCommit> commit) {
        this.input = input; this.preview = preview; this.commit = commit;
        addClassName("production-editor");
        String label = I18n.t(ProductionKeys.EXPANSION);
        amount.setId(IndustryPanel.ALLOCATION_ID); amount.setMin(0); amount.setStepButtonsVisible(true);
        amount.setAriaLabel(label);
        slider.setType("range");
        slider.getElement().setAttribute(HtmlAttributes.MIN, "0");
        slider.getElement().setAttribute(HtmlAttributes.STEP, "1");
        slider.getElement().setAttribute(HtmlAttributes.ARIA_LABEL, label);
        listen();
        Div row = new Div(new Span(label), amount, slider);
        row.addClassName("production-allocation-row");
        result.addClassName(IndustryPanel.RESULT_CLASS);
        notes.addClassName("production-notes");
        status.addClassName("production-save-status");
        status.getElement().setAttribute(HtmlAttributes.ARIA_LIVE, "polite");
        add(row, result, notes, status);
        sync();
        restoreFocus();
    }

    private void listen() {
        amount.addValueChangeListener(event -> {
            if (!updating && event.isFromClient() && event.getValue() != null) { input.expansion(event.getValue()); save(); }
        });
        slider.getElement().addEventListener("input", event -> {
            if (!updating) { input.expansion(event.getEventData().path(VALUE).asInt()); sync(); }
        }).addEventData(VALUE);
        slider.getElement().addEventListener("change", event -> {
            if (!updating) { input.expansion(event.getEventData().path(VALUE).asInt()); save(); }
        }).addEventData(VALUE);
        amount.getElement().addEventListener("focusin", _ -> input.focus(FIELD));
        amount.getElement().addEventListener("focusout", _ -> input.focus(null));
        slider.getElement().addEventListener("focus", _ -> input.focus(SLIDER));
        slider.getElement().addEventListener("blur", _ -> input.focus(null));
    }

    private void restoreFocus() {
        Integer control = input.focus();
        if (control == null) { return; }
        if (control == FIELD) { amount.focus(); }
        else { slider.getElement().callJsFunction("focus"); }
    }

    /** Saves the complete current split once; unchanged or refused input shows the stored plan. */
    private void save() {
        if (!input.changed()) { sync(); return; }
        if (!input.valid()) { input.rejected(); sync(); return; }
        ProductionCommit saved = commit.apply(input.allocation());
        if (saved.accepted()) { input.accepted(saved.relocations()); } else { input.rejected(); }
        sync();
    }

    private void sync() {
        updating = true;
        // At the maximum capacity nothing can be expanded, so the split is fixed.
        int limit = AllocationSplit.limits(input.industry())[AllocationSplit.EXPANSION];
        int value = input.allocation().expansion();
        amount.setMax(limit); amount.setValue(value);
        slider.getElement().setAttribute(HtmlAttributes.MAX, String.valueOf(limit));
        slider.setValue(String.valueOf(value));
        amount.setEnabled(limit > 0); slider.setEnabled(limit > 0);
        updating = false;
        renderResult(); renderStatus();
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
            case NONE -> { /* nothing to report; the status line is hidden below */ }
        }
        status.setVisible(input.status() != ProductionInputs.Status.NONE);
    }

    static String relocation(RelocationAdjustment adjustment) {
        return adjustment.removed()
                ? I18n.t(ProductionKeys.RELOCATION_REMOVED, adjustment.toSystem(), adjustment.before())
                : I18n.t(ProductionKeys.RELOCATION_SHORTENED, adjustment.toSystem(), adjustment.before(), adjustment.after());
    }

    private void renderResult() {
        notes.removeAll();
        // The shown industry can lag behind a just accepted save until the server update arrives.
        boolean shown = input.allocation().equals(AllocationSplit.saved(input.industry()));
        Optional<ProductionPlanPreview> forecast = shown || !input.valid() ? Optional.empty() : preview.apply(input.allocation());
        if (!shown && forecast.isEmpty()) {
            result.setText(IndustryPanel.result(input.industry()));
            note(I18n.t(ProductionKeys.INVALID), true);
        } else {
            IndustryView after = forecast.map(ProductionPlanPreview::industry).orElse(input.industry());
            result.setText(IndustryPanel.result(after));
            if (after.deliveryBottleneck()) { note(I18n.t(UiTexts.UX_DELIVERY_SHORTAGE), true); }
            forecast.ifPresent(plan -> plan.relocations().forEach(adjustment -> note(relocation(adjustment), false)));
        }
        notes.setVisible(notes.getChildren().findAny().isPresent());
    }

    private void note(String text, boolean warning) {
        Span label = new Span(text); label.addClassName(warning ? "production-warning" : "production-relocation"); notes.add(label);
    }
}
