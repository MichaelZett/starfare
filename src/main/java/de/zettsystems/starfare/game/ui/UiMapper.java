package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.game.values.FleetView;
import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.game.values.VisibleSystem;
import de.zettsystems.starfare.i18n.I18n;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

/**
 * Translates domain view state into UI-friendly strings and view models.
 */
public final class UiMapper {
    private UiMapper() {}

    public static String systemLabel(VisibleSystem system) {
        if (system.fullyVisible()) {
            Integer production = system.productionPerTurn();
            Integer routed = system.routedProduction();
            if (production != null && routed != null && routed > 0) {
                return "%s G:%d P:%d (%d frei)".formatted(system.name(), system.garrison(),
                        production, Math.max(0, production - routed));
            }
            return "%s G:%d P:%d".formatted(system.name(), system.garrison(), production);
        }
        if (system.approximate()) {
            return "%s ~G:%d".formatted(system.name(), system.garrison());
        }
        if (system.garrison() != null) {
            return "%s G:%d".formatted(system.name(), system.garrison());
        }
        return system.name();
    }

    public static String systemTooltip(VisibleSystem system, int currentTurn) {
        if (system.fullyVisible()) {
            return I18n.t(UiTexts.MAP_TOOLTIP_LIVE, system.name(), currentTurn);
        }
        if (system.lastSeenTurn() != null) {
            return I18n.t(UiTexts.MAP_TOOLTIP_LAST_SEEN, system.name(), system.lastSeenTurn());
        }
        return I18n.t(UiTexts.MAP_TOOLTIP_NO_SIGHT, system.name());
    }

    public static List<FleetView> toFleetViews(PlayerViewState view) {
        int turn = view.turn();
        Map<Integer, String> systemNames = view.systems().stream()
                .collect(Collectors.toMap(VisibleSystem::id, VisibleSystem::name, (a, _) -> a));
        IntFunction<String> nameOf = id -> systemNames.getOrDefault(id, "?");
        return view.ownFleets().stream()
                .filter(f -> f.arrivalTurn() > turn || f.journey() != null)
                .map(f -> new FleetView(
                        f.localNo(),
                        nameOf.apply(f.fromSystemId()),
                        nameOf.apply(destinationOf(f)),
                        f.ships(),
                        etaOf(f, view),
                        f.globalId(),
                        f.fromSystemId(),
                        destinationOf(f),
                        false
                ))
                .toList();
    }

    private static int destinationOf(de.zettsystems.starfare.game.values.Fleet fleet) {
        var journey = fleet.journey(); return journey == null ? fleet.toSystemId() : journey.destination();
    }
    private static int etaOf(de.zettsystems.starfare.game.values.Fleet fleet, PlayerViewState view) {
        var journey = fleet.journey();
        if (journey != null && journey.blocked()) { return -1; }
        int arrival = journey == null ? fleet.arrivalTurn() : journey.finalArrivalTurn();
        return Math.max(0, arrival - view.turn()) + (view.waitingFleetIds().contains(fleet.globalId()) ? 1 : 0);
    }

    public static List<FleetView> standingAsFleetRows(PlayerViewState view) {
        if (view.standingOrders() == null || view.standingOrders().isEmpty()) {
            return List.of();
        }
        return view.standingOrders().stream()
                .map(so -> new FleetView(
                        0,
                        so.fromSystem(),
                        so.toSystem(),
                        so.productionPerTurn(),
                        -1,
                        -1,
                        so.fromSystemId(),
                        so.toSystemId(),
                        true
                ))
                .toList();
    }
}
