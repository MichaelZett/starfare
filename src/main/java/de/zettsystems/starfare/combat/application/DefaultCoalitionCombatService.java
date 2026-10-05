package de.zettsystems.starfare.combat.application;

import de.zettsystems.starfare.combat.domain.CoalitionResolver;
import de.zettsystems.starfare.combat.domain.ProportionalAllocation;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.navigation.application.NavigationService;
import de.zettsystems.starfare.report.application.ReportService;
import de.zettsystems.starfare.report.values.CoalitionSide;
import de.zettsystems.starfare.report.values.TurnEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntToDoubleFunction;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/** Spaceward-only arrivals: one battle per place, including already stationed allied forces. */
@Service
public class DefaultCoalitionCombatService implements CoalitionCombatService {
    private final ReportService reports;
    private final NavigationService navigation;
    private final IntToDoubleFunction roll;

    @org.springframework.beans.factory.annotation.Autowired
    public DefaultCoalitionCombatService(ReportService reports, NavigationService navigation) {
        this(reports, navigation, _ -> ThreadLocalRandom.current().nextDouble());
    }
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "The supplied entropy source must retain its state across draws, including seeded simulations.")
    public DefaultCoalitionCombatService(ReportService reports, NavigationService navigation, IntToDoubleFunction roll) {
        this.reports = reports; this.navigation = navigation; this.roll = roll;
    }

    @Override public void resolveArrivals(GameState state) {
        Map<Integer, List<Fleet>> places = new TreeMap<>();
        for (Fleet fleet : List.copyOf(state.fleets())) {
            if (fleet.evacuating()) { returnArrival(state, fleet); continue; }
            boolean stationed = !fleet.inFlight() && de.zettsystems.starfare.navigation.domain.Routes.stationAllowed(state, fleet.ownerId(), fleet.toSystemId());
            if (stationed || (fleet.inFlight() && fleet.arrivalTurn() == state.turn() + 1)) {
                places.computeIfAbsent(fleet.toSystemId(), _ -> new ArrayList<>()).add(fleet);
            }
        }
        places.forEach((system, fleets) -> resolvePlace(state, system, fleets));
    }

    private void returnArrival(GameState state, Fleet fleet) {
        if (!fleet.inFlight() || fleet.arrivalTurn() != state.turn() + 1) { return; }
        if (!navigation.interceptStationArrival(state, fleet)) { reinforce(state, fleet, true); }
    }

    private void resolvePlace(GameState state, int location, List<Fleet> fleets) {
        StarSystem target = state.getSystem(location);
        var forces = forces(state, target, fleets);
        if (forces.size() == 1) {
            fleets.stream().filter(Fleet::inFlight).forEach(fleet -> {
                if (!navigation.interceptStationArrival(state, fleet)) { reinforce(state, fleet, true); }
            });
            return;
        }
        var result = CoalitionResolver.resolve(forces, state.combatRandomnessPercent(), roll);
        var winning = result.stream().filter(side -> side.remaining() > 0).findFirst().orElse(null);
        Map<Integer, Integer> survivors = allocateContingents(target, fleets, result);
        Integer owner = ownerAfter(state, target, fleets, winning, survivors);
        int garrison = Objects.equals(owner, target.ownerId()) ? survivors.getOrDefault(0, 0) : 0;
        if (!Objects.equals(owner, target.ownerId()) && owner != null) {
            int initialGarrison = garrison;
            state.updateSystem(location, system -> system.captureBy(owner, initialGarrison));
        } else {
            int initialGarrison = garrison;
            state.updateSystem(location, system -> system.afterDefense(initialGarrison));
        }
        applySurvivors(state, target, fleets, survivors);
        var event = new TurnEvent.CoalitionBattle(location, target.name(), target.ownerId(), owner, owner == null ? "" : name(state, owner), result,
                fleets.stream().map(Fleet::globalId).sorted().toList());
        result.stream().flatMap(side -> side.members().stream()).filter(member -> member.playerId() > 0)
                .forEach(member -> {
                    reports.appendEvent(state, member.playerId(), event);
                    Objects.requireNonNull(state.intel().get(member.playerId())).put(location, new GameState.Intel(owner, state.turn(), state.getSystem(location).garrison()));
                });
    }

    private static List<CoalitionSide> forces(GameState state, StarSystem target, List<Fleet> fleets) {
        Map<Integer, Map<Integer, Integer>> sides = new TreeMap<>();
        Integer ownerId = target.ownerId();
        int owner = ownerId == null ? 0 : ownerId;
        sides.computeIfAbsent(sideId(state, owner), _ -> new TreeMap<>()).put(owner, target.garrison());
        for (var fleet : fleets) {
            sides.computeIfAbsent(sideId(state, fleet.ownerId()), _ -> new TreeMap<>()).merge(fleet.ownerId(), fleet.ships(), Integer::sum);
        }
        return sides.entrySet().stream().map(side -> new CoalitionSide(side.getKey(), 0, side.getValue().entrySet().stream()
                .map(member -> new CoalitionSide.Member(member.getKey(), name(state, member.getKey()), member.getValue(), member.getValue(), 0)).toList())).toList();
    }
    private static int sideId(GameState state, int player) {
        return state.diplomacy().groupFor(player).map(group -> group.members().stream().min(Integer::compareTo).orElseThrow()).orElse(player);
    }
    private static String name(GameState state, int player) {
        return state.players().stream().filter(candidate -> candidate.id() == player).map(candidate -> candidate.label()).findFirst().orElse("");
    }

    private static Map<Integer, Integer> allocateContingents(StarSystem target, List<Fleet> fleets, List<CoalitionSide> result) {
        Map<Integer, Integer> survivors = new TreeMap<>();
        for (var member : result.stream().flatMap(side -> side.members().stream()).toList()) {
            Map<Integer, Double> weights = new TreeMap<>();
            if (Objects.equals(target.ownerId(), member.playerId()) || (target.ownerId() == null && member.playerId() == 0)) {
                weights.put(0, (double) target.garrison());
            }
            fleets.stream().filter(fleet -> fleet.ownerId() == member.playerId()).forEach(fleet -> weights.put(fleet.globalId(), (double) fleet.ships()));
            survivors.putAll(ProportionalAllocation.distribute(member.remaining(), weights));
        }
        return survivors;
    }

    private static @Nullable Integer ownerAfter(GameState state, StarSystem target, List<Fleet> fleets,
                                                @Nullable CoalitionSide winner, Map<Integer, Integer> survivors) {
        Integer previousOwner = target.ownerId();
        if (winner == null || winner.contains(previousOwner == null ? 0 : previousOwner)) { return previousOwner; }
        var members = winner.members().stream().filter(member -> member.remaining() > 0)
                .sorted(Comparator.comparingInt(CoalitionSide.Member::remaining).reversed().thenComparingInt(CoalitionSide.Member::playerId)).toList();
        int leader = members.getFirst().playerId();
        Map<Integer, Integer> claims = new TreeMap<>();
        fleets.stream().filter(fleet -> fleet.ownerId() == leader).forEach(fleet -> {
            int claim = fleet.conquestOwner();
            if (claim != leader && !state.allied(leader, claim)) { claim = leader; }
            claims.merge(claim, survivors.getOrDefault(fleet.globalId(), 0), Integer::sum);
        });
        return claims.entrySet().stream().sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey).findFirst().orElse(leader);
    }

    private void applySurvivors(GameState state, StarSystem original, List<Fleet> fleets, Map<Integer, Integer> survivors) {
        for (var fleet : fleets) {
            int remaining = survivors.getOrDefault(fleet.globalId(), 0);
            if (remaining == 0) { state.fleets().remove(fleet); continue; }
            Fleet survived = fleet.surviveBattle(remaining);
            if (survived.inFlight() && !Objects.equals(original.ownerId(), fleet.ownerId()) && !state.allied(fleet.ownerId(), original.ownerId())) {
                survived = survived.endJourneyAfterBattle();
            }
            state.replaceFleet(survived);
            if (survived.inFlight()) {
                if (!navigation.interceptStationArrival(state, survived)) { reinforce(state, survived, false); }
            } else if (Objects.equals(state.getSystem(fleet.toSystemId()).ownerId(), fleet.ownerId())
                    && java.util.Objects.requireNonNull(survived.journey()).stationed()) {
                reinforce(state, survived, false);
            }
        }
    }

    private void reinforce(GameState state, Fleet fleet, boolean report) {
        if (!Objects.equals(state.getSystem(fleet.toSystemId()).ownerId(), fleet.ownerId())) {
            state.replaceFleet(fleet.dockAtPartner()); return;
        }
        state.fleets().remove(fleet);
        state.updateSystem(fleet.toSystemId(), system -> system.reinforce(fleet.ships()));
        var target = state.getSystem(fleet.toSystemId());
        if (report) { reports.appendEvent(state, fleet.ownerId(), new TurnEvent.Reinforcement(fleet.ownerId(), target.id(), target.name(),
                fleet.ships(), target.garrison(), "F" + fleet.localNo())); }
    }
}
