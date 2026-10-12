package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.button.Button;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.i18n.I18n;

/** Deliberate abandonment only; this never commits drafts as a side effect. */
public final class ProductionNavigation {
    private ProductionNavigation() { }
    public static void submit(int count, Runnable savedPlans, Runnable inspect, Runnable cancel) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(I18n.t(ProductionKeys.SUBMIT, count));
        dialog.setText(I18n.t(ProductionKeys.LOCAL));
        dialog.setCancelable(true); dialog.setCancelText(I18n.t(UiTexts.UX_KEEP_EDITING));
        dialog.setConfirmText(I18n.t(ProductionKeys.SUBMIT_SAVED));
        dialog.setId("production-submit-warning");
        dialog.addConfirmListener(_ -> savedPlans.run());
        dialog.addCancelListener(_ -> cancel.run());
        dialog.add(new Button(I18n.t(ProductionKeys.SEE_DRAFTS), _ -> { dialog.close(); inspect.run(); cancel.run(); }));
        dialog.open();
    }
    public static void leave(Runnable discardAndLeave, Runnable stay) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(I18n.t(UiTexts.UX_UNSAVED)); dialog.setText(I18n.t(ProductionKeys.LEAVE));
        dialog.setCancelable(true); dialog.setCancelText(I18n.t(UiTexts.UX_KEEP_EDITING));
        dialog.setConfirmText(I18n.t(UiTexts.UX_DISCARD));
        dialog.addConfirmListener(_ -> discardAndLeave.run()); dialog.addCancelListener(_ -> stay.run()); dialog.open();
    }
}
