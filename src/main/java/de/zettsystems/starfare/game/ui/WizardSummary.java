package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.i18n.I18n;
import java.util.function.Consumer;

/** Groups the final choices by the question a player is checking. */
final class WizardSummary extends Div {
    WizardSummary(GameSetup setup, String name, RulesetCatalog catalog, Consumer<String> edit) {
        addClassName("wizard-confirmation-content");
        add(new H3(name), new Span(RulesetLabels.label(catalog, setup.ruleset())));
        Div groups = new Div();
        groups.addClassName("wizard-summary-groups");
        groups.add(group(UiTexts.UX_GALAXY, edit,
                line(UiTexts.LOBBY_SUMMARY_SYSTEMS, setup.systemCount()),
                line(UiTexts.LOBBY_SUMMARY_GALAXY, I18n.t(setup.galaxyLayout() == GalaxyLayout.EVEN
                        ? UiTexts.LOBBY_GALAXY_LAYOUT_EVEN : UiTexts.LOBBY_GALAXY_LAYOUT_RANDOM)),
                line(UiTexts.LOBBY_SUMMARY_GARRISON, setup.startGarrison()),
                line(UiTexts.LOBBY_SUMMARY_NEUTRAL_PRODUCTION, setup.neutralMinProduction(), setup.neutralMaxProduction()),
                line(UiTexts.UX_NEUTRAL_DISTRIBUTION, I18n.t(setup.productionDistribution() == ProductionDistribution.UNIFORM
                        ? UiTexts.LOBBY_PRODUCTION_DISTRIBUTION_UNIFORM : UiTexts.LOBBY_PRODUCTION_DISTRIBUTION_GAUSSIAN))));
        Div participants = group(UiTexts.UX_PARTICIPANTS, edit,
                line(UiTexts.LOBBY_SUMMARY_PLAYERS, setup.humanPlayers(), setup.aiPlayers()));
        for (int index = 0; index < setup.totalPlayers(); index++) { participants.add(seat(setup, index)); }
        groups.add(participants, victory(setup, edit), access(setup, edit));
        add(groups);
    }

    private static Div seat(GameSetup setup, int index) {
        boolean human = index < setup.humanPlayers();
        String label = I18n.t(human ? UiTexts.LOBBY_SUMMARY_HUMAN : UiTexts.LOBBY_SUMMARY_AI,
                human ? index + 1 : index - setup.humanPlayers() + 1);
        Div row = new Div(line(UiTexts.LOBBY_SUMMARY_START_PRODUCTION, label, setup.startProductionForSeat(index)));
        row.addClassName("wizard-summary-seat");
        row.getStyle().set(de.zettsystems.starfare.style.CssProperties.SEAT_COLOR, setup.colorForSeat(index));
        if (!human) { row.add(line(UiTexts.AI_STRATEGY_SUMMARY, index - setup.humanPlayers() + 1,
                AiStrategyFields.label(setup.strategyForAi(index - setup.humanPlayers())))); }
        return row;
    }

    private static Div victory(GameSetup setup, Consumer<String> edit) {
        Div section = group(UiTexts.UX_VICTORY, edit);
        if (setup.ruleset().usesConfigurableVictory()) {
            section.add(line(UiTexts.VICTORY_RULES_SUMMARY, setup.victorySystemPercent(),
                    enabled(setup.victoryRules().alliancesAllowed()), enabled(setup.victoryRules().allianceVictoryAllowed()),
                    setup.victoryRules().allianceSystemPercent()));
            if (setup.victoryRules().alliancesAllowed()) {
                section.add(line(UiTexts.AI_ALLIANCES_SUMMARY, enabled(setup.victoryRules().aiAlliancesAllowed())));
            }
        } else { section.add(line(UiTexts.UX_SOLO_VICTORY, setup.victorySystemPercent())); }
        section.add(line(UiTexts.LOBBY_SUMMARY_COMBAT, setup.combatRandomnessPercent(), enabled(setup.battlePresentationEnabled())));
        if (setup.ruleset().resolvesCoalitionBattles()) { section.add(line(UiTexts.LOBBY_SUMMARY_COALITION_COMBAT)); }
        else { section.add(line(UiTexts.LOBBY_SUMMARY_RULES, I18n.t(setup.roundRules().attackOrder() == AttackOrder.RANDOM
                ? UiTexts.LOBBY_ATTACK_ORDER_RANDOM : UiTexts.LOBBY_ATTACK_ORDER_STRONGEST_FIRST))); }
        return section;
    }

    private static Div access(GameSetup setup, Consumer<String> edit) {
        return group(UiTexts.UX_ACCESS, edit,
                setup.humanPlayers() < 2 ? line(UiTexts.UX_NO_DEADLINE)
                        : line(UiTexts.LOBBY_SUMMARY_ROUNDS, DurationLabels.label(setup.roundRules().roundLimit()),
                                DurationLabels.label(setup.roundRules().stragglerLimit())),
                line(UiTexts.LOBBY_SUMMARY_VISIBILITY, I18n.t(UiTexts.GAME_PRIVATE)),
                line(UiTexts.LOBBY_SUMMARY_ACCESS,
                        I18n.t(setup.observersAllowed() ? UiTexts.LOBBY_SUMMARY_OBSERVERS_ALLOWED : UiTexts.LOBBY_SUMMARY_OBSERVERS_BLOCKED),
                        I18n.t(setup.reentryAllowed() ? UiTexts.LOBBY_SUMMARY_REENTRY_ALLOWED : UiTexts.LOBBY_SUMMARY_REENTRY_BLOCKED)));
    }

    private static String enabled(boolean value) {
        return I18n.t(value ? UiTexts.LOBBY_SUMMARY_ENABLED : UiTexts.LOBBY_SUMMARY_DISABLED);
    }

    private static Div group(String key, Consumer<String> edit, Component... rows) {
        Button change = new Button(I18n.t(UiTexts.UX_CHANGE), _ -> edit.accept(key));
        change.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);
        Div heading = new Div(new H3(I18n.t(key)), change);
        heading.addClassName("wizard-summary-heading");
        Div group = new Div(heading);
        group.add(rows);
        group.addClassName("wizard-summary-group");
        return group;
    }

    private static Span line(String key, Object... values) {
        Span line = new Span(I18n.t(key, values));
        line.addClassName("wizard-summary-line");
        return line;
    }
}
