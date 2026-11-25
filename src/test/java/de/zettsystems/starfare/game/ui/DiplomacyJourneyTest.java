package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.legal.ui.ImprintView;
import de.zettsystems.starfare.navigation.values.NavigationSettings;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two empires found an alliance only together, and a visible notice ends the alliance after the fixed notice
 * period and sends stationed ships home.
 */
class DiplomacyJourneyTest extends AbstractMapJourneyTest {
    private String hostEmail = "";
    private String partnerEmail = "";

    @Test
    void allianceNoticeAndDepartureNeedOrRespectBothEmpires() {
        partnerEmail = "browserless-" + UUID.randomUUID() + "@example.test";
        accounts.createAccount(partnerEmail, "Partner");
        String partner = String.valueOf(accounts.findByEmail(partnerEmail).orElseThrow().id());
        GameState state = twoEmpires();
        state.seatByUser().put(partner, 2);
        openGame(state, "Spaceward treaty test");
        hostEmail = email;

        foundingNeedsThePartner();
        shipsStationAtTheAlly();
        aVisibleNoticeEndsTheAllianceAndSendsTheShipsHome();
    }

    private void foundingNeedsThePartner() {
        click("Bündnisse");
        proposeAlliance();
        openTreatiesAs(partnerEmail);
        assertThat(treatyText()).contains("Neues Bündnis");
        assertThat(allied()).isFalse();
        treatyClick("Ablehnen / zurückziehen");
        assertThat(registry.<Boolean>readState(id, current -> current.diplomacy().proposals().isEmpty())).isTrue();

        openTreatiesAs(hostEmail);
        proposeAlliance();
        openTreatiesAs(partnerEmail);
        treatyClick("Zustimmen");
        assertThat(allied()).isTrue();
        openTreatiesAs(hostEmail);
        assertThat(treatyText()).contains("Kündigungsfrist: 3 Runden");
        treatyClick("Schließen");
        changeLanguage(Locale.ENGLISH);
        click("Alliances");
        assertThat(treatyText()).contains("Your alliance", "Notice period: 3 rounds", "Leave alliance");
        treatyClick("Close");
        changeLanguage(Locale.GERMAN);
    }

    private void shipsStationAtTheAlly() {
        assertThat(registry.<Boolean>writeState(id, current -> new DefaultFleetService().sendFleet(current, 1, 1, 2, 10))).isTrue();
        int arrival = registry.readState(id, current -> current.fleets().getFirst().arrivalTurn());
        while (games.viewFor(id, 1).turn() < arrival) {
            advance();
        }
        assertThat(registry.<Boolean>readState(id, current -> current.fleets().getFirst().journey().stationed())).isTrue();
        assertThat(registry.<Integer>readState(id, current -> current.getSystem(2).garrison())).isEqualTo(21);
    }

    private void aVisibleNoticeEndsTheAllianceAndSendsTheShipsHome() {
        int effective = games.viewFor(id, 1).turn() + 3;
        openTreatiesAs(hostEmail);
        treatyClick("Bündnis kündigen");
        assertThat(test(leaveConfirmation()).getText()).contains("Runde " + effective);
        test(leaveConfirmation()).cancel();
        var departures = registry.readState(id, current -> current.diplomacy().groupFor(1).orElseThrow().departures());
        assertThat(departures).isEmpty();
        treatyClick("Bündnis kündigen");
        test(leaveConfirmation()).confirm();
        runPendingUiAccess();

        String notice = "Diplomat scheidet zu Beginn von Runde " + effective + " aus.";
        openTreatiesAs(partnerEmail);
        assertThat(treatyText()).contains(notice);
        openTreatiesAs(hostEmail);
        assertThat(treatyText()).as("the notice survives a fresh view").contains(notice);
        treatyClick("Schließen");

        while (games.viewFor(id, 1).turn() < effective - 1) {
            advance();
        }
        assertThat(allied()).as("the alliance holds during the notice period").isTrue();
        advance();
        assertThat(allied()).isFalse();
        advance();
        reloadMap();
        selectTab("Flotten");
        assertThat(find(Div.class).withClassName("fleet-travel-card").all())
                .anyMatch(card -> elementText(card.getElement()).contains("Automatische Heimreise"));
        int home = registry.readState(id, current -> current.fleets().getFirst().arrivalTurn());
        while (games.viewFor(id, 1).turn() < home) {
            advance();
        }
        assertThat(registry.<Boolean>readState(id, current -> current.fleets().isEmpty())).isTrue();
        assertThat(registry.<Integer>readState(id, current -> current.getSystem(2).ownerId())).isEqualTo(2);
    }

    private void proposeAlliance() {
        Div partnerCard = find(Div.class, openedDialog())
                .withCondition(card -> "2".equals(card.getElement().getAttribute("data-diplomacy-partner"))).single();
        test(find(Button.class, partnerCard).withText("Bündnis vorschlagen").single()).click();
        runPendingUiAccess();
    }

    private void openTreatiesAs(String account) {
        navigate(ImprintView.class);
        loginAs(account);
        openMap();
        click("Bündnisse");
    }

    private void changeLanguage(Locale locale) {
        switchLanguage(locale);
        reloadMap();
    }

    private void treatyClick(String caption) {
        test(find(Button.class, openedDialog()).withText(caption).single()).click();
        runPendingUiAccess();
    }

    private String treatyText() {
        return elementText(openedDialog().getElement());
    }

    private ConfirmDialog leaveConfirmation() {
        return find(ConfirmDialog.class).withId("diplomacy-leave-confirm").single();
    }

    private boolean allied() {
        return registry.readState(id, current -> current.allied(1, 2));
    }

    private void advance() {
        assertThat(games.submitTurn(id, 1)).isTrue();
        assertThat(games.submitTurn(id, 2)).isTrue();
        runPendingUiAccess();
    }

    /** Spaceward with diplomacy; Beta (partner) lies between the two empires' worlds. */
    private static GameState twoEmpires() {
        GameState state = new GameState();
        state.resetForNewGame();
        state.rememberSetup(GameSetup.defaults().selectRuleset(RulesetRef.EXTENDED));
        state.players().addAll(List.of(new Player(1, "Diplomat", false, "#56b4e9"),
                new Player(2, "Partner", false, "#d55e00")));
        // Neighbours 495 map pixels apart: four rounds at 160 pixels per round, within the 495 range.
        state.systems().addAll(List.of(new StarSystem(1, "Alpha", 440, 385, 1, 50, 4, false),
                new StarSystem(2, "Beta", 935, 385, 2, 5, 4, false), new StarSystem(3, "Gamma", 1430, 385, 1, 5, 4, false),
                new StarSystem(4, "Delta", 1925, 385, 2, 100, 4, false)));
        for (int index = 5; index <= 8; index++) {
            state.systems().add(new StarSystem(index, "Remote " + index, 2750 + index * 66, 1430 + index * 55, null, 200, 4, true));
        }
        state.joinedHumanPlayerIds().addAll(List.of(1, 2));
        state.originalHumanPlayerIds().addAll(List.of(1, 2));
        state.intel().put(1, new HashMap<>());
        state.intel().put(2, new HashMap<>());
        state.configureNavigation(new NavigationSettings(495));
        state.start();
        return state;
    }
}
