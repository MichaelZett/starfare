package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import de.zettsystems.starfare.game.application.BattleAcknowledgementService;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.social.application.UserPreferencesService;
import de.zettsystems.starfare.social.values.DisplaySetting;
import de.zettsystems.starfare.testsupport.DomEvents;
import de.zettsystems.starfare.testsupport.FleetUiStates;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class CoalitionJourneyTest extends AbstractMapJourneyTest {
    @Autowired private UserPreferencesService preferences;
    @Autowired private BattleAcknowledgementService acknowledgements;

    @Test
    void beneficiaryConcealmentAndEachAcknowledgementSurviveFreshViews() {
        var state = FleetUiStates.coalition();
        int arrival = state.turn() + state.travelRounds(1, 3);
        openGame(state, "Coalition Browserless");
        preferences.chooseDisplaySetting(account, DisplaySetting.SOUND, "false");
        reloadMap();
        chooseBeneficiary();
        while (view().turn() < arrival) {
            assertThat(games.submitTurn(id, 1)).isTrue();
            assertThat(games.submitTurn(id, 2)).isTrue();
            assertThat(games.submitTurn(id, 3)).isTrue();
        }
        runPendingUiAccess();
        assertThat(view().gameOver()).isTrue();
        assertThat(registry.<Integer>readState(id, current -> current.getSystem(3).ownerId())).isEqualTo(2);
        assertThat(view().report().events().stream().filter(TurnEvent.CoalitionBattle.class::isInstance)).hasSize(2);
        reloadMap();
        selectTab("Bericht");
        assertThat(find(Div.class).withClassName("event-battle-pending").all()).hasSize(2);
        assertThat(find(Component.class).withClassName("event-victory").all()).isEmpty();
        selectTab("Flotten");
        assertThat(viewText()).doesNotContain("Unterstützung in Gamma");
        assertThat(find(Div.class).withClassName("fleet-badge").all()).isEmpty();
        selectTab("Bericht");
        acknowledgeBattles();
        assertThat(elementText(openedDialog().getElement())).contains("Sieg:");
        dialogClick("Nachbetrachtung öffnen");
        reloadMap();
        selectTab("Bericht");
        assertThat(find(Div.class).withClassName("event-battle-pending").all()).isEmpty();
        var report = view().report();
        var confirmed = acknowledgements.acknowledgedEventIndices(account, id, report.turn());
        for (int index = 0; index < report.events().size(); index++) {
            if (report.events().get(index) instanceof TurnEvent.CoalitionBattle) { assertThat(confirmed).contains(index); }
        }
        click("Zurück");
        assertThat(find(Component.class).withClassName("event-coalition-battle").all())
                .anySatisfy(event -> assertThat(elementText(event.getElement())).contains("Besitzer: Partner"));
        assertThat(registry.<Boolean>readState(id, current -> current.fleets().stream()
                .anyMatch(fleet -> fleet.ownerId() == 1 && fleet.toSystemId() == 3))).isTrue();
    }

    @SuppressWarnings("unchecked") // rationale: conquest selection contains a ComboBox<Player>.
    private void chooseBeneficiary() {
        openSend("Alpha", "Gamma", false);
        enterAmount(40);
        find(Details.class, openedDialog()).all().forEach(details -> details.setOpened(true));
        ComboBox<Player> beneficiary = find(ComboBox.class, openedDialog()).single();
        test(beneficiary).selectItem("Partner");
        assertThat(elementText(openedDialog().getElement())).contains("deine Schiffe bleiben deine");
        dialogClick("Flotte senden");
        assertTab("Details");
        selectTab("Befehle");
        assertThat(view().plannedOrders().getFirst().beneficiaryId()).isEqualTo(2);
        assertThat(viewText()).contains("Erobern für: Partner");
        reloadMap();
        selectTab("Befehle");
        assertThat(viewText()).contains("Erobern für: Partner");
    }

    private void acknowledgeBattles() {
        for (int index = 0; index < 2; index++) {
            DomEvents.fire(find(Div.class).withClassName("event-battle-pending").atIndex(1), "click");
            assertThat(find(Div.class, openedDialog()).withClassName("coalition-side").all()).hasSize(index == 0 ? 2 : 3);
            assertThat(find(Div.class, openedDialog()).withClassName("coalition-defenders").single().getChildren().count()).isEqualTo(1);
            assertThat(elementText(openedDialog().getElement())).contains("Commander + Partner");
            assertThat(find(Checkbox.class, openedDialog()).single().getValue()).isFalse();
            dialogClick("Ergebnis übernehmen");
            if (index == 0) {
                assertThat(find(Component.class).withClassName("event-victory").all()).isEmpty();
                reloadMap();
                selectTab("Bericht");
                assertThat(find(Div.class).withClassName("event-battle-pending").all()).hasSize(1);
            }
        }
    }

    @AfterEach void removeAcknowledgements() { acknowledgements.forgetGame(id); }
}
