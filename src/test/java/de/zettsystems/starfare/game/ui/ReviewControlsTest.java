package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.ReplayEventMarker;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewControlsTest {
    private static final List<Player> PLAYERS = List.of(new Player(1, "Ada", false, "#ff0000"),
            new Player(2, "Bo", false, "#00ff00"));
    private static final List<Integer> TURNS = List.of(1, 2, 3, 4);
    private static final ReplayEventMarker BATTLE = new ReplayEventMarker(2, 5, "Alpha", 0, false);
    private static final ReplayEventMarker CONQUEST = new ReplayEventMarker(3, 6, "Beta", 1, true);

    private final List<ReplayEventMarker> selected = new ArrayList<>();
    private final ReviewControls controls = new ReviewControls(() -> { }, selected::add);

    @Test
    void fromTheFinalStateNextEventStepsThroughAllEventsAndStopsAfterTheLast() {
        controls.show(PLAYERS, 1, TURNS, List.of(BATTLE, CONQUEST));
        assertThat(controls.replayTurn()).as("final state").isNegative();

        nextEvent();
        assertThat(selected).containsExactly(BATTLE);
        assertThat(controls.replayTurn()).isEqualTo(2);

        nextEvent();
        assertThat(selected).containsExactly(BATTLE, CONQUEST);
        assertThat(controls.replayTurn()).isEqualTo(3);

        nextEvent();
        assertThat(selected).as("no event after the last one").containsExactly(BATTLE, CONQUEST);
        assertThat(controls.replayTurn()).isEqualTo(3);
    }

    @Test
    void fromAHistoricalTurnNextEventSkipsEarlierEvents() {
        controls.show(PLAYERS, 1, TURNS, List.of(BATTLE, CONQUEST));
        button("Zurück").click();
        button("Zurück").click();
        assertThat(controls.replayTurn()).isEqualTo(3);

        nextEvent();

        assertThat(selected).containsExactly(CONQUEST);
        assertThat(controls.replayTurn()).isEqualTo(3);
    }

    @Test
    void afterTheLastEventsTurnNextEventSelectsNothing() {
        controls.show(PLAYERS, 1, TURNS, List.of(BATTLE));
        button("Zurück").click();
        assertThat(controls.replayTurn()).isEqualTo(4);

        nextEvent();

        assertThat(selected).isEmpty();
        assertThat(controls.replayTurn()).isEqualTo(4);
    }

    @Test
    void withoutEventsNextEventIsHiddenAndDoesNothing() {
        controls.show(PLAYERS, 1, TURNS, List.of());
        Button next = button("Nächstes Ereignis");
        assertThat(next.isVisible()).isFalse();

        next.click();

        assertThat(selected).isEmpty();
        assertThat(controls.replayTurn()).isNegative();
    }

    @Test
    void anEventMarkSelectsItsEventAndNextEventContinuesFromThere() {
        controls.show(PLAYERS, 1, TURNS, List.of(BATTLE, CONQUEST));
        button("⚔ Runde 2 · Alpha").click();

        nextEvent();

        assertThat(selected).containsExactly(BATTLE, CONQUEST);
    }

    private void nextEvent() {
        button("Nächstes Ereignis").click();
    }

    private Button button(String text) {
        return all(controls).filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> button.getText().equals(text)).findFirst().orElseThrow();
    }

    private static Stream<Component> all(Component component) {
        return component.getChildren().flatMap(child -> Stream.concat(Stream.of(child), all(child)));
    }
}
