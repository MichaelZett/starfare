package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import de.zettsystems.starfare.AbstractBrowserlessTest;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.application.GameSessionStore;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.ComputerStrength;
import de.zettsystems.starfare.game.values.CosmicEvent;
import de.zettsystems.starfare.game.values.EventFrequency;
import de.zettsystems.starfare.game.values.GalaxyLayout;
import de.zettsystems.starfare.game.values.GalaxySize;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.GameTempo;
import de.zettsystems.starfare.game.values.GameVisibility;
import de.zettsystems.starfare.game.values.RandomnessLevel;
import de.zettsystems.starfare.game.values.RoundRules;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.RulesetDefinition;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.game.values.SectorOptions;
import de.zettsystems.starfare.game.values.VictoryRules;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/** The one-page dialog "new game": its choices, how they depend on each other and what the game keeps. */
class NewGameDialogTest extends AbstractBrowserlessTest {

    @Autowired
    private GameSessionStore store;

    private final List<GameId> created = new ArrayList<>();

    @AfterEach
    void removeCreatedGames() {
        created.forEach(store::delete);
    }

    @Test
    void theProposedGameAgainstTheComputerStartsRightAway() {
        loginAsNewPlayer("Solo Host");
        String host = UserContext.currentPlayerId().orElseThrow();
        openDialog();
        assertThat(players().getValue()).isEqualTo(3);
        assertThat(computers().getValue()).isEqualTo(2);
        assertThat(strength().getValue()).isEqualTo(ComputerStrength.NORMAL);
        assertThat(find(ComboBox.class).withId("wizard-tempo").all()).as("no deadlines without a second person").isEmpty();
        assertThat(size().getValue()).isEqualTo(GalaxySize.MEDIUM);
        assertThat(randomness().getValue()).isEqualTo(RandomnessLevel.LOW);
        assertThat(variants().getValue().defaultRef()).isEqualTo(RulesetRef.SECTOR_FORCES);
        assertThat(details().isOpened()).isFalse();

        GameId id = create();

        assertThat(getCurrentView()).as("every other seat is a computer").isInstanceOf(MainView.class);
        assertThat(games.summaryOf(id).started()).isTrue();
        assertThat(games.summaryOf(id).visibility()).isEqualTo(GameVisibility.PUBLIC);
        assertThat(games.summaryOf(id).tempo()).isNull();
        assertThat(games.seatFor(id, host)).hasValue(1);
        GameSetup setup = setupOf(id);
        assertThat(setup.humanPlayers()).isOne();
        assertThat(setup.aiPlayers()).isEqualTo(2);
        assertThat(setup.systemCount()).isEqualTo(24);
        assertThat(setup.maxProduction()).isEqualTo(10);
        assertThat(registry.readState(id, GameState::combatRandomnessPercent)).isEqualTo(5);
        assertThat(registry.readState(id, GameState::sectorOptions).eventFrequency()).isEqualTo(EventFrequency.RARE);
    }

    @Test
    void thePlayerCountKeepsTheComputersAndLocksTooSmallGalaxies() {
        loginAsNewPlayer("Counting Host");
        openDialog();
        test(size()).selectItem("Klein");

        test(players()).selectItem("5");
        assertThat(computers().getValue()).as("still alone against the computer").isEqualTo(4);
        test(computers()).selectItem("2");
        assertThat(tempo().isVisible()).as("three people play against deadlines").isTrue();
        test(players()).selectItem("4");
        assertThat(computers().getValue()).as("a mixed table keeps its computers").isEqualTo(2);

        test(players()).selectItem("10");
        assertThat(size().getValue()).as("sixteen systems are too few for ten empires").isEqualTo(GalaxySize.MEDIUM);
        assertThat(size().getItemEnabledProvider().test(GalaxySize.SMALL)).isFalse();
        test(computers()).selectItem("0");
        assertThat(strength().isEnabled()).isFalse();
    }

