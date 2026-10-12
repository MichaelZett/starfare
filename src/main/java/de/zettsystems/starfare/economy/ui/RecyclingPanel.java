package de.zettsystems.starfare.economy.ui;

import de.zettsystems.starfare.economy.values.RecyclingRules;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.economy.values.RecyclingView;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.i18n.I18n;
import java.util.function.IntConsumer;

/** A round order, with zero as cancellation and no immediate resource credit. */
public final class RecyclingPanel extends Div {
    private static final long serialVersionUID = 1L;
    public static final String SHIPS_ID = "recycling-ships";
    public static final String APPLY_ID = "recycling-apply";
    public RecyclingPanel(RecyclingView view, boolean editable, IntConsumer apply) {
        addClassName("recycling-details");
        if (view.ships() > 0) {
            add(new Div(new Span(I18n.t(UiTexts.RECYCLING_PLAN, view.ships(),
                    (double) view.ships() * RecyclingRules.RETURN_TENTHS / RecyclingRules.METAL_SCALE))));
            if (editable) {
                Button change = new Button(I18n.t(UiTexts.UX_CHANGE), _ -> openEditor(view, apply));
                change.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
                Button cancel = new Button(I18n.t(UiTexts.UX_CANCEL_RECYCLING), _ -> apply.accept(0));
                cancel.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
                add(change, cancel);
            }
        }
        var details = new com.vaadin.flow.component.html.Div(new Span(I18n.t(UiTexts.RECYCLING_FRACTION,
                (double) view.fraction() / RecyclingRules.METAL_SCALE)));
        var actions = new com.vaadin.flow.component.details.Details(I18n.t(UiTexts.UX_MORE_ACTIONS), details);
        add(actions);
        if (!editable) { return; }
        Button open = new Button(I18n.t(UiTexts.RECYCLING_SHIPS), _ -> openEditor(view, apply));
        open.setId("recycling-open");
        open.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
        details.add(open);
    }

    private static void openEditor(RecyclingView view, IntConsumer apply) {
        var dialog = new com.vaadin.flow.component.dialog.Dialog();
        dialog.setHeaderTitle(I18n.t(UiTexts.RECYCLING_SHIPS));
        dialog.setWidth("min(480px, 94vw)");
        Span result = new Span();
        var amount = new IntegerField(I18n.t(UiTexts.RECYCLING_SHIPS));
        amount.setId(SHIPS_ID); amount.setMin(0); amount.setMax(view.available());
        amount.setStepButtonsVisible(true); amount.setValue(view.ships());
        Button save = new Button(I18n.t(UiTexts.RECYCLING_APPLY), _ -> {
            Integer ships = amount.getValue();
            if (ships != null && ships >= 0 && ships <= view.available()) { apply.accept(ships); dialog.close(); }
        });
        save.setId(APPLY_ID); save.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.PRIMARY);
        Runnable preview = () -> {
            Integer selected = amount.getValue();
            int ships = selected == null ? 0 : selected;
            result.setText(I18n.t(UiTexts.RECYCLING_PLAN, ships,
                    (double) ships * RecyclingRules.RETURN_TENTHS / RecyclingRules.METAL_SCALE));
        };
        amount.addValueChangeListener(_ -> preview.run());
        preview.run();
        dialog.add(amount, result, new Span(I18n.t(UiTexts.RECYCLING_HINT)),
                new com.vaadin.flow.component.details.Details(I18n.t(UiTexts.UX_BACKGROUND),
                        new Span(I18n.t(UiTexts.RECYCLING_FRACTION, (double) view.fraction() / RecyclingRules.METAL_SCALE))));
        dialog.getFooter().add(new Button(I18n.t(UiTexts.MAP_DIALOG_CANCEL), _ -> dialog.close()), save);
        dialog.open();
    }
}
