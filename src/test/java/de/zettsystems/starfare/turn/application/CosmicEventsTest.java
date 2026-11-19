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

    private final CosmicEvents events = new CosmicEvents(new DefaultReportService(), FirstChoice::new);

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
}
