package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import de.zettsystems.starfare.AbstractBrowserlessTest;
import de.zettsystems.starfare.game.application.GameSessionStore;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The tutorial is chosen when the game is created and marked wherever the game is named. */
class TutorialWizardTest extends AbstractBrowserlessTest {

    @Autowired
    private GameSessionStore store;

    private final List<GameId> created = new ArrayList<>();

    @AfterEach
    void removeCreatedGames() {
        created.forEach(store::delete);
    }

    @Test
    void aTutorialIsMarkedInTheSummaryAndTheWaitingRoom() {
        loginAsNewPlayer("Tutorial Host");
        openWizard();
        test(checkbox("wizard-tutorial")).click();
        test(checkbox("host-only")).click();

        test(button("Zusammenfassung prüfen")).click();
        assertThat(marks()).as("the summary names the choice").hasSize(1);

        GameId id = create();
        assertThat(registry.readState(id, GameState::tutorial)).isTrue();
        assertThat(marks()).as("the waiting room shows it to everyone").hasSize(1);
    }

    @Test
    void aGameIsNoTutorialUnlessChosen() {
        loginAsNewPlayer("Tutorial Host");
        openWizard();
        assertThat(checkbox("wizard-tutorial").getValue()).isFalse();
        test(checkbox("host-only")).click();

        test(button("Zusammenfassung prüfen")).click();
        assertThat(marks()).isEmpty();

        GameId id = create();
        assertThat(registry.readState(id, GameState::tutorial)).isFalse();
        assertThat(marks()).as("the waiting room shows no mark").isEmpty();
    }

    private List<Component> marks() {
        return find(Component.class).withClassName(TutorialMark.CLASS_NAME).all();
    }

    private void openWizard() {
        navigate(LobbyView.class);
        test(find(Button.class).withText("Neues Spiel").atIndex(1)).click();
    }

    private GameId create() {
        List<GameId> before = games.listGames();
        test(button("Spiel anlegen")).click();
        runPendingUiAccess();
        GameId id = games.listGames().stream().filter(game -> !before.contains(game)).findFirst().orElseThrow();
        created.add(id);
        return id;
    }

    private Checkbox checkbox(String id) {
        return find(Checkbox.class).withId(id).single();
    }
}
