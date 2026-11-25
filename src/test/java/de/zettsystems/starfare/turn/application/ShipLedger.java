package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.diplomacy.application.DefaultDiplomacyService;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.PirateFleet;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.navigation.application.DefaultNavigationService;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.report.application.ReportService;
import de.zettsystems.starfare.report.values.TurnEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ship accounting of one round, checked at the steps of the real round pipeline. Every ship is a garrison, a fleet or
 * a pirate fleet. Within a round only production adds ships and only battles and cosmic events remove them; every
 * other step must keep the total. The decorators below see each step of {@link RoundPipeline#advanceTurn} through the
 * services it calls, the report decorator sees every event, and the pipeline's cosmic hook names each destroyed ship.
 */
final class ShipLedger {
    /** What all games of a test run went through, to show the paths the accounting covered. */
    static final Map<String, Long> SEEN = new java.util.concurrent.ConcurrentSkipListMap<>();
    private final List<String> violations = new ArrayList<>();
    private final List<TurnEvent> events = new ArrayList<>();
    private String game = "";
    private long start;
    private long cosmicLosses;
    private boolean open;

    final ReportService reports = new Reports();
    final DefaultFleetService fleets = new Fleets();
    final DefaultNavigationService navigation = new Navigation();
    final DefaultDiplomacyService diplomacy = new Diplomacy();

    static long total(GameState state) {
        return state.systems().stream().mapToLong(StarSystem::garrison).sum()
                + state.fleets().stream().mapToLong(Fleet::ships).sum()
                + state.cosmic().pirates().stream().mapToLong(PirateFleet::ships).sum();
    }

    void countCosmicLoss(int ships) {
        if (ships < 0) { violations.add(game + ": a cosmic event reported " + ships + " destroyed ships"); }
        cosmicLosses += ships;
    }

    /** Called right before {@code advanceTurn}, after every order of the round is planned. */
    void beginRound(String label, GameState state) {
        game = label + " round " + state.turn();
        events.clear();
        cosmicLosses = 0;
        start = total(state);
        open = true;
    }

    /** Called right after {@code advanceTurn}: the round only produced, fought and suffered events. */
    void endRound(GameState state) {
        open = false;
        expect(state, "after the round", start + produced() - battleLosses() - cosmicLosses);
        checkShape(state);
        see("rounds", 1);
        see("ships destroyed by cosmic events", cosmicLosses);
        see("ships lost in battles", battleLosses());
        for (TurnEvent event : events) {
            switch (event) {
                case TurnEvent.JointDeparture departure -> see("planned start " + departure.outcome(), 1);
                case TurnEvent.Cosmic news -> see("cosmic " + news.news(), 1);
                default -> see("event " + event.getClass().getSimpleName(), 1);
            }
        }
    }

    List<String> violations() { return Collections.unmodifiableList(violations); }

    static void see(String what, long count) {
        SEEN.merge(what, count, Long::sum);
    }

    long produced() {
        return events.stream().filter(TurnEvent.Production.class::isInstance).map(TurnEvent.Production.class::cast)
                .mapToLong(TurnEvent.Production::amount).sum();
    }

    /** Both sides' losses of every battle; a coalition battle is reported to each party but counted once. */
    long battleLosses() {
        Set<TurnEvent> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        long lost = 0;
        for (TurnEvent event : events) {
            switch (event) {
                case TurnEvent.BattleWon won -> lost += won.attacking() + won.defending() - won.remaining();
                case TurnEvent.BattleLost beaten -> lost += beaten.attacking() + beaten.defending() - beaten.defendersLeft();
                case TurnEvent.CoalitionBattle battle when seen.add(battle) -> lost += battle.sides().stream()
                        .flatMap(side -> side.members().stream())
                        .mapToLong(member -> member.ships() - member.remaining()).sum();
                default -> { }
            }
        }
        return lost;
    }

    private void expect(GameState state, String step, long expected) {
        long actual = total(state);
        if (actual != expected) {
            violations.add(game + " " + step + ": expected " + expected + " ships but found " + actual
                    + " (start " + start + ", produced " + produced() + ", battles " + battleLosses()
                    + ", cosmic " + cosmicLosses + ")");
        }
    }

    /** Nothing is negative, no fleet exists twice and nothing in flight or planned lies in the past. */
    private void checkShape(GameState state) {
        Set<Integer> ids = new HashSet<>();
        for (Fleet fleet : state.fleets()) {
            if (fleet.ships() <= 0) { violations.add(game + ": empty fleet " + fleet); }
            if (!ids.add(fleet.globalId())) { violations.add(game + ": fleet " + fleet.globalId() + " exists twice"); }
            if (fleet.inFlight() && fleet.arrivalTurn() <= state.turn()) {
                violations.add(game + ": fleet " + fleet + " can never arrive any more (turn " + state.turn() + ")");
            }
        }
        state.systems().stream().filter(system -> system.garrison() < 0)
                .forEach(system -> violations.add(game + ": negative garrison " + system));
        state.scheduledDepartures().stream().filter(departure -> departure.departureTurn() < state.turn())
                .forEach(departure -> violations.add(game + ": start left behind " + departure));
    }

    private final class Reports implements ReportService {
        private final DefaultReportService delegate = new DefaultReportService();

        @Override
        public void appendEvent(GameState state, int playerId, TurnEvent event) {
            events.add(event);
            delegate.appendEvent(state, playerId, event);
        }

        @Override
        public void appendReport(GameState state, int playerId, String line) {
            delegate.appendReport(state, playerId, line);
        }

        @Override
        public boolean hasSignificantEventsFor(GameState state, int playerId) {
            return delegate.hasSignificantEventsFor(state, playerId);
        }
    }

    /** Relocations become fleets before production; until production takes them, the routed ships count twice. */
    private final class Fleets extends DefaultFleetService {
        @Override
        public Map<Integer, Integer> applyStandingOrdersForProduction(GameState state) {
            if (open) { expect(state, "before relocations", start); }
            Map<Integer, Integer> before = garrisons(state);
            Map<Integer, Integer> routed = super.applyStandingOrdersForProduction(state);
            if (open) {
                routed.forEach((system, ships) -> {
                    StarSystem source = state.getSystem(system);
                    int garrison = before.getOrDefault(system, 0);
                    if (ships > garrison + source.productionPerTurn()) {
                        violations.add(game + ": relocations took " + ships + " ships from " + source);
                    }
                    // Relocations ignore reservations: beyond production they take garrison ships a later start holds.
                    Integer owner = source.ownerId();
                    int fromGarrison = Math.max(0, ships - source.productionPerTurn());
                    if (owner != null && fromGarrison > 0
                            && garrison - fromGarrison < state.reservedShips(owner, system)) {
                        see("relocations took reserved ships", 1);
                    }
                });
                long relocated = routed.values().stream().mapToLong(Integer::longValue).sum();
                expect(state, "after relocations", start + relocated);
                see("ships relocated", relocated);
            }
            return routed;
        }

        private static Map<Integer, Integer> garrisons(GameState state) {
            Map<Integer, Integer> garrisons = new java.util.HashMap<>();
            state.systems().forEach(system -> garrisons.put(system.id(), system.garrison()));
            return garrisons;
        }
    }

    /** Sends, planned starts and departing legs move ships; battles have not been fought yet. */
    private final class Navigation extends DefaultNavigationService {
        @Override
        public void departReadyFleets(GameState state) {
            if (open) { expect(state, "after sends and planned starts", start); }
            super.departReadyFleets(state);
            if (open) { expect(state, "after departing legs", start); }
        }

        @Override
        public void refreshStationAccess(GameState state, int accessTurn) {
            if (open) { expect(state, "after the battles", start + produced() - battleLosses()); }
            super.refreshStationAccess(state, accessTurn);
            if (open) { expect(state, "after station access", start + produced() - battleLosses()); }
        }
    }

    /** Production is complete; wait orders and treaty changes only move ships in time. */
    private final class Diplomacy extends DefaultDiplomacyService {
        @Override
        public void beginRound(GameState state, int turn) {
            if (open) { expect(state, "after production", start + produced()); }
            super.beginRound(state, turn);
            if (open) { expect(state, "after treaties", start + produced()); }
        }
    }
}
