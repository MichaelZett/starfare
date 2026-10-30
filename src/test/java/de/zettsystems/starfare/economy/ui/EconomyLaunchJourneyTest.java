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
    void ordinaryWizardCreatesCurrentSpacewardWithMiningRecyclingAndResearch() {
        loginAsNewPlayer("Spaceward Founder");
        navigate(LobbyView.class);
        test(find(Button.class).withText("Neues Spiel").atIndex(1)).click();
        chooseSpaceward();
        test(button("Zusammenfassung prüfen")).click();
        assertThat(elementText(openedDialog().getElement())).contains("Spaceward · Regeln 1.3.0-r1");
        test(button("Spiel anlegen")).click();
        runPendingUiAccess();
        assertThat(getCurrentView()).isInstanceOf(MainView.class);
        id = games.listGames().getLast();
        var before = registry.readState(id, GameState::toSnapshot);
        assertThat(before.ruleset()).isEqualTo(RulesetRef.SPACEWARD_SHIP_RESEARCH);
        assertThat(before.metals()).isNotNull();
        var home = before.systems().stream().filter(system -> Integer.valueOf(1).equals(system.ownerId()))
                .findFirst().orElseThrow();
        select(home.name());
        assertThat(text("app-header-stats")).contains("10 (");
        click("production-plan-open");
        enter("industry-mining", 1);
        assertThat(text("production-save-status")).contains("Gespeichert");
        assertThat(text("metal-details")).contains("1 Punkt × 3 → 3 Metall/Runde");
        click("recycling-open");
        enter("recycling-ships", 1);
        click("recycling-apply");
        assertThat(text("recycling-details")).contains("Vorgemerkt: 1 Schiffe");
        navigate(de.zettsystems.starfare.legal.ui.ImprintView.class);
        navigate(MainView.class, java.util.Map.of("gameId", id.value()));
        select(home.name());
        assertThat(text("recycling-details")).contains("Vorgemerkt: 1 Schiffe");
        test(button("Runde abgeben")).click();
        runPendingUiAccess();
        assertThat(viewText()).contains("1 Schiffe verschrottet, 0,7 Metall zurückgewonnen.");
        select(home.name());
        assertThat(text("recycling-details")).contains("Gespeicherter Metallrest: 0,7");
        var after = registry.readState(id, GameState::toSnapshot);
        assertThat(after.metals().deposits().get(home.id()).remaining())
                .isEqualTo(before.metals().deposits().get(home.id()).remaining() - 3);
        assertThat(after.metals().recycling().orders().get(home.id())).isZero();
        assertThat(after.turn()).isEqualTo(before.turn() + 1);
    }

    @SuppressWarnings("unchecked") // rationale: the wizard's group contains RulesetDefinition values.
    private void chooseSpaceward() {
        RadioButtonGroup<RulesetDefinition> choices = find(RadioButtonGroup.class).withId("ruleset-selection").single();
        assertThat(choices.getValue().defaultRef()).isEqualTo(RulesetRef.SECTOR_FORCES);
        choices.setValue(registry.rulesets().find("spaceward").orElseThrow());
        assertThat(find(RadioButtonGroup.class).withId("ruleset-version-selection").all()).isEmpty();
    }
}
