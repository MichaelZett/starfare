package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.game.values.GameConfig;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.game.values.VictoryRules;
import de.zettsystems.starfare.i18n.I18n;

/** Keeps wizard choices consistent with the selected version and diplomacy availability. */
final class VictorySettings extends VerticalLayout {
    private final Checkbox alliances = new Checkbox(I18n.t(UiTexts.ALLIANCES_ALLOWED));
    private final Checkbox groupVictory = new Checkbox(I18n.t(UiTexts.ALLIANCE_VICTORY_ALLOWED));
    private final IntegerField groupPercent = new IntegerField(I18n.t(UiTexts.ALLIANCE_VICTORY_PERCENT));
    VictorySettings() {
        setPadding(false); alliances.setId("alliances-allowed"); groupVictory.setId("alliance-victory-allowed");
        groupPercent.setId("alliance-victory-percent"); groupPercent.setMin(GameConfig.MIN_VICTORY_SYSTEM_PERCENT);
        groupPercent.setMax(100); groupPercent.setValue(GameConfig.VICTORY_SYSTEM_PERCENT);
        alliances.addValueChangeListener(_ -> refresh()); groupVictory.addValueChangeListener(_ -> refresh());
        add(alliances, groupVictory, groupPercent); select(RulesetRef.SECTOR_FORCES);
    }
    void select(RulesetRef rules) {
        alliances.setEnabled(rules.spaceward()); alliances.setValue(rules.spaceward());
        groupVictory.setValue(false); refresh();
    }
    private void refresh() {
        if (!alliances.getValue()) { groupVictory.setValue(false); }
        groupVictory.setEnabled(alliances.isEnabled() && alliances.getValue());
        groupPercent.setEnabled(groupVictory.isEnabled() && groupVictory.getValue());
    }
    VictoryRules chosen(int individual) {
        Integer percent = groupPercent.getValue();
        return new VictoryRules(individual, alliances.getValue(), groupVictory.getValue(),
                percent == null ? GameConfig.VICTORY_SYSTEM_PERCENT : percent);
    }
}
