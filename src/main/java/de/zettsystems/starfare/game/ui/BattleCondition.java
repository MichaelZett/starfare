package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.report.values.CombatReadiness;

/** Opening condition stays visible; exact combat arithmetic is optional. */
final class BattleCondition {
    private static final String[][] CONDITIONS = {
        {UiTexts.BATTLE_CONDITION_LOW_0, UiTexts.BATTLE_CONDITION_LOW_1, UiTexts.BATTLE_CONDITION_LOW_2},
        {UiTexts.BATTLE_CONDITION_TENSE_0, UiTexts.BATTLE_CONDITION_TENSE_1, UiTexts.BATTLE_CONDITION_TENSE_2},
        {UiTexts.BATTLE_CONDITION_READY_0, UiTexts.BATTLE_CONDITION_READY_1, UiTexts.BATTLE_CONDITION_READY_2},
        {UiTexts.BATTLE_CONDITION_GOOD_0, UiTexts.BATTLE_CONDITION_GOOD_1, UiTexts.BATTLE_CONDITION_GOOD_2},
        {UiTexts.BATTLE_CONDITION_HIGH_0, UiTexts.BATTLE_CONDITION_HIGH_1, UiTexts.BATTLE_CONDITION_HIGH_2}
    };

    private BattleCondition() { }

    static void addTo(Div card, int ships, double strength, int sideId, String formattedStrength) {
        if (ships > 0) {
            CombatReadiness condition = CombatReadiness.from(ships, strength, sideId);
            Span text = new Span(I18n.t(CONDITIONS[condition.level()][condition.variant()]));
            text.addClassName("battle-condition");
            card.add(text);
        }
        double deviation = ships > 0 ? (strength / ships - 1) * 100 : 0;
        String percent = String.format(com.vaadin.flow.component.UI.getCurrentOrThrow().getLocale(), "%+.2f", deviation);
        Span facts = new Span(I18n.t(UiTexts.BATTLE_CONDITION_FACTS, ships, formattedStrength, percent));
        facts.addClassName("battle-strength");
        card.add(new Details(I18n.t(UiTexts.BATTLE_CONDITION_DETAILS), facts));
    }
}
