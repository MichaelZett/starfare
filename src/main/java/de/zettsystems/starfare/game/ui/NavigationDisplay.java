package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.html.Span;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.i18n.I18n;

/** Itinerary and station status, using only the player's immutable view. */
final class NavigationDisplay {
    private NavigationDisplay() { }
    static void addJourney(HasComponents target, Fleet fleet, PlayerViewState view) {
        var journey = fleet.journey();
        if (journey == null) { return; }
        String route = String.join(" → ", journey.stations().stream().map(id -> name(view, id)).toList());
        Span itinerary = new Span(route); itinerary.addClassName("navigation-itinerary");
        target.add(itinerary);
        target.add(new Span(status(fleet, view)));
    }
    static String status(Fleet fleet, PlayerViewState view) {
        var journey = fleet.journey();
        if (journey == null) { return ""; }
        String key = switch (journey.phase()) {
            case FLYING -> UiTexts.NAV_FLYING;
            case STATION -> UiTexts.NAV_STATION;
            case BLOCKED -> UiTexts.NAV_BLOCKED;
        };
        return I18n.t(key, name(view, fleet.toSystemId()), journey.readyTurn() + (view.waitingFleetIds().contains(fleet.globalId()) ? 1 : 0));
    }
    private static String name(PlayerViewState view, int id) {
        return view.systems().stream().filter(system -> system.id() == id).map(system -> system.name()).findFirst().orElse("?");
    }
}
