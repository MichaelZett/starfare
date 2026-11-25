package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import de.zettsystems.starfare.game.application.BattleAcknowledgementService;
import de.zettsystems.starfare.game.application.Broadcaster;
import de.zettsystems.starfare.game.application.GameEvent;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.legal.ui.ImprintView;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;
import de.zettsystems.starfare.testsupport.DomEvents;
import de.zettsystems.starfare.testsupport.ReviewGameFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The winner continues towards full conquest: the outcome closes, the round deadline starts again, and the
 * second end once more waits for the open battles of both players.
 */
class GameContinuationJourneyTest extends AbstractMapJourneyTest {
    private static final String CONTINUE = "Bis zur vollständigen Eroberung weiterspielen";

    @Autowired
    private Broadcaster broadcaster;
    @Autowired
    private BattleAcknowledgementService acknowledgements;

    private ReviewGameFixture.Game game = new ReviewGameFixture.Game(id, 1, 2);
    private String hostEmail = "";
    private String guestEmail = "";

    @Test
    void theWinnerContinuesAndTheSecondEndWaitsForTheOpenBattlesAgain() {
        guestEmail = "browserless-" + UUID.randomUUID() + "@example.test";
        accounts.createAccount(guestEmail, "Review Loser");
        hostEmail = loginAsNewPlayer("Review Winner");
        game = reviewGames.create(hostEmail, guestEmail);
        id = game.id();

        openMapAs(guestEmail);
        finishRound(false);
        assertThat(outcome()).as("the loser has no open battle and sees the outcome at once").isNotNull();
        openMapAs(hostEmail);
        selectTab("Bericht");
        assertPending();
        acknowledgeTheBattle();
        assertThat(outcome()).isNotNull();
        assertThat(text("event-victory")).contains("60");
        assertReviewHidden();

        Instant beforeContinuation = Instant.now();
        test(find(Button.class, outcome()).withText(CONTINUE).single()).click();
        runPendingUiAccess();
        assertThat(outcome()).isNull();
        assertThat(registry.readState(id, GameState::gameOver)).isFalse();
        assertThat(registry.readState(id, GameState::victorySystemPercent)).isEqualTo(100);
        assertThat(registry.readState(id, GameState::turnStartedAt)).isBetween(beforeContinuation, Instant.now());
        assertThat(find(Component.class).withClassName("round-status-clock").all()).isNotEmpty();
        openMapAs(guestEmail);
        assertThat(outcome()).as("the game goes on for the loser, too").isNull();
        assertThat(find(Component.class).withClassName("round-status-clock").all()).isNotEmpty();

        finishRound(true);
        assertPending();
        openMapAs(hostEmail);
        selectTab("Bericht");
        assertPending();
        reloadMap();
        selectTab("Bericht");
        assertPending();
        acknowledgeTheBattle();
        assertThat(text("event-victory")).contains("100");
        assertThat(find(Button.class, outcome()).withText(CONTINUE).all()).as("only once").isEmpty();

        openMapAs(guestEmail);
        selectTab("Bericht");
        assertPending();
        acknowledgeTheBattle();
        assertThat(elementText(outcome().getElement())).contains("Dein Ergebnis: Niederlage");
    }

    @AfterEach
    void forgetAcknowledgements() {
        acknowledgements.forgetGame(id);
    }

    /** The host's round: a battle and a victory, at 60 % or by conquering every system. */
    private void finishRound(boolean fullConquest) {
        int host = game.hostSeat();
        int guest = game.guestSeat();
        registry.writeState(id, state -> {
            int systemId = fullConquest ? 3 : 4;
            String name = state.getSystem(systemId).name();
            if (fullConquest) {
                List.copyOf(state.systems()).forEach(system ->
                        state.updateSystem(system.id(), before -> before.captureBy(host, 0)));
            } else {
                state.configureVictorySystemPercent(60);
                state.updateSystem(systemId, system -> system.captureBy(host, 0));
            }
            state.reports().put(host, new TurnReport(state.turn(), List.of(), List.of(
                    new TurnEvent.BattleWon(host, systemId, name, 1, 1, 0, !fullConquest, 1.1, 0.9),
                    new TurnEvent.Victory(host, state.victorySystemPercent()))));
            state.reports().put(guest, new TurnReport(state.turn(), List.of(), fullConquest
                    ? List.of(new TurnEvent.SystemLost(guest, host, systemId, name, 1, 1, 0, 1.1, 0.9),
                    new TurnEvent.Defeat(host, "Review Host"))
                    : List.of(new TurnEvent.Defeat(host, "Review Host"))));
            state.captureReplayFrame();
            state.endGame(host);
            state.nextTurn();
            return null;
        });
        broadcaster.publish(new GameEvent.TurnAdvanced(id, registry.readState(id, GameState::turn)));
        broadcaster.publish(new GameEvent.GameFinished(id, host));
        runPendingUiAccess();
    }

    private void openMapAs(String email) {
        navigate(ImprintView.class);
        loginAs(email);
        openMap();
    }

    private void acknowledgeTheBattle() {
        DomEvents.fire(find(Div.class).withClassName("event-battle-pending").single(), "click");
        dialogClick("Ergebnis übernehmen");
    }

    private void assertPending() {
        assertThat(find(Div.class).withClassName("event-battle-pending").all()).hasSize(1);
        assertThat(outcome()).isNull();
        assertReviewHidden();
    }

    private void assertReviewHidden() {
        assertThat(find(Component.class).withClassName("review-controls").all()).noneMatch(Component::isVisible);
    }

    private Dialog outcome() {
        return find(Dialog.class).all().stream()
                .filter(dialog -> dialog.isOpened() && dialog.hasClassName("game-outcome-dialog"))
                .findFirst().orElse(null);
    }
}
