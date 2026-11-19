package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.AbstractBrowserlessTest;
import de.zettsystems.starfare.game.application.GameSessionStore;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.testsupport.MetalStates;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Mining assistance is saved only after a global preview of exactly the chosen settings. */
class MiningAutomationDialogTest extends AbstractBrowserlessTest {
    private static final String OWNER = "owner";

    @Autowired
    private GameSessionStore store;

    private GameId id = GameId.of("unset");

    @BeforeEach
    void aRunningMetalGame() {
        var state = MetalStates.running(0, 100);
        state.seatByUser().put(OWNER, 1);
        id = GameId.newId();
        store.save(new GameSession(id, "Mining dialog", OWNER, Instant.now(), state));
    }

    @AfterEach
    void removeTheGame() {
        store.delete(id);
    }

    @Test
    void savesTheReviewedSettingsAndAnyChangeAsksForANewPreview() {
        Dialog dialog = open(Set.of());
        assertThat(dialog.getHeaderTitle()).isEqualTo("Förderautomatik");
        assertThat(save(dialog).isEnabled()).as("nothing reviewed yet").isFalse();

        test(find(Checkbox.class, dialog).withCaption("Dauerhaft einschalten").single()).click();
        test(rounds(dialog)).setValue(1);
        test(preview(dialog)).click();
        assertThat(elementText(dialog.getElement())).contains("Geplanter Verbrauch:");
        assertThat(save(dialog).isEnabled()).isTrue();

        test(rounds(dialog)).setValue(2);
        assertThat(save(dialog).isEnabled()).as("the preview no longer matches").isFalse();
        assertThat(elementText(dialog.getElement())).doesNotContain("Geplanter Verbrauch:");

        test(preview(dialog)).click();
        test(save(dialog)).click();

        assertThat(dialog.isOpened()).isFalse();
        var policy = games.miningAutomationFor(id, OWNER).orElseThrow().policy();
        assertThat(policy.enabled()).isTrue();
        assertThat(policy.reserveRounds()).isEqualTo(2);
    }

    @Test
    void aProductionChangeAfterThePreviewIsNotSaved() {
        Dialog dialog = open(Set.of());
        test(rounds(dialog)).setValue(1);
        test(preview(dialog)).click();
        registry.writeState(id, state -> {
            state.planMining(2, 1);
            return null;
        });
        test(save(dialog)).click();

        assertThat(dialog.isOpened()).isTrue();
        assertThat(save(dialog).isEnabled()).isFalse();
        assertThat(notifications()).anyMatch(text -> text.startsWith("Nicht gespeichert."));
        assertThat(games.miningAutomationFor(id, OWNER).orElseThrow().policy().revision()).isZero();
    }

    @Test
    void systemsPinnedOnTheMapStayProtected() {
        Dialog dialog = open(Set.of(1));
        MultiSelectComboBox<Integer> protectedSystems = protectedSystems(dialog);
        assertThat(protectedSystems.getValue()).contains(1);

        protectedSystems.setValue(Set.of());
        assertThat(protectedSystems.getValue()).as("a local pin cannot be removed here").contains(1);
    }

    @Test
    void afterSubmittingTheTurnTheSettingsAreReadOnly() {
        registry.writeState(id, state -> state.submittedThisTurn().add(1));
        Dialog dialog = open(Set.of());

        assertThat(rounds(dialog).isEnabled()).isFalse();
        assertThat(preview(dialog).isEnabled()).isFalse();
        assertThat(save(dialog).isEnabled()).isFalse();
        assertThat(find(Checkbox.class, dialog).withCaption("Dauerhaft einschalten").single().isEnabled()).isFalse();
    }

    private Dialog open(Set<Integer> localPins) {
        MiningAutomationDialog.open(games, id, OWNER, localPins);
        return openedDialog();
    }

    private IntegerField rounds(Dialog dialog) {
        return find(IntegerField.class, dialog).withId("mining-reserve-rounds").single();
    }

    @SuppressWarnings("unchecked") // rationale: the finder is not typed, the dialog's field is.
    private MultiSelectComboBox<Integer> protectedSystems(Dialog dialog) {
        return find(MultiSelectComboBox.class, dialog).withLabel("Vor Automatik geschützte Systeme").single();
    }

    private Button preview(Dialog dialog) {
        return find(Button.class, dialog).withText("Globale Vorschau").single();
    }

    private Button save(Dialog dialog) {
        return find(Button.class, dialog).withText("Einstellungen übernehmen").single();
    }

    private List<String> notifications() {
        return find(Notification.class).all().stream().map(notification -> test(notification).getText()).toList();
    }
}
