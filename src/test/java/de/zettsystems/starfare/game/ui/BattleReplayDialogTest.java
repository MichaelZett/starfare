package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import de.zettsystems.starfare.AbstractBrowserlessTest;
import de.zettsystems.starfare.report.values.BattleReplay;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class BattleReplayDialogTest extends AbstractBrowserlessTest {

    @Test
    void eachSideShowsItsLabelIdentityShipsAndScaledMarkers() {
        BattleReplayDialog.open(new BattleReplay("Alpha", 20, 4, 15, 0),
                new BattleReplayDialog.BattleSides("Du", ""), null, () -> { });
        Dialog dialog = openedDialog();
        assertThat(dialog.getHeaderTitle()).isEqualTo("Schlacht bei Alpha");

        Div attacker = side(dialog, "battle-attacker");
        assertThat(texts(attacker)).startsWith("Sieger", "Angreifer", "Du", "20");
        assertThat(number(attacker).getElement().hasAttribute("data-battle-attacking")).isTrue();
        assertThat(number(attacker).getElement().hasAttribute("data-battle-defending")).isFalse();
        // 20 ships against 4: one marker stands for three ships.
        assertThat(markers(attacker)).hasSize(7)
                .allSatisfy(marker -> assertThat(marker.getElement().hasAttribute("data-battle-attacker-ship")).isTrue());

        Div defender = side(dialog, "battle-defender");
        assertThat(texts(defender)).startsWith("Verteidiger", "4");
        assertThat(find(Span.class, defender).withClassName("battle-identity").all()).as("no identity").isEmpty();
        assertThat(number(defender).getElement().hasAttribute("data-battle-defending")).isTrue();
        assertThat(number(defender).getElement().hasAttribute("data-battle-attacking")).isFalse();
        assertThat(markers(defender)).hasSize(2)
                .allSatisfy(marker -> assertThat(marker.getElement().hasAttribute("data-battle-defender-ship")).isTrue());
    }

    @Test
    void smallFleetsGetOneMarkerPerShipAndAnEmptySideKeepsOneMarker() {
        BattleReplayDialog.open(new BattleReplay("Beta", 3, 0, 3, 0),
                new BattleReplayDialog.BattleSides("", "Neutrales System"), "3", () -> { });
        Dialog dialog = openedDialog();

        Div attacker = side(dialog, "battle-attacker");
        assertThat(find(Span.class, attacker).withClassName("battle-identity").all()).isEmpty();
        assertThat(markers(attacker)).hasSize(3);
        Div defender = side(dialog, "battle-defender");
        assertThat(find(Span.class, defender).withClassName("battle-identity").single().getText()).isEqualTo("Neutrales System");
        assertThat(number(defender).getText()).isEqualTo("0");
        assertThat(markers(defender)).hasSize(1);
        assertThat(find(Div.class, dialog).withClassName("battle-replay-production").single().getText())
                .isEqualTo("Produktion von Beta: 3 je Runde");
    }

    @Test
    void offersSoundAndSpeedButNoSoundTestAndLeavesOutAnUnknownProduction() {
        BattleReplayDialog.open(new BattleReplay("Delta", 4, 2, 3, 0),
                new BattleReplayDialog.BattleSides("Du", "Gegner"), null, () -> { });
        Dialog dialog = openedDialog();

        assertThat(find(Button.class, dialog).all()).extracting(Button::getText).containsExactly("Ergebnis übernehmen");
        assertThat(find(com.vaadin.flow.component.checkbox.Checkbox.class, dialog).single().getLabel()).isEqualTo("Kampfgeräusche");
        assertThat(find(com.vaadin.flow.component.combobox.ComboBox.class, dialog).single().getLabel()).isEqualTo("Geschwindigkeit");
        assertThat(find(Div.class, dialog).withClassName("battle-replay-production").all()).isEmpty();
    }

    @Test
    void onlyTheWinnerIsNamedVictorAndGetsTheWreath() {
        BattleReplayDialog.open(new BattleReplay("Gamma", 5, 5, 0, 2),
                new BattleReplayDialog.BattleSides("Gegner", "Du"), null, () -> { });
        Dialog dialog = openedDialog();

        Div defender = side(dialog, "battle-defender");
        assertThat(defender.hasClassName("battle-winner")).isTrue();
        assertThat(find(Span.class, defender).withClassName("battle-victor").single().getText()).isEqualTo("Sieger");
        Div attacker = side(dialog, "battle-attacker");
        assertThat(attacker.hasClassName("battle-winner")).isFalse();
        assertThat(find(Span.class, attacker).withClassName("battle-victor").all()).isEmpty();
    }

    @Test
    void acknowledgingClosesTheReplayAndReportsIt() {
        AtomicInteger acknowledged = new AtomicInteger();
        BattleReplayDialog.open(new BattleReplay("Gamma", 5, 5, 0, 2),
                new BattleReplayDialog.BattleSides("Gegner", "Du"), null, acknowledged::incrementAndGet);
        Dialog dialog = openedDialog();

        test(find(Button.class, dialog).withText("Ergebnis übernehmen").single()).click();

        assertThat(dialog.isOpened()).isFalse();
        assertThat(acknowledged).hasValue(1);
    }

    private Div side(Dialog dialog, String sideClass) {
        return find(Div.class, dialog).withClassName(sideClass).single();
    }

    private Span number(Div side) {
        return find(Span.class, side).withClassName("battle-number").single();
    }

    private List<Span> markers(Div side) {
        return find(Span.class, side).withClassName("battle-ship").all();
    }

    private static List<String> texts(Div side) {
        return side.getChildren().filter(Span.class::isInstance).map(Span.class::cast).map(Span::getText).toList();
    }
}