    @Test
    void aTableOfPeoplePlaysAtTheChosenTempoAndCanBeInvitationOnly() {
        loginAsNewPlayer("Tempo Host");
        openDialog();
        test(computers()).selectItem("1");
        test(strength()).selectItem("Anspruchsvoll");
        assertThat(tempo().getValue()).isEqualTo(GameTempo.DEFAULT);
        test(tempo()).selectItem(TempoLabels.label(GameTempo.DAILY));
        test(find(Checkbox.class).withId("wizard-invite-only").single()).click();
        layout().setValue(GalaxyLayout.RANDOM);
        test(size()).selectItem("Groß");
        test(find(ComboBox.class).withId("max-production").single()).selectItem("15");

        GameId id = create();

        assertThat(getCurrentView()).as("a free seat opens the waiting room").isInstanceOf(WaitingRoomView.class);
        assertThat(games.summaryOf(id).visibility()).isEqualTo(GameVisibility.PRIVATE);
        assertThat(games.summaryOf(id).tempo()).isEqualTo(GameTempo.DAILY);
        RoundRules rules = store.load(id).orElseThrow().readState(GameState::roundRules);
        assertThat(rules.roundLimit()).isEqualTo(Duration.ofHours(24));
        GameSetup setup = setupOf(id);
        assertThat(setup.humanPlayers()).isEqualTo(2);
        assertThat(setup.computerStrength()).isEqualTo(ComputerStrength.HARD);
        assertThat(setup.galaxyLayout()).isEqualTo(GalaxyLayout.RANDOM);
        assertThat(setup.systemCount()).isEqualTo(40);
        assertThat(setup.maxProduction()).isEqualTo(15);
    }

    @Test
    void eventsAndTheRoundLimitAreClassicDetailsThatFollowTheRandomness() {
        loginAsNewPlayer("Sector Host");
        openDialog();
        details().setOpened(true);
        assertThat(events().getValue()).containsExactlyInAnyOrder(CosmicEvent.values());
        events().deselect(CosmicEvent.SUPERNOVA);
        assertThat(events().getItemEnabledProvider().test(CosmicEvent.PIRATES)).as("pirates need the supernova").isFalse();
        events().deselect(CosmicEvent.SPACEPORT_QUAKE);
        assertThat(roundLimit().isEnabled()).isFalse();
        test(find(Checkbox.class).withId("sector-round-limit-enabled").single()).click();
        test(roundLimit()).setValue(40);

        randomness().setValue(RandomnessLevel.NONE);
        assertThat(find(CheckboxGroup.class).withId("sector-events").all()).as("no events without chance").isEmpty();
        randomness().setValue(RandomnessLevel.HIGH);
        assertThat(events().isVisible()).isTrue();
        chooseVariant(RulesetRef::isSpaceward);
        assertThat(find(Details.class).withId("setup-details").all()).as("extended games have none of these").isEmpty();
        chooseVariant(ref -> ref == RulesetRef.SECTOR_FORCES);
        test(computers()).selectItem("1");

        GameId id = create();

        assertThat(registry.readState(id, GameState::sectorOptions)).isEqualTo(new SectorOptions(EventFrequency.FREQUENT,
                EnumSet.of(CosmicEvent.COSMIC_STORM, CosmicEvent.SPACETIME_QUAKE, CosmicEvent.DISAPPEARANCE,
                        CosmicEvent.PIRATES), true, 40));
        assertThat(registry.readState(id, GameState::combatRandomnessPercent)).isEqualTo(20);
    }

    @Test
    void anExtendedGameAllowsAlliancesAndAnAllianceVictory() {
        loginAsNewPlayer("Extended Host");
        openDialogWithEveryVariant();
        chooseVariant(RulesetRef::isSpaceward);
        test(computers()).selectItem("1");

        GameId id = create();

        assertThat(games.summaryOf(id).ruleset()).isEqualTo(RulesetCatalog.ECONOMY_CANDIDATE);
        assertThat(registry.readState(id, GameState::victoryRules)).isEqualTo(new VictoryRules(70, true, true, 70, true));
    }

