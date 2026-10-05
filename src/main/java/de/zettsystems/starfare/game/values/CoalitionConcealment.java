package de.zettsystems.starfare.game.values;

import de.zettsystems.starfare.report.values.TurnEvent;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Presentation-only concealment of surviving allied contingents and post-combat industry. */
public final class CoalitionConcealment {
    private CoalitionConcealment() { }
    public static PlayerViewState hide(PlayerViewState view, Set<Integer> pending) {
        var report = view.report();
        if (report == null || pending.isEmpty()) { return view; }
        var battles = report.events().stream().flatMap(event -> event instanceof TurnEvent.CoalitionBattle battle
                ? java.util.stream.Stream.of(battle) : java.util.stream.Stream.empty())
                .filter(battle -> pending.contains(battle.systemId())).toList();
        Set<Integer> systems = battles.stream().map(TurnEvent.CoalitionBattle::systemId).collect(Collectors.toSet());
        if (systems.isEmpty()) { return view; }
        List<VisibleSystem> concealed = view.systems().stream().map(system -> systems.contains(system.id())
                ? new VisibleSystem(system.id(), system.name(), system.x(), system.y(), null, null, null,
                        false, null, null, false, null, List.of()) : system).toList();
        Set<Integer> participants = battles.stream().flatMap(battle -> battle.fleetIds().stream()).collect(Collectors.toSet());
        var fleets = view.ownFleets().stream().filter(fleet -> fleet.inFlight() || fleet.evacuating() || !participants.contains(fleet.globalId())).toList();
        return new PlayerViewState(view.turn(), view.players(), concealed, fleets, report, view.gameOver(), view.winnerId(),
                view.plannedOrders(), view.standingOrders(), EmpireStats.NONE, view.waitingFleetIds(), view.battlePresentationEnabled(), view.roundStatus(), view.outcome());
    }
}
