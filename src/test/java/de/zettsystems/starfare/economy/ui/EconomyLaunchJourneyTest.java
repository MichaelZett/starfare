package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.ui.LobbyView;
import de.zettsystems.starfare.game.ui.MainView;
import de.zettsystems.starfare.game.values.RulesetDefinition;
import de.zettsystems.starfare.game.values.RulesetRef;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EconomyLaunchJourneyTest extends AbstractEconomyUiTest {
    @Test
    void ordinaryWizardCreatesTheExtendedRulesWithIndustryExpansion() {
        loginAsNewPlayer("Spaceward Founder");
        navigate(LobbyView.class);
        test(find(Button.class).withText("Neues Spiel").atIndex(1)).click();
        chooseSpaceward();
        assertThat(elementText(openedDialog().getElement())).contains("Erweitert").doesNotContain("2.0.0");
        test(button("Spiel anlegen")).click();
        runPendingUiAccess();
        assertThat(getCurrentView()).isInstanceOf(MainView.class);
        id = games.listGames().getLast();
        var before = registry.readState(id, GameState::toSnapshot);
        assertThat(before.ruleset()).isEqualTo(RulesetRef.EXTENDED);
        var home = before.systems().stream().filter(system -> Integer.valueOf(1).equals(system.ownerId()))
                .findFirst().orElseThrow();
        select(home.name());
        enter(IndustryPanel.ALLOCATION_ID, 1);
        assertThat(text("production-save-status")).contains("Gespeichert");
        test(button("Runde abgeben")).click();
        runPendingUiAccess();
        var after = registry.readState(id, GameState::toSnapshot);
        assertThat(after.turn()).isEqualTo(before.turn() + 1);
        var industry = after.industries().get(home.id());
        assertThat(industry.capacity() > before.industries().get(home.id()).capacity()
                || industry.expansionProgress() == 1).as("the expansion point was invested").isTrue();
    }

    @SuppressWarnings("unchecked") // rationale: the rules cards contain RulesetDefinition values.
    private void chooseSpaceward() {
        RadioButtonGroup<RulesetDefinition> choices = find(RadioButtonGroup.class).withId("ruleset-selection").single();
        assertThat(choices.getValue().defaultRef()).isEqualTo(RulesetRef.SECTOR_FORCES);
        choices.setValue(registry.rulesets().find("spaceward").orElseThrow());
    }
}
