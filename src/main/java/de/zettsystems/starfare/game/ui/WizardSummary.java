package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import de.zettsystems.starfare.game.values.AttackOrder;
import de.zettsystems.starfare.game.values.GalaxyLayout;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.ProductionDistribution;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.VictoryRules;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.CssProperties;

import java.util.function.Consumer;

/** Presents the final choices as a mission briefing, grouped by the question a player is checking. */
final class WizardSummary extends Div {
    private static final String SYSTEMS_ID = "wizard-summary-systems";

    WizardSummary(GameSetup setup, String name, RulesetCatalog catalog, Consumer<String> edit) {
        addClassName("wizard-confirmation-content");
        Div groups = new Div(galaxy(setup, edit), participants(setup, edit), victory(setup, edit), access(setup, edit));
        groups.addClassName("wizard-summary-groups");
        add(hero(setup, name, catalog), groups);
    }

    private static Div hero(GameSetup setup, String name, RulesetCatalog catalog) {
        Span variant = new Span(RulesetLabels.label(catalog, setup.ruleset()));
        variant.addClassName("wizard-hero-variant");
        Div heading = new Div(new H2(name), variant);
        heading.addClassName("wizard-hero-heading");
        Span systems = new Span(String.valueOf(setup.systemCount()));
        systems.setId(SYSTEMS_ID);
        Div stats = new Div(
                stat(VaadinIcon.STAR, UiTexts.UX_SUMMARY_SYSTEMS, systems),
                stat(VaadinIcon.GROUP, UiTexts.UX_SUMMARY_PLAYERS,
                        new Span(I18n.t(UiTexts.UX_SUMMARY_PLAYERS_VALUE, setup.humanPlayers(), setup.aiPlayers()))),
                stat(VaadinIcon.TROPHY, UiTexts.UX_SUMMARY_VICTORY,
                        new Span(I18n.t(UiTexts.UX_SUMMARY_PERCENT, setup.victorySystemPercent()))));
        stats.addClassName("wizard-hero-stats");
        Div hero = new Div(heading, stats);
        hero.addClassNames("wizard-hero", RulesetSelector.artClass(setup.ruleset()));
        return hero;
    }

    private static Div stat(VaadinIcon icon, String labelKey, Span value) {
        value.addClassName("wizard-hero-stat-value");
        Span label = new Span(I18n.t(labelKey));
        label.addClassName("wizard-hero-stat-label");
        Div stat = new Div(icon.create(), value, label);
        stat.addClassName("wizard-hero-stat");
        return stat;
    }

    private static Div galaxy(GameSetup setup, Consumer<String> edit) {
        Div facts = new Div(
                fact(VaadinIcon.CLUSTER, UiTexts.LOBBY_FIELD_GALAXY_LAYOUT, I18n.t(setup.galaxyLayout() == GalaxyLayout.EVEN
                        ? UiTexts.LOBBY_GALAXY_LAYOUT_EVEN : UiTexts.LOBBY_GALAXY_LAYOUT_RANDOM)),
                fact(VaadinIcon.ROCKET, UiTexts.UX_SUMMARY_GARRISON, String.valueOf(setup.startGarrison())),
                fact(VaadinIcon.FACTORY, UiTexts.UX_SUMMARY_NEUTRAL_PRODUCTION, I18n.t(UiTexts.UX_SUMMARY_RANGE,
                        setup.neutralMinProduction(), setup.neutralMaxProduction())),
                fact(VaadinIcon.RANDOM, UiTexts.UX_SUMMARY_NEUTRAL_DISTRIBUTION,
                        I18n.t(setup.productionDistribution() == ProductionDistribution.UNIFORM
                                ? UiTexts.LOBBY_PRODUCTION_DISTRIBUTION_UNIFORM
                                : UiTexts.LOBBY_PRODUCTION_DISTRIBUTION_GAUSSIAN)));
        facts.addClassName("wizard-summary-facts");
        Div content = new Div(facts, new GalaxyPreview(setup.galaxyLayout()));
        content.addClassName("wizard-summary-galaxy");
        return group(UiTexts.UX_GALAXY, WizardEmblem.GALAXY, edit, content);
    }

    private static Div participants(GameSetup setup, Consumer<String> edit) {
        Div seats = new Div();
        seats.addClassName("wizard-summary-seats");
        for (int index = 0; index < setup.totalPlayers(); index++) {
            seats.add(seat(setup, index));
        }
        return group(UiTexts.UX_PARTICIPANTS, WizardEmblem.FLEET, edit, seats);
    }

    private static Div seat(GameSetup setup, int index) {
        boolean human = index < setup.humanPlayers();
        int aiIndex = index - setup.humanPlayers();
        Icon token = (human ? VaadinIcon.USER : VaadinIcon.AUTOMATION).create();
        token.addClassName("wizard-summary-seat-token");
        Span name = new Span(human ? I18n.t(UiTexts.LOBBY_SUMMARY_HUMAN, index + 1)
                : I18n.t(UiTexts.AI_STRATEGY_SUMMARY, aiIndex + 1, AiStrategyFields.label(setup.strategyForAi(aiIndex))));
        name.addClassName("wizard-summary-seat-name");
        Span production = new Span(VaadinIcon.FACTORY.create(), new Span(String.valueOf(setup.startProductionForSeat(index))));
        production.addClassName("wizard-summary-seat-production");
        production.setTitle(I18n.t(UiTexts.UX_SUMMARY_SEAT_PRODUCTION));
        Div row = new Div(token, name, production);
        row.addClassName("wizard-summary-seat");
        row.getStyle().set(CssProperties.SEAT_COLOR, setup.colorForSeat(index));
        return row;
    }