    @Test
    void theNameCanBeRolledAgainButNotLeftEmpty() {
        loginAsNewPlayer("Naming Host");
        openDialog();
        TextField name = find(TextField.class).withId("wizard-game-name").single();
        String first = name.getValue();
        assertThat(first).isNotBlank();
        for (int roll = 0; roll < 5 && name.getValue().equals(first); roll++) {
            test(find(Button.class).withId("roll-game-name").single()).click();
        }
        assertThat(name.getValue()).isNotEqualTo(first);

        test(name).setValue(" ");
        List<GameId> before = games.listGames();
        test(button("Spiel anlegen")).click();
        assertThat(name.isInvalid()).isTrue();
        assertThat(games.listGames()).containsExactlyInAnyOrderElementsOf(before);
    }

    private void openDialog() {
        navigate(LobbyView.class);
        test(find(Button.class).withText("Neues Spiel").atIndex(1)).click();
    }

    /** Every variant creatable, as the tester preview offered it. */
    private void openDialogWithEveryVariant() {
        NewGameDialog.open(games, players, () -> { }, new RulesetCatalog(games.rulesets().definitions().stream()
                .map(entry -> new RulesetDefinition(entry.defaultRef(), entry.nameKey(), entry.descriptionKey(),
                        entry.supportedVersions(), true, entry.creatableVersions())).toList()));
    }

    @Autowired
    private de.zettsystems.starfare.auth.application.PlayerDirectory players;

    private GameId create() {
        List<GameId> before = games.listGames();
        test(button("Spiel anlegen")).click();
        runPendingUiAccess();
        GameId id = games.listGames().stream().filter(game -> !before.contains(game)).findFirst().orElseThrow();
        created.add(id);
        return id;
    }

    private GameSetup setupOf(GameId id) {
        return registry.readState(id, state -> state.originalSetup().orElseThrow());
    }

    private void chooseVariant(Predicate<RulesetRef> wanted) {
        RadioButtonGroup<RulesetDefinition> variants = variants();
        variants.setValue(variants.getListDataView().getItems()
                .filter(definition -> wanted.test(definition.defaultRef())).findFirst().orElseThrow());
    }

    @SuppressWarnings("unchecked") // rationale: the finder is not typed; the rules group holds RulesetDefinition.
    private RadioButtonGroup<RulesetDefinition> variants() {
        return find(RadioButtonGroup.class).withId("ruleset-selection").single();
    }

    @SuppressWarnings("unchecked") // rationale: the finder is not typed; the size group holds GalaxySize.
    private RadioButtonGroup<GalaxySize> size() {
        return find(RadioButtonGroup.class).withId("galaxy-size").single();
    }

    @SuppressWarnings("unchecked") // rationale: the finder is not typed; the layout group holds GalaxyLayout.
    private RadioButtonGroup<GalaxyLayout> layout() {
        return find(RadioButtonGroup.class).withId("galaxy-layout").single();
    }

    @SuppressWarnings("unchecked") // rationale: the finder is not typed; the chance group holds RandomnessLevel.
    private RadioButtonGroup<RandomnessLevel> randomness() {
        return find(RadioButtonGroup.class).withId("randomness").single();
    }

    @SuppressWarnings("unchecked") // rationale: the finder is not typed; the count holds Integer values.
    private ComboBox<Integer> players() {
        return find(ComboBox.class).withId("wizard-players").single();
    }

    @SuppressWarnings("unchecked") // rationale: the finder is not typed; the count holds Integer values.
    private ComboBox<Integer> computers() {
        return find(ComboBox.class).withId("wizard-ai").single();
    }

    @SuppressWarnings("unchecked") // rationale: the finder is not typed; the strength holds ComputerStrength.
    private ComboBox<ComputerStrength> strength() {
        return find(ComboBox.class).withId("wizard-strength").single();
    }

    @SuppressWarnings("unchecked") // rationale: the finder is not typed; the tempo field holds GameTempo.
    private ComboBox<GameTempo> tempo() {
        return find(ComboBox.class).withId("wizard-tempo").single();
    }

    @SuppressWarnings("unchecked") // rationale: the event choice holds CosmicEvent values.
    private CheckboxGroup<CosmicEvent> events() {
        return find(CheckboxGroup.class).withId("sector-events").single();
    }

    private IntegerField roundLimit() {
        return find(IntegerField.class).withId("sector-round-limit").single();
    }

    private Details details() {
        return find(Details.class).withId("setup-details").single();
    }
}
