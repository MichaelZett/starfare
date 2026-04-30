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
            return "%s G:%d P:%d".formatted(system.name(), system.garrison(), system.productionPerTurn());
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
                .filter(f -> f.arrivalTurn() > turn)
                .map(f -> new FleetView(
                        f.localNo(),
                        nameOf.apply(f.fromSystemId()),
                        nameOf.apply(f.toSystemId()),
                        f.ships(),
                        f.arrivalTurn() - turn,
                        f.globalId(),
                        f.fromSystemId(),
                        f.toSystemId(),
                        false
                ))
                .toList();
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
