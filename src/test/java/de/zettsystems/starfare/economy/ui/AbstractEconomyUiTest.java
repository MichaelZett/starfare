package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridTester;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.AbstractBrowserlessTest;
import de.zettsystems.starfare.game.application.GameSessionStore;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.ui.MainView;
import de.zettsystems.starfare.game.ui.ArchiveView;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.legal.ui.ImprintView;
import de.zettsystems.starfare.testsupport.DomEvents;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Component interactions, persisted fixtures and fresh route instances for economy journeys. */
abstract class AbstractEconomyUiTest extends AbstractBrowserlessTest {
    @Autowired
    protected GameSessionStore store;
    protected GameId id = GameId.newId();

    protected void openGame(GameState state, String name) {
        String email = loginAsNewPlayer("Economy Tester");
        String host = String.valueOf(accounts.findByEmail(email).orElseThrow().id());
        state.seatByUser().put(host, 1);
        if (!state.started()) { state.start(); }
        store.save(new GameSession(id, name, host, Instant.now(), state));
        navigate(MainView.class, Map.of("gameId", id.value()));
        select("Alpha");
    }

    protected void reloadMap() {
        navigate(ImprintView.class);
        navigate(MainView.class, Map.of("gameId", id.value()));
        select("Alpha");
    }

    protected void select(String name) {
        test(mapSystem(name)).click();
        runPendingUiAccess();
        expandDetails();
    }

    protected void expandDetails() {
        for (int level = 0; level < 3; level++) {
            find(Details.class).all().forEach(details -> details.setOpened(true));
            roundTrip();
        }
    }

    protected void click(String buttonId) {
        test(find(Button.class).withId(buttonId).single()).click();
        runPendingUiAccess();
        expandDetails();
    }

    protected void enter(String fieldId, int value) {
        test(field(fieldId)).setValue(value);
        runPendingUiAccess();
        expandDetails();
    }

    protected IntegerField field(String fieldId) {
        return find(IntegerField.class).withId(fieldId).single();
    }

    protected String text(String className) {
        return elementText(find(Component.class).withClassName(className).single().getElement());
    }

    protected void nextRound() {
        assertThat(games.submitTurn(id, 1)).isTrue();
        assertThat(games.submitTurn(id, 2)).isTrue();
        runPendingUiAccess();
        select("Alpha");
    }

    protected int mining(int system) {
        return registry.readState(id, state -> state.metals().orElseThrow().mining().get(system));
    }

    protected int garrison() {
        return registry.readState(id, state -> state.getSystem(1).garrison());
    }

    protected void finishAndReviewFirstRound() {
        registry.writeState(id, state -> { state.endGame(1); return null; });
        reloadMap();
        reviewFirstRound();
    }

    protected void reviewFirstRound() {
        DomEvents.changeRange(find(Input.class).withId("review-timeline").single(), 1);
        select("Alpha");
        assertThat(text("review-notice")).contains("Historischer Stand: vollständig aufgedeckt.");
    }

    @SuppressWarnings("unchecked") // rationale: archive cells are rendered components; their row type is immaterial here.
    protected void assertArchived(String name) {
        navigate(ArchiveView.class);
        GridTester<Grid<Object>, Object> grid = test(find(Grid.class).single());
        assertThat(java.util.stream.IntStream.range(0, grid.size())
                .mapToObj(row -> elementText(grid.getCellComponent(row, 0).getElement())).toList())
                .anySatisfy(row -> assertThat(row).contains(name, "Spaceward"));
    }

    @AfterEach
    void removeFixture() {
        store.delete(id);
    }
}
