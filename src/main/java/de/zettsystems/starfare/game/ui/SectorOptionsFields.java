package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.game.values.CosmicEvent;
import de.zettsystems.starfare.game.values.EventFrequency;
import de.zettsystems.starfare.game.values.SectorOptions;
import de.zettsystems.starfare.i18n.I18n;

import java.util.EnumSet;
import java.util.Set;

/**
 * SectorForces options after Galactic Empire (1983): random events with a master switch and one
 * switch per event, a start garrison balanced by position and a round limit with a score.
 */
final class SectorOptionsFields extends VerticalLayout {
    static final int DEFAULT_ROUND_LIMIT = 60;

    private final RadioButtonGroup<EventFrequency> frequency =
            new RadioButtonGroup<>(I18n.t(UiTexts.SECTOR_EVENTS));
    private final CheckboxGroup<CosmicEvent> events = new CheckboxGroup<>(I18n.t(UiTexts.SECTOR_EVENTS_CHOICE));
    private final Checkbox positionalBalance = new Checkbox(I18n.t(UiTexts.SECTOR_POSITIONAL_BALANCE));
    private final Checkbox roundLimitEnabled = new Checkbox(I18n.t(UiTexts.SECTOR_ROUND_LIMIT));
    private final IntegerField roundLimit = new IntegerField(I18n.t(UiTexts.SECTOR_ROUND_LIMIT_LAST));

    SectorOptionsFields() {
        setPadding(false);
        frequency.setId("sector-event-frequency");
        frequency.setItems(EventFrequency.values());
        frequency.setItemLabelGenerator(SectorOptionsFields::label);
        frequency.setValue(EventFrequency.OFF);
        events.setId("sector-events");
        events.setItems(CosmicEvent.values());
        events.setItemLabelGenerator(SectorOptionsFields::label);
        events.setValue(EnumSet.allOf(CosmicEvent.class));
        events.setItemEnabledProvider(event -> event != CosmicEvent.PIRATES
                || events.getValue().contains(CosmicEvent.SUPERNOVA));
        events.setHelperText(I18n.t(UiTexts.SECTOR_EVENTS_HINT));
        positionalBalance.setId("sector-positional-balance");
        positionalBalance.setHelperText(I18n.t(UiTexts.SECTOR_POSITIONAL_BALANCE_HINT));
        roundLimitEnabled.setId("sector-round-limit-enabled");
        roundLimitEnabled.setHelperText(I18n.t(UiTexts.SECTOR_ROUND_LIMIT_HINT));
        roundLimit.setId("sector-round-limit");
        roundLimit.setMin(SectorOptions.MIN_ROUND_LIMIT);
        roundLimit.setMax(SectorOptions.MAX_ROUND_LIMIT);
        roundLimit.setStepButtonsVisible(true);
        roundLimit.setValue(DEFAULT_ROUND_LIMIT);
        frequency.addValueChangeListener(_ -> refresh());
        // refreshAll() would rebuild the boxes and drop the selection; only the pirates depend on another box.
        events.addValueChangeListener(_ -> events.getDataProvider().refreshItem(CosmicEvent.PIRATES));
        roundLimitEnabled.addValueChangeListener(_ -> refresh());
        add(frequency, events, positionalBalance, roundLimitEnabled, roundLimit);
        refresh();
    }

    private void refresh() {
        events.setEnabled(frequency.getValue() != EventFrequency.OFF);
        roundLimit.setEnabled(roundLimitEnabled.getValue());
    }

    SectorOptions chosen() {
        EventFrequency chosenFrequency = frequency.getValue() == null ? EventFrequency.OFF : frequency.getValue();
        Set<CosmicEvent> chosenEvents = events.getValue().isEmpty()
                ? EnumSet.noneOf(CosmicEvent.class) : EnumSet.copyOf(events.getValue());
        Integer last = roundLimit.getValue();
        int limit = 0;
        if (Boolean.TRUE.equals(roundLimitEnabled.getValue())) {
            limit = last == null ? DEFAULT_ROUND_LIMIT : last;
        }
        return new SectorOptions(chosenFrequency, chosenEvents, positionalBalance.getValue(), limit);
    }

    static String label(EventFrequency value) {
        return I18n.t(switch (value) {
            case OFF -> UiTexts.SECTOR_EVENTS_OFF;
            case RARE -> UiTexts.SECTOR_EVENTS_RARE;
            case NORMAL -> UiTexts.SECTOR_EVENTS_NORMAL;
        });
    }

    static String label(CosmicEvent event) {
        return I18n.t(switch (event) {
            case COSMIC_STORM -> UiTexts.SECTOR_EVENT_STORM;
            case SPACETIME_QUAKE -> UiTexts.SECTOR_EVENT_SPACETIME_QUAKE;
            case DISAPPEARANCE -> UiTexts.SECTOR_EVENT_DISAPPEARANCE;
            case SPACEPORT_QUAKE -> UiTexts.SECTOR_EVENT_SPACEPORT_QUAKE;
            case SUPERNOVA -> UiTexts.SECTOR_EVENT_SUPERNOVA;
            case PIRATES -> UiTexts.SECTOR_EVENT_PIRATES;
        });
    }
}
