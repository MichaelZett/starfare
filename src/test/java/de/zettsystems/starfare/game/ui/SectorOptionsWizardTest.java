package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.game.domain.GameState;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.AbstractBrowserlessTest;
import de.zettsystems.starfare.game.application.GameSessionStore;
import de.zettsystems.starfare.game.values.CosmicEvent;
import de.zettsystems.starfare.game.values.EventFrequency;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.RulesetDefinition;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.game.values.SectorOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** SectorForces options after Galactic Empire: master switch, single events, balance and round limit. */
class SectorOptionsWizardTest extends AbstractBrowserlessTest {

    @Autowired
    private GameSessionStore store;

    private final List<GameId> created = new ArrayList<>();

    @AfterEach
    void removeCreatedGames() {
        created.forEach(store::delete);
    }

    @Test
    void theMasterSwitchUnlocksTheSingleEventsAndPiratesNeedTheSupernova() {
        loginAsNewPlayer("Sector Host");
        openWizard();
        assertThat(frequency().getValue()).isEqualTo(EventFrequency.OFF);
        assertThat(events().isEnabled()).as("everything off by default").isFalse();
        assertThat(events().getValue()).containsExactlyInAnyOrder(CosmicEvent.values());
        assertThat(roundLimit().isEnabled()).isFalse();

        frequency().setValue(EventFrequency.NORMAL);
        assertThat(events().isEnabled()).isTrue();
        assertThat(events().getItemEnabledProvider().test(CosmicEvent.PIRATES)).isTrue();
        events().deselect(CosmicEvent.SUPERNOVA);
        assertThat(events().getItemEnabledProvider().test(CosmicEvent.PIRATES)).isFalse();
        assertThat(events().getValue()).as("the other choices stay").hasSize(CosmicEvent.values().length - 1);

        test(checkbox("sector-round-limit-enabled")).click();
        assertThat(roundLimit().isEnabled()).isTrue();

        chooseVariant(RulesetRef::isSpaceward);
        assertThat(find(RadioButtonGroup.class).withId("sector-event-frequency").all())
                .as("Spaceward has no sector options").isEmpty();
        chooseVariant(ref -> ref == RulesetRef.SECTOR_FORCES);
        assertThat(test(frequency()).isUsable()).isTrue();
    }

    @Test
    void theSummaryNamesTheActiveEventsAndTheGameKeepsTheChoice() {
        loginAsNewPlayer("Sector Host");
        openWizard();
        frequency().setValue(EventFrequency.RARE);
        events().setValue(EnumSet.of(CosmicEvent.COSMIC_STORM, CosmicEvent.SUPERNOVA, CosmicEvent.PIRATES));
        test(checkbox("sector-positional-balance")).click();
        test(checkbox("sector-round-limit-enabled")).click();
        test(roundLimit()).setValue(40);
        test(checkbox("host-only")).click();

        test(button("Zusammenfassung prüfen")).click();
        String summary = elementText(find(Component.class).withId("wizard-summary-events").single().getElement());
        assertThat(summary).contains("Kosmische Stürme", "Supernova", "Piraten").doesNotContain("Raumhafenbeben");
        assertThat(find(Component.class).withClassName("wizard-summary-group").all()).hasSize(5);
        assertThat(elementText(openedDialog().getElement())).contains("Selten", "nach Runde 40");

        GameId id = create();
        SectorOptions saved = registry.readState(id, GameState::sectorOptions);
        assertThat(saved).isEqualTo(new SectorOptions(EventFrequency.RARE,
                EnumSet.of(CosmicEvent.COSMIC_STORM, CosmicEvent.SUPERNOVA, CosmicEvent.PIRATES), true, 40));
    }

    @Test
    void untouchedOptionsCreateAPlainSectorForcesGame() {
        loginAsNewPlayer("Sector Host");
        openWizard();
        test(checkbox("host-only")).click();
        test(button("Zusammenfassung prüfen")).click();
        GameId id = create();
        SectorOptions saved = registry.readState(id, GameState::sectorOptions);
        assertThat(saved.isDefault()).isTrue();
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

    @SuppressWarnings("unchecked") // rationale: the master switch holds EventFrequency values.
    private RadioButtonGroup<EventFrequency> frequency() {
        return find(RadioButtonGroup.class).withId("sector-event-frequency").single();
    }

    @SuppressWarnings("unchecked") // rationale: the event choice holds CosmicEvent values.
    private CheckboxGroup<CosmicEvent> events() {
        return find(CheckboxGroup.class).withId("sector-events").single();
    }

    private IntegerField roundLimit() {
        return find(IntegerField.class).withId("sector-round-limit").single();
    }

    private Checkbox checkbox(String id) {
        return find(Checkbox.class).withId(id).single();
    }

    @SuppressWarnings("unchecked") // rationale: the wizard's group contains RulesetDefinition values.
    private void chooseVariant(java.util.function.Predicate<RulesetRef> wanted) {
        RadioButtonGroup<RulesetDefinition> variants = find(RadioButtonGroup.class).withId("ruleset-selection").single();
        variants.setValue(variants.getListDataView().getItems()
                .filter(definition -> wanted.test(definition.defaultRef())).findFirst().orElseThrow());
    }
}
