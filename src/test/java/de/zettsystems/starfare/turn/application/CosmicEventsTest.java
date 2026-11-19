package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.CosmicEvent;
import de.zettsystems.starfare.game.values.EventFrequency;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.SectorOptions;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.report.values.CosmicNews;
import de.zettsystems.starfare.report.values.TurnEvent;
import org.junit.jupiter.api.Test;

import java.io.Serial;
import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CosmicEventsTest {
    /** Every roll strikes and every random choice takes the first candidate. */
    private static final class FirstChoice extends Random {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public double nextDouble() { return 0.0; }

        @Override
        public int nextInt(int bound) { return 0; }
    }

    /** Rolls and choices in the given order; afterwards every roll strikes and every choice takes the first. */
    private static final class Scripted extends Random {
        @Serial
        private static final long serialVersionUID = 1L;
        private final ArrayDeque<Double> doubles;
        private final ArrayDeque<Integer> ints;

        Scripted(List<Double> doubles, List<Integer> ints) {
            this.doubles = new ArrayDeque<>(doubles);
            this.ints = new ArrayDeque<>(ints);
        }

        @Override
        public double nextDouble() { return doubles.isEmpty() ? 0.0 : doubles.poll(); }

        @Override
        public int nextInt(int bound) { return ints.isEmpty() ? 0 : Math.min(bound - 1, ints.poll()); }
    }

    /** Takes the highest value of every choice. */
    private static final int HIGHEST = Integer.MAX_VALUE;

    private final CosmicEvents events = new CosmicEvents(new DefaultReportService(), FirstChoice::new);

    private static CosmicEvents scripted(List<Double> doubles, List<Integer> ints) {
        Random random = new Scripted(doubles, ints);
        return new CosmicEvents(new DefaultReportService(), () -> random);
    }

    private static GameState galaxy(Set<CosmicEvent> enabled) {
        return galaxy(new SectorOptions(EventFrequency.NORMAL, enabled, false, 0));
    }

    private static GameState galaxy(SectorOptions options) {
        GameState state = new GameState();
        state.configureLobby(true, true, true, 0);
        state.rememberSetup(GameSetup.defaults().withSectorOptions(options));
        for (int id = 1; id <= 2; id++) {
            state.players().add(new Player(id, "P" + id, false, "#ffffff"));
            state.intel().put(id, new HashMap<>());
        }
        state.systems().addAll(List.of(
                new StarSystem(1, "Home", 0, 0, 1, 10, 4, false),
                new StarSystem(2, "Colony", 100, 0, 1, 6, 2, false),
                new StarSystem(3, "Enemy", 300, 0, 2, 10, 4, false),
                new StarSystem(4, "Neutral", 200, 0, null, 8, 3, true)));
        state.start();
        state.updateCosmic(cosmic -> cosmic.rememberHomes(List.of(1, 3)));
        return state;
    }

    private static List<TurnEvent.Cosmic> news(GameState state, int player) {
        var report = state.reports().get(player);
        return report == null ? List.of() : report.events().stream()
                .filter(TurnEvent.Cosmic.class::isInstance).map(TurnEvent.Cosmic.class::cast).toList();
    }

    private static Fleet fleet(GameState state, int owner) {
        return state.fleets().stream().filter(fleet -> fleet.ownerId() == owner).findFirst().orElseThrow();
    }

    @Test
    void nothingHappensWhenEventsAreOff() {
        GameState state = galaxy(SectorOptions.none());
        state.addFleet(1, 1, 3, 5);
        int arrival = fleet(state, 1).arrivalTurn();

        events.resolve(state);

        assertThat(fleet(state, 1).arrivalTurn()).isEqualTo(arrival);
        assertThat(news(state, 1)).isEmpty();
        assertThat(state.getSystem(2).garrison()).isEqualTo(6);
    }

    @Test
    void stormDelaysEveryFleetAndTellsEveryone() {
        GameState state = galaxy(EnumSet.of(CosmicEvent.COSMIC_STORM));
        state.addFleet(1, 1, 3, 5);
        int arrival = fleet(state, 1).arrivalTurn();

        events.resolve(state);

        assertThat(fleet(state, 1).arrivalTurn()).isEqualTo(arrival + 1);
        assertThat(news(state, 1)).extracting(TurnEvent.Cosmic::news).containsExactly(CosmicNews.STORM);
        assertThat(news(state, 2)).extracting(TurnEvent.Cosmic::news).containsExactly(CosmicNews.STORM);
    }

    @Test
    void spacetimeQuakeHalvesTheRemainingFlight() {
        assertThat(CosmicEvents.halved(5, 6)).isEqualTo(6);
        assertThat(CosmicEvents.halved(5, 9)).isEqualTo(7);
        assertThat(CosmicEvents.halved(5, 10)).isEqualTo(8);

        GameState state = galaxy(EnumSet.of(CosmicEvent.SPACETIME_QUAKE));
        state.addFleet(1, 1, 3, 5);
        int arrival = fleet(state, 1).arrivalTurn();

        events.resolve(state);

        assertThat(fleet(state, 1).arrivalTurn()).isEqualTo(CosmicEvents.halved(state.turn() + 1, arrival));
        assertThat(news(state, 2)).extracting(TurnEvent.Cosmic::news).containsExactly(CosmicNews.SPACETIME_QUAKE);
    }

    @Test
    void disappearanceCostsShipsAndOnlyTheOwnerHearsOfIt() {
        GameState state = galaxy(EnumSet.of(CosmicEvent.DISAPPEARANCE));
        state.addFleet(1, 1, 3, 5);

        events.resolve(state);

        assertThat(fleet(state, 1).ships()).isEqualTo(4);
        assertThat(news(state, 1)).singleElement().satisfies(item -> {
            assertThat(item.news()).isEqualTo(CosmicNews.DISAPPEARANCE);
            assertThat(item.ships()).isOne();
            assertThat(item.remaining()).isEqualTo(4);
            assertThat(item.systemName()).isEqualTo("Enemy");
        });
        assertThat(news(state, 2)).isEmpty();
    }

    @Test
    void spaceportQuakeDestroysPartOfAGarrison() {
        GameState state = galaxy(EnumSet.of(CosmicEvent.SPACEPORT_QUAKE));

        events.resolve(state);

        assertThat(state.getSystem(1).garrison()).isEqualTo(9);
        assertThat(state.getSystem(1).ownerId()).isEqualTo(1);
        assertThat(news(state, 1)).extracting(TurnEvent.Cosmic::news).containsExactly(CosmicNews.SPACEPORT_QUAKE_LIGHT);
        assertThat(news(state, 2)).isEmpty();
    }

    @Test
    void supernovaWarnsOneRoundAheadSparesHomesAndSendsRefugees() {
        GameState state = galaxy(EnumSet.of(CosmicEvent.SUPERNOVA));
        state.addFleet(2, 3, 2, 4);

        events.resolve(state);

        assertThat(state.cosmic().unstableSystems()).containsExactly(Map.entry(2, state.turn() + 1));
        assertThat(news(state, 2)).extracting(TurnEvent.Cosmic::news).containsExactly(CosmicNews.SUN_UNSTABLE);
        assertThat(state.destroyed(2)).isFalse();

        state.nextTurn();
        events.resolve(state);

        assertThat(state.destroyed(2)).isTrue();
        assertThat(state.getSystem(2).ownerId()).isNull();
        assertThat(state.getSystem(2).garrison()).isZero();
        assertThat(state.getSystem(2).productionPerTurn()).isZero();
        assertThat(state.livingSystemCount()).isEqualTo(3);
        assertThat(news(state, 1)).extracting(TurnEvent.Cosmic::news)
                .contains(CosmicNews.SUPERNOVA, CosmicNews.REFUGEES);
        Fleet refugees = state.fleets().stream().filter(fleet -> fleet.ownerId() == 1).findFirst().orElseThrow();
        assertThat(refugees.ships()).isEqualTo(6);
        assertThat(refugees.toSystemId()).isEqualTo(1);
        assertThat(refugees.arrivalTurn()).isGreaterThan(state.turn() + 1);
        Fleet panicking = fleet(state, 2);
        assertThat(panicking.fromSystemId()).isEqualTo(2);
        assertThat(panicking.toSystemId()).isEqualTo(1);
        assertThat(news(state, 2)).extracting(TurnEvent.Cosmic::news).contains(CosmicNews.PANIC_JUMP);
        assertThat(state.cosmic().unstableSystems()).doesNotContainKey(2);
    }

    @Test
    void destroyedSystemsCanNoLongerBeTargeted() {
        GameState state = galaxy(EnumSet.of(CosmicEvent.SUPERNOVA));
        state.updateCosmic(cosmic -> cosmic.destabilize(2, state.turn()));

        events.resolve(state);

        assertThat(new DefaultFleetService().queueSend(state, 2, 3, 2, 1)).isFalse();
        assertThat(new DefaultFleetService().queueSend(state, 2, 3, 4, 1)).isTrue();
    }

    @Test
    void neutralGarrisonFleesAsPiratesThatMakeTheirTargetNeutral() {
        GameState state = galaxy(EnumSet.of(CosmicEvent.SUPERNOVA, CosmicEvent.PIRATES));
        state.systems().set(3, new StarSystem(4, "Neutral", 200, 0, null, 40, 3, true));
        // Every other system counts as home, so no further sun turns unstable during the test.
        state.updateCosmic(cosmic -> cosmic.destabilize(4, state.turn()).rememberHomes(List.of(1, 2, 3)));
        events.resolve(state);

        assertThat(state.cosmic().pirates()).singleElement().satisfies(pirates -> {
            assertThat(pirates.ships()).isEqualTo(40);
            assertThat(pirates.toSystemId()).isEqualTo(1);
        });
        assertThat(news(state, 2)).extracting(TurnEvent.Cosmic::news).contains(CosmicNews.PIRATES_FLEE);

        int landing = state.cosmic().pirates().getFirst().arrivalTurn();
        while (state.turn() + 1 < landing) {
            state.nextTurn();
        }
        events.resolve(state);

        assertThat(state.cosmic().pirates()).isEmpty();
        assertThat(state.getSystem(1).ownerId()).isNull();
        assertThat(state.getSystem(1).neutral()).isTrue();
        assertThat(state.getSystem(1).productionPerTurn()).isEqualTo(4);
        assertThat(news(state, 1)).extracting(TurnEvent.Cosmic::news).contains(CosmicNews.PIRATE_RAID_WON);
    }

    @Test
    void piratesNeedTheSupernova() {
        GameState state = galaxy(EnumSet.of(CosmicEvent.PIRATES));
        state.updateCosmic(cosmic -> cosmic.destabilize(4, state.turn()));

        events.resolve(state);

        assertThat(state.destroyed(4)).isTrue();
        assertThat(state.cosmic().pirates()).isEmpty();
    }

    @Test
    void sunsAndPiratesUnderwayFinishEvenWithEventsSwitchedOff() {
        GameState state = galaxy(SectorOptions.none());
        state.updateCosmic(cosmic -> cosmic.destabilize(4, state.turn()).launchPirates(4, 2, 50, state.turn() + 1));

        events.resolve(state);

        assertThat(state.destroyed(4)).isTrue();
        assertThat(state.cosmic().pirates()).isEmpty();
        assertThat(state.getSystem(2).ownerId()).isNull();
    }

    @Test
    void onlySwitchedOnEventsDrawARoll() {
        GameState state = galaxy(EnumSet.of(CosmicEvent.SUPERNOVA));

        scripted(List.of(0.0, 0.99, 0.99, 0.99, 0.99), List.of()).resolve(state);

        assertThat(state.cosmic().unstableSystems()).containsOnlyKeys(2);
    }

    @Test
    void aRollAsHighAsTheChanceMisses() {
        GameState state = galaxy(EnumSet.of(CosmicEvent.COSMIC_STORM));
        state.addFleet(1, 1, 3, 5);
        int arrival = fleet(state, 1).arrivalTurn();

        scripted(List.of(state.sectorOptions().chance(CosmicEvent.COSMIC_STORM)), List.of()).resolve(state);

        assertThat(fleet(state, 1).arrivalTurn()).isEqualTo(arrival);
        assertThat(news(state, 1)).isEmpty();
    }

    @Test
    void stormAndQuakeAlsoMovePirates() {
        GameState stormy = galaxy(EnumSet.of(CosmicEvent.COSMIC_STORM));
        int stormArrival = stormy.turn() + 9;
        stormy.updateCosmic(cosmic -> cosmic.launchPirates(4, 2, 5, stormArrival));
        events.resolve(stormy);
        assertThat(stormy.cosmic().pirates().getFirst().arrivalTurn()).isEqualTo(stormArrival + 1);

        GameState quaking = galaxy(EnumSet.of(CosmicEvent.SPACETIME_QUAKE));
        int quakeArrival = quaking.turn() + 9;
        quaking.updateCosmic(cosmic -> cosmic.launchPirates(4, 2, 5, quakeArrival));
        events.resolve(quaking);
        assertThat(quaking.cosmic().pirates().getFirst().arrivalTurn())
                .isEqualTo(CosmicEvents.halved(quaking.turn() + 1, quakeArrival));
    }

    @Test
    void stormAndQuakeStayQuietWithNothingInFlight() {
        GameState stormy = galaxy(EnumSet.of(CosmicEvent.COSMIC_STORM));
        GameState quaking = galaxy(EnumSet.of(CosmicEvent.SPACETIME_QUAKE));

        events.resolve(stormy);
        events.resolve(quaking);

        assertThat(news(stormy, 1)).isEmpty();
        assertThat(news(quaking, 1)).isEmpty();
    }

    @Test
    void disappearanceTakesAtMostHalfTheFleet() {
        GameState state = galaxy(EnumSet.of(CosmicEvent.DISAPPEARANCE));
        state.addFleet(1, 1, 3, 5);

        scripted(List.of(), List.of(0, HIGHEST)).resolve(state);

        assertThat(fleet(state, 1).ships()).isEqualTo(3);
        assertThat(news(state, 1)).singleElement().satisfies(item -> {
            assertThat(item.ships()).isEqualTo(2);
            assertThat(item.remaining()).isEqualTo(3);
        });
    }

    @Test
    void aLoneShipThatDisappearsTakesItsFleetAlong() {
        GameState state = galaxy(EnumSet.of(CosmicEvent.DISAPPEARANCE));
        state.addFleet(1, 1, 3, 1);

        events.resolve(state);

        assertThat(state.fleets()).isEmpty();
        assertThat(news(state, 1)).singleElement().satisfies(item -> assertThat(item.remaining()).isZero());
    }

    @Test
    void spaceportQuakeSparesEmptyAndDestroyedSystemsAndCountsHalfAsHeavy() {
        GameState state = galaxy(EnumSet.of(CosmicEvent.SPACEPORT_QUAKE));
        state.systems().set(0, new StarSystem(1, "Home", 0, 0, 1, 0, 4, false));
        state.updateCosmic(cosmic -> cosmic.explode(2));

        scripted(List.of(), List.of(0, 4)).resolve(state);

        assertThat(state.getSystem(2).garrison()).isEqualTo(6);
        assertThat(state.getSystem(3).garrison()).isEqualTo(5);
        assertThat(news(state, 2)).singleElement().satisfies(item -> {
            assertThat(item.news()).isEqualTo(CosmicNews.SPACEPORT_QUAKE_HEAVY);
            assertThat(item.ships()).isEqualTo(5);
            assertThat(item.remaining()).isEqualTo(5);
        });
        assertThat(news(state, 1)).isEmpty();
    }

    @Test
    void anEmptySunExplodesWithoutRefugees() {
        GameState state = galaxy(SectorOptions.none());
        state.systems().set(1, new StarSystem(2, "Colony", 100, 0, 1, 0, 2, false));
        state.updateCosmic(cosmic -> cosmic.destabilize(2, state.turn()));

        events.resolve(state);

        assertThat(state.fleets()).isEmpty();
        assertThat(news(state, 1)).extracting(TurnEvent.Cosmic::news).containsExactly(CosmicNews.SUPERNOVA);
    }

    @Test
    void refugeesLeaveWithTheComingRoundAsTheOwnersNextFleet() {
        GameState state = galaxy(SectorOptions.none());
        state.addFleet(1, 1, 3, 2);
        state.updateCosmic(cosmic -> cosmic.destabilize(2, state.turn()));
        int arrival = state.turn() + 1 + state.travelRounds(2, 1);

        events.resolve(state);

        Fleet refugees = state.fleets().stream().filter(fleet -> fleet.fromSystemId() == 2).findFirst().orElseThrow();
        assertThat(refugees.launchTurn()).isEqualTo(state.turn() + 1);
        assertThat(refugees.arrivalTurn()).isEqualTo(arrival);
        assertThat(refugees.localNo()).isEqualTo(2);
        assertThat(refugees.globalId()).isNotEqualTo(fleet(state, 1).globalId());
        assertThat(news(state, 1)).filteredOn(item -> item.news() == CosmicNews.REFUGEES).singleElement()
                .satisfies(item -> {
                    assertThat(item.fleetNo()).isEqualTo(2);
                    assertThat(item.round()).isEqualTo(arrival);
                    assertThat(item.targetName()).isEqualTo("Home");
                });
    }

    @Test
    void fleetsAndPiratesBoundForAnExplodedSunJumpOnToALivingSystem() {
        GameState state = galaxy(SectorOptions.none());
        state.updateCosmic(cosmic -> cosmic.explode(1));
        state.addFleet(2, 3, 2, 4);
        state.addFleet(2, 3, 4, 3);
        Fleet bound = state.fleets().stream().filter(fleet -> fleet.toSystemId() == 2).findFirst().orElseThrow();
        Fleet elsewhere = state.fleets().stream().filter(fleet -> fleet.toSystemId() == 4).findFirst().orElseThrow();
        int pirateArrival = state.turn() + 6;
        state.updateCosmic(cosmic -> cosmic.launchPirates(4, 2, 7, pirateArrival).destabilize(2, state.turn()));

        events.resolve(state);

        Fleet jumped = state.fleets().stream().filter(fleet -> fleet.globalId() == bound.globalId()).findFirst().orElseThrow();
        assertThat(jumped.toSystemId()).isEqualTo(3);
        assertThat(jumped.arrivalTurn()).isEqualTo(bound.arrivalTurn() + state.travelRounds(2, 3));
        assertThat(state.fleets()).contains(elsewhere);
        assertThat(state.cosmic().pirates()).singleElement().satisfies(pirates -> {
            assertThat(pirates.toSystemId()).isEqualTo(3);
            assertThat(pirates.arrivalTurn()).isEqualTo(pirateArrival + state.travelRounds(2, 3));
        });
    }

    @Test
    void aStrongGarrisonRepelsPiratesAndLaterPiratesKeepFlying() {
        GameState state = galaxy(SectorOptions.none());
        state.updateCosmic(cosmic -> cosmic.launchPirates(4, 3, 1, state.turn() + 1)
                .launchPirates(4, 2, 3, state.turn() + 4));

        events.resolve(state);

        assertThat(state.getSystem(3).ownerId()).isEqualTo(2);
        assertThat(state.cosmic().pirates()).singleElement()
                .satisfies(pirates -> assertThat(pirates.toSystemId()).isEqualTo(2));
        assertThat(news(state, 1)).as("pirates still on their way do not raid yet").isEmpty();
        assertThat(state.getSystem(2).garrison()).isEqualTo(6);
        assertThat(news(state, 2)).singleElement().satisfies(item -> {
            assertThat(item.news()).isEqualTo(CosmicNews.PIRATE_RAID_REPELLED);
            assertThat(item.ships()).isOne();
            assertThat(item.remaining()).isEqualTo(state.getSystem(3).garrison()).isPositive();
        });
    }
}