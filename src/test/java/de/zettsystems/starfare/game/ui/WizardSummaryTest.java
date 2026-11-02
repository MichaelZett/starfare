package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.dom.Element;
import de.zettsystems.starfare.game.values.GalaxyLayout;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.game.values.VictoryRules;
import de.zettsystems.starfare.i18n.I18n;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class WizardSummaryTest {

    @Test
    void headerNamesTheGameAndItsKeyNumbers() {
        String text = textOf(GameSetup.defaults());

        assertThat(text).contains("Aufstand bei Deneb", String.valueOf(GameSetup.defaults().systemCount()),
                I18n.t(UiTexts.UX_SUMMARY_PERCENT, GameSetup.defaults().victorySystemPercent()));
    }

    @Test
    void everySeatAppearsWithItsStrategy() {
        GameSetup setup = GameSetup.defaults();

        List<Component> seats = byClass(summary(setup), "wizard-summary-seat");

        assertThat(seats).hasSize(setup.totalPlayers());
        assertThat(textOf(setup)).contains(I18n.t(UiTexts.AI_STRATEGY_SUMMARY, 1,
                AiStrategyFields.label(setup.strategyForAi(0))));
    }

    @Test
    void soloGameHasNoDeadlineAndGroupGameShowsItsLimits() {
        var defaults = GameSetup.defaults();
        GameSetup group = new GameSetup(16, 2, 1, List.of(5, 5, 5), 1, 10, 20, true, true,
                defaults.seatColorHexes(), defaults.productionDistribution(), defaults.galaxyLayout(), false,
                10, defaults.roundRules(), 70);

        assertThat(textOf(defaults)).contains(I18n.t(UiTexts.UX_NO_DEADLINE));
        assertThat(textOf(group)).doesNotContain(I18n.t(UiTexts.UX_NO_DEADLINE))
                .contains(I18n.t(UiTexts.LOBBY_FIELD_ROUND_LIMIT), I18n.t(UiTexts.LOBBY_FIELD_STRAGGLER_LIMIT));
    }

    @Test
    void aiAlliancesOnlyAppearWhenAlliancesAreAllowed() {
        GameSetup spaceward = GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD_SHIP_RESEARCH);

        assertThat(textOf(spaceward.chooseVictoryRules(new VictoryRules(70, true, true, 60, true))))
                .contains(I18n.t(UiTexts.UX_SUMMARY_AI_ALLIANCES), I18n.t(UiTexts.UX_SUMMARY_PERCENT, 60));
        assertThat(textOf(spaceward.chooseVictoryRules(new VictoryRules(70, false, false, 60, false))))
                .contains(I18n.t(UiTexts.UX_SUMMARY_ALLIANCES))
                .doesNotContain(I18n.t(UiTexts.UX_SUMMARY_AI_ALLIANCES));
    }

    @Test
    void classicGameHasNoAllianceFacts() {
        assertThat(textOf(GameSetup.defaults())).doesNotContain(I18n.t(UiTexts.UX_SUMMARY_ALLIANCES));
    }

    @Test
    void everyGroupOffersToChangeItsOwnChoices() {
        List<String> requested = new ArrayList<>();
        WizardSummary summary = new WizardSummary(GameSetup.defaults(), "Aufstand bei Deneb", RulesetCatalog.builtIn(),
                requested::add);

        descendants(summary).filter(Button.class::isInstance).map(Button.class::cast).forEach(Button::click);

        assertThat(requested).containsExactly(UiTexts.UX_GALAXY, UiTexts.UX_PARTICIPANTS, UiTexts.UX_VICTORY, UiTexts.UX_ACCESS);
    }

    @Test
    void previewFollowsTheChosenDistribution() {
        GalaxyPreview preview = new GalaxyPreview(GalaxyLayout.EVEN);
        assertThat(preview.hasClassName("wizard-galaxy-even")).isTrue();

        preview.show(GalaxyLayout.RANDOM);

        assertThat(preview.hasClassName("wizard-galaxy-even")).isFalse();
        assertThat(preview.hasClassName("wizard-galaxy-random")).isTrue();
        assertThat(byClass(preview, "wizard-galaxy-star")).hasSize(12);
    }

    private static WizardSummary summary(GameSetup setup) {
        return new WizardSummary(setup, "Aufstand bei Deneb", RulesetCatalog.builtIn(), _ -> { });
    }

    private static String textOf(GameSetup setup) {
        Element element = summary(setup).getElement();
        return element.getTextRecursively();
    }

    private static List<Component> byClass(Component root, String className) {
        return descendants(root).filter(component -> component.getElement().getClassList().contains(className)).toList();
    }

    private static Stream<Component> descendants(Component root) {
        return root.getChildren().flatMap(child -> Stream.concat(Stream.of(child), descendants(child)));
    }
}