    private static Div victory(GameSetup setup, Consumer<String> edit) {
        Div facts = new Div();
        facts.addClassName("wizard-summary-facts");
        if (setup.ruleset().usesConfigurableVictory()) {
            addAllianceFacts(facts, setup.victoryRules());
        }
        facts.add(fact(VaadinIcon.FLASH, UiTexts.UX_SUMMARY_COMBAT_RANDOMNESS,
                        I18n.t(UiTexts.UX_SUMMARY_COMBAT_RANDOMNESS_VALUE, setup.combatRandomnessPercent())),
                toggle(VaadinIcon.PLAY_CIRCLE, UiTexts.UX_SUMMARY_REPLAY, setup.battlePresentationEnabled()),
                setup.ruleset().resolvesCoalitionBattles()
                        ? fact(VaadinIcon.SWORD, UiTexts.UX_SUMMARY_COMBAT_FLOW, I18n.t(UiTexts.UX_SUMMARY_COALITION_COMBAT))
                        : fact(VaadinIcon.SWORD, UiTexts.UX_SUMMARY_ATTACK_ORDER,
                                I18n.t(setup.roundRules().attackOrder() == AttackOrder.RANDOM
                                        ? UiTexts.LOBBY_ATTACK_ORDER_RANDOM : UiTexts.LOBBY_ATTACK_ORDER_STRONGEST_FIRST)));
        return group(UiTexts.UX_VICTORY, WizardEmblem.VICTORY, edit, facts);
    }

    private static void addAllianceFacts(Div facts, VictoryRules rules) {
        facts.add(toggle(VaadinIcon.HANDSHAKE, UiTexts.UX_SUMMARY_ALLIANCES, rules.alliancesAllowed()));
        if (rules.alliancesAllowed()) {
            facts.add(toggle(VaadinIcon.AUTOMATION, UiTexts.UX_SUMMARY_AI_ALLIANCES, rules.aiAlliancesAllowed()));
        }
        facts.add(rules.allianceVictoryAllowed()
                ? fact(VaadinIcon.TROPHY, UiTexts.UX_SUMMARY_ALLIANCE_VICTORY,
                        I18n.t(UiTexts.UX_SUMMARY_PERCENT, rules.allianceSystemPercent()))
                : toggle(VaadinIcon.TROPHY, UiTexts.UX_SUMMARY_ALLIANCE_VICTORY, false));
    }

    private static Div access(GameSetup setup, Consumer<String> edit) {
        Div facts = new Div();
        facts.addClassName("wizard-summary-facts");
        if (setup.humanPlayers() < 2) {
            Span note = new Span(I18n.t(UiTexts.UX_NO_DEADLINE));
            note.addClassName("wizard-summary-note");
            facts.add(note);
        } else {
            facts.add(fact(VaadinIcon.CLOCK, UiTexts.LOBBY_FIELD_ROUND_LIMIT, DurationLabels.label(setup.roundRules().roundLimit())),
                    fact(VaadinIcon.HOURGLASS, UiTexts.LOBBY_FIELD_STRAGGLER_LIMIT,
                            DurationLabels.label(setup.roundRules().stragglerLimit())));
        }
        facts.add(fact(VaadinIcon.LOCK, UiTexts.UX_SUMMARY_VISIBILITY, I18n.t(UiTexts.GAME_PRIVATE)),
                state(VaadinIcon.EYE, UiTexts.UX_SUMMARY_OBSERVERS, setup.observersAllowed(),
                        UiTexts.LOBBY_SUMMARY_OBSERVERS_ALLOWED, UiTexts.LOBBY_SUMMARY_OBSERVERS_BLOCKED),
                state(VaadinIcon.SIGN_IN, UiTexts.UX_SUMMARY_REENTRY, setup.reentryAllowed(),
                        UiTexts.LOBBY_SUMMARY_REENTRY_ALLOWED, UiTexts.LOBBY_SUMMARY_REENTRY_BLOCKED));
        return group(UiTexts.UX_ACCESS, WizardEmblem.ROUNDS, edit, facts);
    }

    private static Div group(String key, WizardEmblem emblem, Consumer<String> edit, Component content) {
        Button change = new Button(I18n.t(UiTexts.UX_CHANGE), VaadinIcon.PENCIL.create(), _ -> edit.accept(key));
        change.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);
        Div heading = new Div(emblem.create(), new H3(I18n.t(key)), change);
        heading.addClassName("wizard-summary-heading");
        Div group = new Div(heading, content);
        group.addClassName("wizard-summary-group");
        return group;
    }

    private static Div toggle(VaadinIcon icon, String labelKey, boolean enabled) {
        return state(icon, labelKey, enabled, UiTexts.LOBBY_SUMMARY_ENABLED, UiTexts.LOBBY_SUMMARY_DISABLED);
    }

    private static Div state(VaadinIcon icon, String labelKey, boolean on, String onKey, String offKey) {
        Div row = fact(icon, labelKey, I18n.t(on ? onKey : offKey));
        row.addClassName(on ? "wizard-summary-on" : "wizard-summary-off");
        return row;
    }

    private static Div fact(VaadinIcon icon, String labelKey, String value) {
        Span label = new Span(I18n.t(labelKey));
        label.addClassName("wizard-summary-label");
        Span shown = new Span(value);
        shown.addClassName("wizard-summary-value");
        Div row = new Div(icon.create(), label, shown);
        row.addClassName("wizard-summary-line");
        return row;
    }
}
