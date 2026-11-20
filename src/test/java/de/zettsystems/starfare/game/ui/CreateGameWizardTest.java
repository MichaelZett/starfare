package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.AbstractBrowserlessTest;
import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.application.GameSessionStore;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameTempo;
import de.zettsystems.starfare.game.values.GameVisibility;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.RoundRules;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.RulesetDefinition;
import de.zettsystems.starfare.game.values.RulesetRef;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Choices in the new-game wizard: AI strategies, a private game without the host, alliance options. */
class CreateGameWizardTest extends AbstractBrowserlessTest {

    @Autowired
    private GameSessionStore store;
    @Autowired
    private PlayerDirectory players;

    private final List<GameId> created = new ArrayList<>();

    @AfterEach
    void removeCreatedGames() {
        created.forEach(store::delete);
    }

    @Test
    void aiStrategiesFollowTheVariantAndSurviveSeatChanges() {
        loginAsNewPlayer("Strategist");
        openWizard();
        assertThat(variants().getValue().defaultRef()).isEqualTo(RulesetRef.SECTOR_FORCES);
        assertThat(choices(1)).hasSize(5);
        choose(1, "Früher Angriff");
        choose(2, "Verteidigung");

        test(aiPlayers()).setValue(1);
        assertThat(find(ComboBox.class).withId("ai-strategy-2").all()).isEmpty();
        test(aiPlayers()).setValue(2);
        assertSelection(1, AiStrategy.RUSH);
        assertSelection(2, AiStrategy.DEFENSE);

        chooseVariant(RulesetRef::isSpaceward);
        assertThat(choices(1)).hasSize(8);
        choose(1, "Adaptiver Industrieausbau");
        chooseVariant(ref -> ref == RulesetRef.SECTOR_FORCES);
        assertThat(choices(1)).hasSize(5);
        assertSelection(1, AiStrategy.BASELINE);
        assertSelection(2, AiStrategy.DEFENSE);
    }

    @Test
    void aiStrategiesAppearInTheSummaryAndAreSavedWithTheGame() {
        loginAsNewPlayer("Strategist");
        openWizard();
        chooseVariant(RulesetRef::isSpaceward);
        choose(1, "Adaptiver Industrieausbau");
        choose(2, "Verteidigung");

        test(find(Checkbox.class).withId("host-only").single()).click();
        test(button("Zusammenfassung prüfen")).click();
        assertThat(elementText(openedDialog().getElement()))
                .contains("Adaptiver Industrieausbau", "Verteidigung", "Keine Rundenfrist");
        assertThat(find(Component.class).withClassName("wizard-summary-group").all()).hasSize(4);
        test(button("Zurück zu den Einstellungen")).click();
        assertSelection(1, AiStrategy.INDUSTRY_ADAPTIVE);

        test(button("Zusammenfassung prüfen")).click();
        GameId id = create();
        assertThat(getCurrentView()).as("a free seat opens the waiting room").isInstanceOf(WaitingRoomView.class);
        List<Player> seats = registry.readState(id, state -> List.copyOf(state.players()));
        assertThat(seats).extracting(Player::aiStrategy)
                .containsExactly(AiStrategy.BASELINE, AiStrategy.INDUSTRY_ADAPTIVE, AiStrategy.DEFENSE);
        assertThat(store.load(id).orElseThrow().readState(state -> state.originalSetup().orElseThrow()).aiStrategies())
                .containsExactly(AiStrategy.INDUSTRY_ADAPTIVE, AiStrategy.DEFENSE);
    }

    @Test
    void aGameWithSeveralPeopleRunsAtTheChosenTempoWhoseDeadlinesAreFixed() {
        loginAsNewPlayer("Tempo Host");
        openWizard();
        test(find(IntegerField.class).withId("wizard-humans").single()).setValue(2);
        assertThat(tempo().getValue()).isEqualTo(GameTempo.LIVE);
        assertThat(tempo().getGenericDataView().getItems().toList()).containsExactly(GameTempo.values());
        String daily = TempoLabels.label(GameTempo.DAILY);
        assertThat(daily).startsWith("Täglich: ").contains("je Runde", "für den Letzten");
        test(tempo()).selectItem(daily);

        test(button("Zusammenfassung prüfen")).click();
        assertThat(elementText(openedDialog().getElement())).contains(daily);
        GameId id = create();

        assertThat(games.summaryOf(id).tempo()).isEqualTo(GameTempo.DAILY);
        RoundRules rules = store.load(id).orElseThrow().readState(state -> state.roundRules());
        assertThat(rules.roundLimit()).isEqualTo(java.time.Duration.ofHours(24));
        assertThat(rules.stragglerLimit()).isEqualTo(java.time.Duration.ofHours(6));
    }

    @Test
    void aGameForOnePersonHasNoTempo() {
        loginAsNewPlayer("Solo Host");
        openWizard();
        test(button("Zusammenfassung prüfen")).click();
        GameId id = create();

        assertThat(games.summaryOf(id).tempo()).isNull();
    }

