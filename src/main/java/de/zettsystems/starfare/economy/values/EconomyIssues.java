package de.zettsystems.starfare.economy.values;

import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.game.values.VisibleSystem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Derives grouped hints from the already-authorized saved forecast, without service calls. */
public final class EconomyIssues {
    private EconomyIssues() { }
    public enum Kind { DELIVERY, METAL, DEPOSIT, LABOR }
    public record Issue(VisibleSystem system, List<Kind> reasons) {
        public Issue { reasons = List.copyOf(reasons); }
    }
    public static List<Issue> forView(PlayerViewState view, int player, Set<Integer> pendingBattles) {
        Set<Integer> blocked = view.ownFleets().stream().filter(fleet -> fleet.ownerId() == player)
                .filter(fleet -> { var journey = fleet.journey(); return journey != null && journey.blocked(); })
                .map(fleet -> fleet.toSystemId()).collect(java.util.stream.Collectors.toSet());
        return forSystems(view.systems(), player, pendingBattles, blocked);
    }
    public static List<Issue> forSystems(List<VisibleSystem> systems, int player, Set<Integer> pendingBattles, Set<Integer> blocked) {
        if (!pendingBattles.isEmpty()) { return List.of(); }
        return systems.stream().filter(system -> system.fullyVisible() && Objects.equals(system.ownerId(), player))
                .map(system -> new Issue(system, reasons(system, blocked.contains(system.id()))))
                .filter(issue -> !issue.reasons().isEmpty())
                .sorted(Comparator.comparing((Issue issue) -> issue.system().name()).thenComparingInt(issue -> issue.system().id()))
                .toList();
    }
    private static List<Kind> reasons(VisibleSystem system, boolean blocked) {
        IndustryView industry = system.industry();
        List<Kind> result = new ArrayList<>();
        if (blocked || (industry != null && industry.deliveryBottleneck())) { result.add(Kind.DELIVERY); }
        if (industry == null) { return result; }
        MetalView metal = industry.metal();
        if (metal != null) {
            if (metal.actualShips() < metal.plannedShips()) { result.add(Kind.METAL); }
            if (metal.miningPoints() > 0 && metal.remaining() < (long) metal.miningPoints() * metal.yieldPerPoint()) { result.add(Kind.DEPOSIT); }
        }
        ColonyView colony = industry.colony();
        if (colony != null && colony.laborLimited() && colony.labor() < industry.capacity()) { result.add(Kind.LABOR); }
        return result;
    }
}
