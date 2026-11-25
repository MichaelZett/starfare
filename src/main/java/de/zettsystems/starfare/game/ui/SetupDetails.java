package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.game.values.CosmicEvent;
import de.zettsystems.starfare.game.values.SectorOptions;
import de.zettsystems.starfare.i18n.I18n;

import java.util.EnumSet;
import java.util.Set;

/**
 * Collapsed details of a classic game after Galactic Empire (1983): which cosmic events may strike and a round
 * limit with a score. How often events strike follows the chosen randomness.
 */
final class SetupDetails extends Details {
    static final int DEFAULT_ROUND_LIMIT = 60;

    private final CheckboxGroup<CosmicEvent> events = new CheckboxGroup<>(I18n.t(UiTexts.SECTOR_EVENTS_CHOICE));
    private final Checkbox roundLimitEnabled = new Checkbox(I18n.t(UiTexts.SECTOR_ROUND_LIMIT));
    private final IntegerField roundLimit = new IntegerField(I18n.t(UiTexts.SECTOR_ROUND_LIMIT_LAST));

    SetupDetails() {
        super(I18n.t(UiTexts.SETUP_SECTION_DETAILS));
        addClassName("setup-details");
        setId("setup-details");
        events.setId("sector-events");
        events.setItems(CosmicEvent.values());
        events.setItemLabelGenerator(SetupDetails::label);
        events.setValue(EnumSet.allOf(CosmicEvent.class));
        events.setItemEnabledProvider(event -> event != CosmicEvent.PIRATES
                || events.getValue().contains(CosmicEvent.SUPERNOVA));
        events.setHelperText(I18n.t(UiTexts.SECTOR_EVENTS_HINT));
        // refreshAll() would rebuild the boxes and drop the selection; only the pirates depend on another box.
        events.addValueChangeListener(_ -> events.getDataProvider().refreshItem(CosmicEvent.PIRATES));
        roundLimitEnabled.setId("sector-round-limit-enabled");
        roundLimitEnabled.setHelperText(I18n.t(UiTexts.SECTOR_ROUND_LIMIT_HINT));
        roundLimit.setId("sector-round-limit");
        roundLimit.setMin(SectorOptions.MIN_ROUND_LIMIT);
        roundLimit.setMax(SectorOptions.MAX_ROUND_LIMIT);
        roundLimit.setStepButtonsVisible(true);
        roundLimit.setValue(DEFAULT_ROUND_LIMIT);
        roundLimit.setEnabled(false);
        roundLimitEnabled.addValueChangeListener(event -> roundLimit.setEnabled(Boolean.TRUE.equals(event.getValue())));
        Div content = new Div(events, roundLimitEnabled, roundLimit);
        content.addClassName("setup-details-content");
        add(content);
    }

    /** Single events only matter when the randomness lets events strike at all. */
    void showEvents(boolean shown) {
        events.setVisible(shown);
    }

    Set<CosmicEvent> chosenEvents() {
        return events.getValue().isEmpty() ? EnumSet.noneOf(CosmicEvent.class) : EnumSet.copyOf(events.getValue());
    }

    /** The last round of the game, 0 without a limit. */
    int chosenRoundLimit() {
        if (!Boolean.TRUE.equals(roundLimitEnabled.getValue())) {
            return 0;
        }
        Integer last = roundLimit.getValue();
        return last == null ? DEFAULT_ROUND_LIMIT : last;
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
