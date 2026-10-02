package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
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
    private record PreviewControls(Span ships, Span expansion, Span distribution, Span delivery, Span estimate) { }
    public IndustryPanel(IndustryView industry, int available, int outgoing, boolean editable, IntConsumer apply) {
        this(industry, available, outgoing, outgoing, editable, apply);
    }
    public IndustryPanel(IndustryView industry, int available, int outgoing, int reachable, boolean editable, IntConsumer apply) {
        addClassName("industry-panel");
        add(new H3(I18n.t(UiTexts.ECONOMY_TITLE)),
                new Span(I18n.t(UiTexts.ECONOMY_CAPACITY, industry.capacity(), EconomyRules.MAX_CAPACITY)));
        Div bar = new Div();
        bar.addClassName("industry-allocation-bar");
        Span ships = new Span(); ships.addClassName("industry-shipbuilding");
        Span expansion = new Span(); expansion.addClassName("industry-expansion");
        bar.add(ships, expansion);
        Span distribution = new Span(); distribution.addClassName("industry-distribution");
        Span delivery = new Span(); delivery.addClassName("industry-delivery");
        Span estimate = new Span(); estimate.addClassName("industry-estimate");
        add(bar, distribution, expansionProgress(industry), estimate, delivery);
        PreviewControls controls = new PreviewControls(ships, expansion, distribution, delivery, estimate);
        IntConsumer preview = points -> preview(industry, available, outgoing, reachable, points, controls);
        preview.accept(industry.expansionAllocation());
        if (editable && !industry.atMaximum()) { addEditor(industry, apply, preview); }
        Span hint = new Span(I18n.t(UiTexts.ECONOMY_NEXT_ROUND));
        hint.addClassName("industry-hint"); add(hint);
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

    private void addEditor(IndustryView industry, IntConsumer apply, IntConsumer preview) {
        IntegerField allocation = new IntegerField(I18n.t(UiTexts.ECONOMY_ALLOCATION));
        allocation.setId(ALLOCATION_ID);
        allocation.setMin(0); allocation.setMax(industry.capacity()); allocation.setStepButtonsVisible(true);
        allocation.setValue(industry.expansionAllocation());
        allocation.addValueChangeListener(event -> {
            Integer value = event.getValue();
            if (value != null && value >= 0 && value <= industry.capacity()) { preview.accept(value); }
        });
        Div quick = new Div(); quick.addClassName("industry-quick-actions");
        quick.add(choice(UiTexts.ECONOMY_ALL_SHIPS, allocation, 0),
                choice(UiTexts.ECONOMY_HALF, allocation, industry.capacity() / 2),
                choice(UiTexts.ECONOMY_ALL_EXPANSION, allocation, industry.capacity()));
        Button save = new Button(I18n.t(UiTexts.ECONOMY_APPLY), _ -> {
            Integer value = allocation.getValue();
            if (value != null && value >= 0 && value <= industry.capacity()) { apply.accept(value); }
        });
        save.setId(APPLY_ID); save.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.SMALL);
        add(allocation, quick, save);
    }

    private static Button choice(String key, IntegerField field, int points) {
        Button button = new Button(I18n.t(key), _ -> field.setValue(points));
        button.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY); return button;
    }

    private static void preview(IndustryView industry, int available, int outgoing, int reachable, int points,
                                PreviewControls controls) {
        Span ships = controls.ships();
        Span expansion = controls.expansion();
        Span distribution = controls.distribution();
        Span delivery = controls.delivery();
        Span estimate = controls.estimate();
        int output = industry.capacity() - points;
        ships.getStyle().set(CssProperties.WIDTH, (100.0 * output / industry.capacity()) + "%");
        expansion.getStyle().set(CssProperties.WIDTH, (100.0 * points / industry.capacity()) + "%");
        distribution.setText(I18n.t(UiTexts.ECONOMY_DISTRIBUTION, output, points));
        int delivered = Math.min(reachable, Math.max(0, available + output - industry.reserveShortfall()));
        delivery.setText(I18n.t(UiTexts.ECONOMY_DELIVERY, delivered, outgoing));
        delivery.setClassName("industry-delivery" + (delivered < outgoing ? " industry-bottleneck" : ""));
        int turns = points == 0 || industry.atMaximum() ? 0 : Math.ceilDiv(industry.expansionCost() - industry.expansionProgress(), points);
        estimate.setText(I18n.t(turns == 0 ? UiTexts.ECONOMY_PAUSED : UiTexts.ECONOMY_ESTIMATE, turns));
        distribution.getElement().setAttribute(HtmlAttributes.ARIA_LIVE, "polite");
    }
}
