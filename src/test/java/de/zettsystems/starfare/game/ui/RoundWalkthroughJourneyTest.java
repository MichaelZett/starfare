package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.server.VaadinSession;
import de.zettsystems.starfare.game.application.BattleAcknowledgementService;
import de.zettsystems.starfare.game.application.Broadcaster;
import de.zettsystems.starfare.game.application.GameEvent;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.report.values.CosmicNews;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;
import de.zettsystems.starfare.style.values.DeviceLayout;
import de.zettsystems.starfare.testsupport.DomEvents;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** After a round change the located events come one by one, centred on the map, before any order. */
class RoundWalkthroughJourneyTest extends AbstractMapJourneyTest {
    private static final String HINT = "Erst die Ereignisse der Runde durchgehen";

    @Autowired
    private Broadcaster broadcaster;
    @Autowired
    private BattleAcknowledgementService acknowledgements;

    @Test
    void eachLocatedEventIsConfirmedBeforeOrdersAndTheProgressSurvivesAReload() {
        createReviewGame();
        int round = advanceWith(false,
                new TurnEvent.Production(player, 1, "Alpha", 5),
                new TurnEvent.BattleWon(player, 4, "Remote 4", 12, 8, 4, true, 12.5, 7.5),
                new TurnEvent.Cosmic(CosmicNews.SUPERNOVA, 5, "Remote 5", 0, 0, 0, 0, ""));

        assertTab("Bericht");
        assertCurtainInFrontOfTheBoard();
        assertThat(text("round-walkthrough")).contains("Runde " + round + " ausgewertet", "Ereignis 1 von 2", "Remote 4")
                .as("only the tutorial explains the walkthrough").doesNotContain(HINT);
        assertThat(text("sys-report-event-active")).startsWith("Remote 4");
        assertThat(submit().isEnabled()).isFalse();
        assertNoOrders();

        selectTab("Bericht");
        test(action("Schlacht ansehen")).click();
        runPendingUiAccess();
        assertThat(openedDialog().hasClassName("battle-replay-dialog")).isTrue();
        dialogClick("Ergebnis übernehmen");
        assertThat(text("round-walkthrough")).contains("Ereignis 2 von 2", "Supernova");
        assertThat(text("sys-report-event-active")).startsWith("Remote 5");

        reloadMap();
        assertTab("Bericht");
        assertThat(text("round-walkthrough")).as("the progress is stored").contains("Ereignis 2 von 2");
        assertThat(text("event-walkthrough-current")).contains("Remote 5");
        assertThat(submit().isEnabled()).isFalse();

        test(action("Weiter")).click();
        runPendingUiAccess();
        assertThat(find(Div.class).withClassName("round-walkthrough").all()).isEmpty();
        assertBoardFree();
        assertThat(acknowledgements.acknowledgedEventIndices(account, id, round)).containsExactlyInAnyOrder(1, 2);
        assertThat(submit().isEnabled()).isTrue();
        select("Alpha");
        assertThat(find(Button.class).withText("Flotte senden").all()).hasSize(1);
    }

    @Test
    void theReplayOfALostSystemShowsWhatItProduced() {
        createReviewGame();
        advanceWith(false, new TurnEvent.SystemLost(player, player + 1, 4, "Remote 4", 12, 8, 4, 12, 8,
                "Gegner", "Du", 7));

        test(action("Schlacht ansehen")).click();
        runPendingUiAccess();

        assertThat(find(Div.class, openedDialog()).withClassName("battle-replay-production").single().getText())
                .isEqualTo("Produktion von Remote 4: 7 je Runde");
    }

    @Test
    void theTutorialExplainsWhyTheOrdersWait() {
        createReviewGame(true);
        advanceWith(false, new TurnEvent.Cosmic(CosmicNews.SUN_UNSTABLE, 5, "Remote 5", 0, 0, 0, 9, ""));

        assertThat(text("round-walkthrough")).contains(HINT, "Ereignis 1 von 1");

        test(action("Weiter")).click();
        runPendingUiAccess();
        assertThat(viewText()).doesNotContain(HINT);
    }

