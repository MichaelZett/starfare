package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.tabs.Tabs;
import de.zettsystems.starfare.AbstractBrowserlessTest;
import de.zettsystems.starfare.game.application.GameSessionStore;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.legal.ui.ImprintView;
import de.zettsystems.starfare.testsupport.DomEvents;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Real map components; all actions are followed by pending broadcast delivery. */
abstract class AbstractMapJourneyTest extends AbstractBrowserlessTest {
    @Autowired protected GameSessionStore store;
    protected GameId id = GameId.newId();
    protected int player = 1;
    protected String email = "";
    protected String account = "";

    protected void createReviewGame() {
        email = loginAsNewPlayer("Map Tester");
        account = String.valueOf(accounts.findByEmail(email).orElseThrow().id());
        var fixture = reviewGames.create(email, null);
        id = fixture.id();
        player = fixture.hostSeat();
        registry.writeState(id, state -> { state.addFleet(player, 1, 2, 2); return null; });
        openMap();
    }

    protected void openGame(GameState state, String name) {
        email = loginAsNewPlayer("Map Tester");
        account = String.valueOf(accounts.findByEmail(email).orElseThrow().id());
        state.seatByUser().put(account, player);
        store.save(new GameSession(id, name, account, Instant.now(), state));
        openMap();
    }

    protected void openMap() {
        navigate(MainView.class, Map.of("gameId", id.value()));
        runPendingUiAccess();
    }

    protected void reloadMap() {
        navigate(ImprintView.class);
        openMap();
    }

    protected void select(String name) {
        test(mapSystem(name)).click();
        runPendingUiAccess();
    }

    protected void click(String caption) {
        test(button(caption)).click();
        runPendingUiAccess();
    }

    @Override
    protected Button button(String caption) {
        return find(Button.class).withCondition(button -> button.getText().equals(caption)
                || button.getAriaLabel().filter(caption::equals).isPresent()).single();
    }

    protected void selectCard(String className, String contains) {
        DomEvents.fireFiltered(card(className, contains), "click", "!event.target.closest('vaadin-button, vaadin-details')");
        runPendingUiAccess();
    }

    protected void dialogClick(String caption) {
        Dialog dialog = openedDialog();
        test(find(Button.class, dialog).withText(caption).single()).click();
        runPendingUiAccess();
    }

    protected void enterAmount(int amount) {
        test(find(IntegerField.class, openedDialog()).single()).setValue(amount);
        roundTrip();
    }

    protected void openSend(String source, String target, boolean relocation) {
        select(source);
        click(relocation ? "Verlegung anlegen" : "Flotte senden");
        select(target);
        assertThat(openedDialog().hasClassName("send-fleet-dialog")).isTrue();
    }

    protected String text(String className) {
        return elementText(find(Component.class).withClassName(className).single().getElement()).strip();
    }

    protected Div card(String className, String contains) {
        return find(Div.class).withClassName(className).withCondition(div -> elementText(div.getElement()).contains(contains)).single();
    }

    protected void cardAction(String className, String contains, String caption) {
        test(find(Button.class, card(className, contains)).withText(caption).single()).click();
        runPendingUiAccess();
    }

    protected void assertTab(String caption) {
        assertThat(find(Tabs.class).withClassName("map-sidebar-tabs").single().getSelectedTab().getLabel()).isEqualTo(caption);
    }

    protected PlayerViewState view() { return games.viewFor(id, player); }

    protected void assertAmount(int amount, int maximum) {
        assertThat(find(IntegerField.class, openedDialog()).single().getValue()).isEqualTo(amount);
        Input slider = find(Input.class, openedDialog()).withClassName("send-fleet-slider").single();
        assertThat(slider.getValue()).isEqualTo(String.valueOf(amount));
        assertThat(slider.getElement().getAttribute("max")).isEqualTo(String.valueOf(maximum));
    }

    @AfterEach void removeGame() { store.delete(id); }
}
