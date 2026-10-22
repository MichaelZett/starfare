package de.zettsystems.starfare.research.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.research.values.ShipResearch;

/** Own-empire budget editor, with optimistic concurrency enforced by the application service. */
public final class ResearchDialog {
    private ResearchDialog() { }
    public static void open(GameService game, GameId id, String account) {
        game.shipResearchFor(id, account).ifPresent(view -> {
            var research = view.research();
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle(I18n.t(de.zettsystems.starfare.game.ui.UiTexts.RESEARCH_TITLE));
            dialog.setWidth("min(30rem, 95vw)");
            dialog.add(new Paragraph(I18n.t(de.zettsystems.starfare.game.ui.UiTexts.RESEARCH_STATUS, research.level(), research.level() * 10,
                    research.progress(), research.level() == ShipResearch.MAX_LEVEL ? "—" : ShipResearch.cost(research.level()), view.pointsPerTurn())),
                    new Paragraph(I18n.t(de.zettsystems.starfare.game.ui.UiTexts.RESEARCH_EXPLANATION)));
            IntegerField budget = new IntegerField(I18n.t(de.zettsystems.starfare.game.ui.UiTexts.RESEARCH_BUDGET));
            budget.setId("research-budget"); budget.setMin(0); budget.setMax(100); budget.setStep(5);
            budget.setStepButtonsVisible(true); budget.setValue(research.percent());
            budget.setEnabled(view.editable() && research.level() < ShipResearch.MAX_LEVEL);
            dialog.add(budget);
            Button save = new Button(I18n.t(de.zettsystems.starfare.game.ui.UiTexts.RESEARCH_SAVE), _ -> {
                Integer percent = budget.getValue();
                if (percent != null && game.planShipResearch(id, account, view.turn(), research.percent(), percent)) { dialog.close(); }
                else { Notification.show(I18n.t(de.zettsystems.starfare.game.ui.UiTexts.RESEARCH_REJECTED)); }
            });
            save.setId("research-save"); save.setEnabled(budget.isEnabled());
            dialog.getFooter().add(save, new Button(I18n.t(de.zettsystems.starfare.game.ui.UiTexts.RESEARCH_CLOSE), _ -> dialog.close()));
            dialog.open();
        });
    }
}
