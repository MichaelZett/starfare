package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import de.zettsystems.starfare.AbstractBrowserlessTest;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.GameVisibility;
import de.zettsystems.starfare.game.values.RoundRules;
import de.zettsystems.starfare.legal.ui.ImprintView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** The host adjusts both deadlines of a running game; others may look but not change them. */
class RoundRulesDialogTest extends AbstractBrowserlessTest {

    @Autowired
    private PlayerDirectory players;

    private final AtomicInteger saved = new AtomicInteger();
    private GameId id = GameId.of("unset");
    private String hostEmail = "";
    private RoundRules initial = RoundRules.defaults();

    @BeforeEach
    void aRunningGameWithHostAndGuest() {
        hostEmail = loginAsNewPlayer("Rules Host");
        String host = UserContext.currentPlayerId().orElseThrow();
        GameSetup setup = GameSetup.defaults().seatPlayers(2, 0).chooseGalaxy(20, de.zettsystems.starfare.game.values.GalaxyLayout.EVEN);
        id = games.newGame(setup, host, "Rules test");
        join(host);
        assertThat(games.changeVisibility(id, host, GameVisibility.PUBLIC)).isTrue();
        navigate(ImprintView.class);
        loginAsNewPlayer("Rules Guest");
        join(UserContext.currentPlayerId().orElseThrow());
        assertThat(games.startGame(id)).isTrue();
        initial = games.roundRulesFor(id, host).orElseThrow();
    }

    @Test
    void theHostChangesBothDeadlines() {
        loginAs(hostEmail);
        RoundRulesDialog.open(games, id, saved::incrementAndGet);
        Dialog dialog = openedDialog();
        assertThat(dialog.getHeaderTitle()).isEqualTo("Rundenfristen anpassen");
        assertThat(roundLimit(dialog).getValue()).isEqualTo(initial.roundLimit());
        assertThat(stragglerLimit(dialog).getValue()).isEqualTo(initial.stragglerLimit());

        Duration round = other(RoundRules.ROUND_LIMIT_CHOICES, initial.roundLimit());
        Duration straggler = other(RoundRules.STRAGGLER_LIMIT_CHOICES, initial.stragglerLimit());
        test(roundLimit(dialog)).selectItem(DurationLabels.label(round));
        test(stragglerLimit(dialog)).selectItem(DurationLabels.label(straggler));
        test(save(dialog)).click();

        assertThat(dialog.isOpened()).isFalse();
        assertThat(saved).hasValue(1);
        RoundRules now = games.roundRulesFor(id, accountId()).orElseThrow();
        assertThat(now.roundLimit()).isEqualTo(round);
        assertThat(now.stragglerLimit()).isEqualTo(straggler);
        assertThat(now.attackOrder()).isEqualTo(initial.attackOrder());
    }

    @Test
    void aGuestSeesTheDeadlinesButCannotSaveThem() {
        RoundRulesDialog.open(games, id, saved::incrementAndGet);
        Dialog dialog = openedDialog();
        test(roundLimit(dialog)).selectItem(
                DurationLabels.label(other(RoundRules.ROUND_LIMIT_CHOICES, initial.roundLimit())));
        test(save(dialog)).click();

        assertThat(dialog.isOpened()).isTrue();
        assertThat(saved).hasValue(0);
        assertThat(games.roundRulesFor(id, accountId()).orElseThrow()).isEqualTo(initial);
    }

    @Test
    void someoneOutsideTheGameGetsNoDialog() {
        navigate(ImprintView.class);
        loginAsNewPlayer("Rules Stranger");
        RoundRulesDialog.open(games, id, saved::incrementAndGet);

        assertThat(find(Dialog.class).all()).isEmpty();
    }

    private void join(String account) {
        String name = players.displayName(account);
        assertThat(games.joinGame(id, account, name, name + " Empire")).isPresent();
    }

    private static Duration other(List<Duration> choices, Duration current) {
        return choices.stream().filter(choice -> !choice.equals(current)).findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked") // rationale: the finder is not typed, the dialog's fields are.
    private ComboBox<Duration> roundLimit(Dialog dialog) {
        return find(ComboBox.class, dialog).withLabel("Rundenlimit").single();
    }

    @SuppressWarnings("unchecked") // rationale: the finder is not typed, the dialog's fields are.
    private ComboBox<Duration> stragglerLimit(Dialog dialog) {
        return find(ComboBox.class, dialog).withLabel("Nachzügler-Limit").single();
    }

    private Button save(Dialog dialog) {
        return find(Button.class, dialog).withText("Fristen speichern").single();
    }

    private static String accountId() {
        return UserContext.currentPlayerId().orElseThrow();
    }
}
