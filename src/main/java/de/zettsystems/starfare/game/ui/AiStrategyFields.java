package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.game.values.GameConfig;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.i18n.I18n;
import java.util.ArrayList;
import java.util.List;

/** Keeps each AI seat's choice when the player count changes. */
final class AiStrategyFields extends FormLayout {
    private static final String FIELD_ID_PREFIX = "ai-strategy-";
    private final List<AiStrategy> selected = new ArrayList<>();
    private RulesetRef ruleset = RulesetRef.SECTOR_FORCES;
    private int count;

    AiStrategyFields() {
        setWidthFull();
        setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("32em", 2), new ResponsiveStep("55em", 3));
        for (int i = 0; i < GameConfig.MAX_AI_PLAYERS; i++) { selected.add(AiStrategy.BASELINE); }
    }

    void selectRuleset(RulesetRef rules) {
        ruleset = rules;
        selected.replaceAll(strategy -> strategy.supports(rules) ? strategy : AiStrategy.BASELINE);
        rebuild();
    }

    void showSeats(int seats) {
        count = Math.clamp(seats, 0, GameConfig.MAX_AI_PLAYERS);
        rebuild();
    }

    List<AiStrategy> strategies() { return List.copyOf(selected.subList(0, count)); }

    private void rebuild() {
        removeAll();
        setVisible(count > 0);
        for (int index = 0; index < count; index++) { addSeat(index); }
    }

    private void addSeat(int index) {
        ComboBox<AiStrategy> field = new ComboBox<>(I18n.t(UiTexts.AI_STRATEGY_SEAT, index + 1));
        field.setId(FIELD_ID_PREFIX + (index + 1));
        field.setItems(AiStrategy.forRules(ruleset));
        field.setItemLabelGenerator(AiStrategyFields::label);
        field.setValue(selected.get(index));
        field.setHelperText(I18n.t(UiTexts.AI_STRATEGY_HINT));
        field.addValueChangeListener(event -> {
            AiStrategy value = event.getValue();
            selected.set(index, value == null ? AiStrategy.BASELINE : value);
        });
        add(field);
    }

    static String label(AiStrategy strategy) {
        return I18n.t(switch (strategy) {
            case BASELINE -> UiTexts.AI_STRATEGY_BASELINE;
            case RUSH -> UiTexts.AI_STRATEGY_RUSH;
            case EXPANSION -> UiTexts.AI_STRATEGY_EXPANSION;
            case CONCENTRATION -> UiTexts.AI_STRATEGY_CONCENTRATION;
            case DEFENSE -> UiTexts.AI_STRATEGY_DEFENSE;
            case INDUSTRY -> UiTexts.AI_STRATEGY_INDUSTRY;
            case INDUSTRY_LIGHT -> UiTexts.AI_STRATEGY_INDUSTRY_LIGHT;
            case INDUSTRY_ADAPTIVE -> UiTexts.AI_STRATEGY_INDUSTRY_ADAPTIVE;
        });
    }
}
