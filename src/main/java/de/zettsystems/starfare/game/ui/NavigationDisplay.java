package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.game.values.VisibleSystem;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.details.Details;
import java.util.Optional;
import com.vaadin.flow.component.html.Span;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.i18n.I18n;

/** Itinerary and station status, using only the player's immutable view. */
final class NavigationDisplay {
    private NavigationDisplay() { }
    static Optional<Details> addJourney(HasComponents target, Fleet fleet, PlayerViewState view) {
        var journey = fleet.journey();
        if (journey != null) { target.add(new Span(status(fleet, view))); }
        addItinerary(target, fleet, view);
        if (journey == null) { return Optional.empty(); }
        var itinerary = new com.vaadin.flow.component.html.Div();
        itinerary.add(new Span(I18n.t(de.zettsystems.starfare.navigation.ui.RouteKeys.RULES)));
        var details = new Details(I18n.t(UiTexts.MAP_FLEET_DETAILS), itinerary);
        target.add(details);
        return Optional.of(details);
    }
    static de.zettsystems.starfare.navigation.ui.RouteStrip addItinerary(HasComponents target, Fleet fleet, PlayerViewState view) {
        var state = de.zettsystems.starfare.navigation.values.RouteState.forFleet(fleet, view.waitingFleetIds().contains(fleet.globalId()));
        var itinerary = new de.zettsystems.starfare.navigation.ui.RouteStrip(state, view, fleet.ownerId());
        itinerary.addClassName("navigation-itinerary");
        target.add(itinerary);
        return itinerary;
    }
    static String status(Fleet fleet, PlayerViewState view) {
        var journey = fleet.journey();
        if (journey == null) { return ""; }
        if (journey.evacuating()) { return I18n.t(journey.blocked() ? UiTexts.NAV_RETURN_BLOCKED : UiTexts.NAV_RETURN, name(view, fleet.toSystemId())); }
        if (journey.stationed()) { return I18n.t(UiTexts.NAV_ALLIED_STATION, name(view, fleet.toSystemId())); }
        String key = switch (journey.phase()) {
            case FLYING -> UiTexts.NAV_FLYING;
            case PARTNER -> UiTexts.NAV_ALLIED_STATION;
            case STATION -> UiTexts.NAV_STATION;
            case BLOCKED -> UiTexts.NAV_BLOCKED;
        };
        var state = de.zettsystems.starfare.navigation.values.RouteState.forFleet(fleet, view.waitingFleetIds().contains(fleet.globalId()));
        Integer next = state.nextTurn();
        int nextTurn = next == null ? 0 : next;
        String text = I18n.t(key, name(view, fleet.toSystemId()), nextTurn);
        if (journey.flying()) { text += " · " + I18n.t(UiTexts.MAP_COLUMN_ARRIVAL_TURN) + ": " + next; }
        if (journey.hasNextLeg() && !journey.flying()) {
            text += " · " + I18n.t(de.zettsystems.starfare.navigation.ui.RouteKeys.NEXT,
                    name(view, journey.stations().get(journey.legIndex() + 1)));
        }
        return text;
    }
    private static String name(PlayerViewState view, int id) {
        return view.systems().stream().filter(system -> system.id() == id).map(VisibleSystem::name).findFirst().orElse("?");
    }
}
