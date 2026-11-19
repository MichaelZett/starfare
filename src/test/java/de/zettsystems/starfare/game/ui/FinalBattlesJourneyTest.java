package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import de.zettsystems.starfare.game.application.Broadcaster;
import de.zettsystems.starfare.game.application.GameEvent;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Two battles decide the game; until both are evaluated, victory and the open map stay hidden. */
class FinalBattlesJourneyTest extends AbstractMapJourneyTest {

    @Autowired
    private Broadcaster broadcaster;

    @Test
    void victoryAndTheOpenMapWaitForTheLastBattleEvenAfterReload() {
        email = loginAsNewPlayer("Battle Tester");
        account = String.valueOf(accounts.findByEmail(email).orElseThrow().id());
        id = games.newGame(GameSetup.defaults(), account, "Final battles");
        player = games.joinGame(id, account, "Battle Tester", "Battle Empire").orElseThrow();
        assertThat(games.startGame(id)).isTrue();
        openMap();

        int seat = player;
        registry.writeState(id, state -> {
            var first = state.systems().get(1);
            var second = state.systems().get(2);
            state.updateSystem(first.id(), system -> system.captureBy(seat, 20));
            state.updateSystem(second.id(), system -> system.captureBy(seat, 60));
            state.reports().put(seat, new TurnReport(state.turn(), List.of(), List.of(
                    new TurnEvent.BattleWon(seat, first.id(), first.name(), 60, 40, 20, true, 64.8, 36.8),
                    new TurnEvent.BattleWon(seat, second.id(), second.name(), 90, 30, 60, true, 97.2, 27.6),
                    new TurnEvent.Victory(seat))));
            state.endGame(seat);
            state.nextTurn();
            return null;
        });
        broadcaster.publish(new GameEvent.GameFinished(id, seat));
        runPendingUiAccess();
        assertConcealed();

        reloadMap();
        selectTab("Bericht");
        assertConcealed();
    }

    private void assertConcealed() {
        assertThat(find(Div.class).withClassName("event-battle-pending").all()).hasSize(2);
        assertThat(find(Component.class).withClassName("event-victory").all()).isEmpty();
        assertThat(find(Component.class).withClassName("review-controls").all()).noneMatch(Component::isVisible);
        assertThat(find(Div.class).withClassName("sys-fog").all()).isNotEmpty();
    }
}