    @Test
    void theDecidedGameShowsItsOutcomeOnlyAfterTheWalkthroughAndOffersNoOrdersMeanwhile() {
        createReviewGame();
        advanceWith(true,
                new TurnEvent.Cosmic(CosmicNews.SPACEPORT_QUAKE_LIGHT, 2, "Beta", 0, 1, 1, 0, ""),
                new TurnEvent.Victory(player));

        assertThat(outcome()).isNull();
        assertThat(find(Component.class).withClassName("event-victory").all()).isEmpty();
        assertThat(text("round-walkthrough")).contains("Ereignis 1 von 1", "Beta");
        assertThat(submit().isEnabled()).isFalse();
        assertNoOrders();

        selectTab("Bericht");
        test(action("Weiter")).click();
        runPendingUiAccess();
        assertThat(outcome()).isNotNull();
    }

    @Test
    void onPhonesTheCurtainLiesOverTheWholeScreen() {
        VaadinSession.getCurrent().setAttribute("starfare.deviceLayout", DeviceLayout.MOBILE);
        createReviewGame();
        advanceWith(false, new TurnEvent.Cosmic(CosmicNews.SUN_UNSTABLE, 5, "Remote 5", 0, 0, 0, 9, ""));

        var layout = find(MobileMapLayout.class).single();
        assertThat(find(Div.class, layout).withClassName("mobile-map-sheet").exists()).as("the map in front").isFalse();
        RoundCurtain curtain = assertCurtainInFrontOfTheBoard();
        assertThat(elementText(curtain.getElement())).contains("Ereignis 1 von 1", "Remote 5");

        test(find(Button.class, curtain).withText("Weiter").single()).click();
        runPendingUiAccess();
        assertBoardFree();
        assertThat(submit().isEnabled()).isTrue();
    }

    @AfterEach
    void forgetAcknowledgements() {
        acknowledgements.forgetGame(id);
    }

    /** Resolves the current round with the given report and returns the round the report belongs to. */
    private int advanceWith(boolean decided, TurnEvent... events) {
        int seat = player;
        int round = registry.readState(id, GameState::turn);
        registry.writeState(id, state -> {
            state.reports().put(seat, new TurnReport(round, List.of(), List.of(events)));
            if (decided) {
                state.captureReplayFrame();
                state.endGame(seat);
            }
            state.nextTurn();
            return null;
        });
        broadcaster.publish(new GameEvent.TurnAdvanced(id, round + 1));
        if (decided) {
            broadcaster.publish(new GameEvent.GameFinished(id, seat));
        }
        runPendingUiAccess();
        return round;
    }

    /** The locked map neither starts a target selection nor offers to send from an own system. */
    private void assertNoOrders() {
        DomEvents.fire(mapSystem("Alpha"), "contextmenu");
        runPendingUiAccess();
        assertThat(find(Div.class).withClassName("sys-target").all()).isEmpty();
        select("Alpha");
        assertThat(find(Button.class).withText("Flotte senden").all()).isEmpty();
    }

    /** The curtain shows and everything else of the map view is inert behind it. */
    private RoundCurtain assertCurtainInFrontOfTheBoard() {
        RoundCurtain curtain = find(RoundCurtain.class).single();
        assertThat(curtain.isVisible()).isTrue();
        List<Component> board = boardBehind(curtain);
        assertThat(board).isNotEmpty().allSatisfy(part ->
                assertThat(part.getElement().hasAttribute("inert")).as("inert: %s", part).isTrue());
        return curtain;
    }

    private void assertBoardFree() {
        // The lifted curtain is hidden, which the finder skips.
        RoundCurtain curtain = find(MainView.class).single().getChildren()
                .filter(RoundCurtain.class::isInstance).map(RoundCurtain.class::cast).findFirst().orElseThrow();
        assertThat(curtain.isVisible()).isFalse();
        assertThat(boardBehind(curtain)).noneSatisfy(part ->
                assertThat(part.getElement().hasAttribute("inert")).isTrue());
    }

    private static List<Component> boardBehind(RoundCurtain curtain) {
        return curtain.getParent().orElseThrow().getChildren().filter(part -> part != curtain).toList();
    }

    private Button submit() {
        return find(Button.class).withClassName("round-submit").single();
    }

    private Button action(String caption) {
        return find(Button.class).withClassName("round-walkthrough-action").withText(caption).single();
    }

    private Dialog outcome() {
        return find(Dialog.class).all().stream()
                .filter(dialog -> dialog.isOpened() && dialog.hasClassName("game-outcome-dialog"))
                .findFirst().orElse(null);
    }
}