    @Test
    void aPrivateGameWithoutTheHostStartsWithSectorForces() {
        loginAsNewPlayer("Lobby Host");
        String host = UserContext.currentPlayerId().orElseThrow();
        openWizard();
        Dialog wizard = openedDialog();
        assertThat(elementText(wizard.getElement())).contains("Private Partien sehen nur Eingeladene.");
        List<Component> cards = find(Component.class, wizard).withClassName("ruleset-card").all();
        assertThat(cards).hasSize(2);
        assertThat(elementText(cards.getFirst().getElement())).contains("SectorForces", "Regeln 1.0.0");
        assertThat(elementText(cards.get(1).getElement())).contains("Spaceward");
        assertThat(variants().getValue().defaultRef()).isEqualTo(RulesetRef.SECTOR_FORCES);

        test(find(Checkbox.class).withId("host-only").single()).click();
        test(button("Zusammenfassung prüfen")).click();
        GameId id = create();

        assertThat(games.summaryOf(id).visibility()).isEqualTo(GameVisibility.PRIVATE);
        assertThat(games.summaryOf(id).ruleset()).isEqualTo(RulesetRef.SECTOR_FORCES);
        assertThat(games.seatFor(id, host)).as("the host only hosts").isEmpty();
        assertThat(viewText()).contains("Privat", "SectorForces · Regeln 1.0.0");
    }

    @Test
    void allianceOptionsDependOnTheVariantAndOnEachOther() {
        openWizardWithEveryVariant();
        assertThat(checkbox("alliances-allowed").isEnabled()).as("SectorForces has no alliances").isFalse();

        chooseVariant(RulesetRef::isSpaceward);
        assertThat(checkbox("alliances-allowed").getValue()).isTrue();
        assertThat(checkbox("ai-alliances-allowed").getValue()).isTrue();
        assertEnabled(true, "alliance-victory-allowed", "ai-alliances-allowed");
        assertThat(percent().isEnabled()).isFalse();
        test(checkbox("alliance-victory-allowed")).click();
        assertThat(percent().isEnabled()).isTrue();

        test(checkbox("alliances-allowed")).click();
        assertEnabled(false, "alliance-victory-allowed", "ai-alliances-allowed");
        assertThat(checkbox("alliance-victory-allowed").getValue()).isFalse();
        assertThat(percent().isEnabled()).isFalse();

        test(checkbox("alliances-allowed")).click();
        assertEnabled(true, "alliance-victory-allowed", "ai-alliances-allowed");
        test(checkbox("alliance-victory-allowed")).click();
        assertThat(percent().isEnabled()).isTrue();
    }

    @Test
    void theSummaryNamesTheAllianceVictoryAndAiAlliances() {
        openWizardWithEveryVariant();
        chooseVariant(RulesetRef::isSpaceward);
        test(checkbox("alliance-victory-allowed")).click();

        test(button("Zusammenfassung prüfen")).click();
        List<String> lines = find(Component.class).withClassName("wizard-summary-line").all().stream()
                .map(line -> elementText(line.getElement())).toList();
        assertThat(lines)
                .anyMatch(line -> line.contains("Bündnissieg") && line.contains("70 %"))
                .anyMatch(line -> line.contains("KI-Bündnisse") && line.contains("aktiv"));
    }

    /** Every variant creatable, as the tester preview offered it; alliances belong to Spaceward. */
    private void openWizardWithEveryVariant() {
        loginAsNewPlayer("Commander");
        CreateGameWizardDialog.open(games, players, () -> { }, new RulesetCatalog(games.rulesets().definitions().stream()
                .map(entry -> new RulesetDefinition(entry.defaultRef(), entry.nameKey(), entry.descriptionKey(),
                        entry.supportedVersions(), true, entry.creatableVersions())).toList()));
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

    @SuppressWarnings("unchecked") // rationale: the wizard's group contains RulesetDefinition values.
    private RadioButtonGroup<RulesetDefinition> variants() {
        return find(RadioButtonGroup.class).withId("ruleset-selection").single();
    }

    private void chooseVariant(java.util.function.Predicate<RulesetRef> wanted) {
        RadioButtonGroup<RulesetDefinition> variants = variants();
        variants.setValue(variants.getListDataView().getItems()
                .filter(definition -> wanted.test(definition.defaultRef())).findFirst().orElseThrow());
    }

    @SuppressWarnings("unchecked") // rationale: the finder is not typed, the tempo field holds GameTempo.
    private ComboBox<GameTempo> tempo() {
        return find(ComboBox.class).withId("wizard-tempo").single();
    }

    private IntegerField aiPlayers() {
        return find(IntegerField.class).withId("wizard-ai").single();
    }

    @SuppressWarnings("unchecked") // rationale: the finder is not typed, the seat fields hold AiStrategy.
    private ComboBox<AiStrategy> strategy(int seat) {
        return find(ComboBox.class).withId("ai-strategy-" + seat).single();
    }

    private List<AiStrategy> choices(int seat) {
        return strategy(seat).getGenericDataView().getItems().toList();
    }

    private void choose(int seat, String label) {
        test(strategy(seat)).selectItem(label);
        assertThat(AiStrategyFields.label(strategy(seat).getValue())).isEqualTo(label);
    }

    private void assertSelection(int seat, AiStrategy expected) {
        assertThat(strategy(seat).getValue()).as("AI seat %d", seat).isEqualTo(expected);
    }

    private Checkbox checkbox(String id) {
        return find(Checkbox.class).withId(id).single();
    }

    private IntegerField percent() {
        return find(IntegerField.class).withId("alliance-victory-percent").single();
    }

    private void assertEnabled(boolean enabled, String... ids) {
        for (String id : ids) {
            assertThat(checkbox(id).isEnabled()).as(id).isEqualTo(enabled);
        }
    }
}
