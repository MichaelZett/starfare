package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.ai.application.DefaultAiService;
import de.zettsystems.starfare.ai.application.DefaultExtendedPlanning;
import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.combat.application.DefaultCoalitionCombatService;
import de.zettsystems.starfare.combat.application.DefaultCombatService;
import de.zettsystems.starfare.diplomacy.application.DefaultDiplomacyService;
import de.zettsystems.starfare.economy.application.DefaultEconomyService;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.fleet.values.ScheduledDeparture;
import de.zettsystems.starfare.game.application.DefaultPlayerViewBuilder;
import de.zettsystems.starfare.game.domain.GalaxyGenesis;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.ArrivalPreview;
import de.zettsystems.starfare.game.values.CosmicEvent;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.GalaxyLayout;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.PlannedDeparture;
import de.zettsystems.starfare.game.values.PlannedOrder;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.RandomnessLevel;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.game.values.VisibleSystem;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plays whole seeded games through the real round pipeline, people giving random commands of every kind and computers
 * of every profile planning as usual, and balances the ships of every round: what exists after a round is what
 * existed before, plus production, minus the losses of both sides in every battle and minus what cosmic events
 * destroyed. Every step in between keeps the total, and while a round is planned no ship is booked twice and the map
 * shows every ship once.
 */
class ShipConservationTest {
    private static final int ROUND_LIMIT = 90;
    private static final int SEEDS = 3;

    static Stream<Arguments> games() {
        List<Arguments> games = new ArrayList<>();
        for (RulesetRef rules : List.of(RulesetRef.SECTOR_FORCES, RulesetRef.EXTENDED)) {
            for (int systems : List.of(12, 30, 60)) {
                for (int[] seats : List.of(new int[]{1, 1}, new int[]{2, 1}, new int[]{2, 3})) {
                    for (int seed = 1; seed <= SEEDS; seed++) {
                        games.add(Arguments.of(rules, systems, seats[0], seats[1], 1000L * systems + 100L * seats[1] + seed));
                    }
                }
            }
        }
        return games.stream();
    }

    @AfterAll
    static void showWhatTheGamesWentThrough() {
        ShipLedger.SEEN.forEach((what, count) -> System.out.println("ship accounting: " + what + " = " + count));
    }

    @ParameterizedTest(name = "{0} {1} systems {2} people {3} computers seed {4}")
    @MethodSource("games")
    void everyRoundBalancesItsShips(RulesetRef rules, int systems, int people, int computers, long seed) {
        var match = new Match(rules, systems, people, computers, seed);
        match.play();

        assertThat(match.ledger.violations()).isEmpty();
        assertThat(match.rounds).as("rounds played").isPositive();
        ShipLedger.see("games " + rules.variant(), 1);
        assertThat(match.orders.jointAttacks()).as("joint attacks tried").isPositive();
    }

    /** One game: real services, the ledger watching every step, people planning at random. */
    private static final class Match {
        private final ShipLedger ledger = new ShipLedger();
        private final DefaultPlayerViewBuilder views = new DefaultPlayerViewBuilder();
        private final GameState state = new GameState();
        private final Random people;
        private final HumanOrders orders;
        private final RulesetRoundImplementation engine;
        private final String label;
        private int rounds;

        Match(RulesetRef rules, int systems, int humans, int computers, long seed) {
            label = rules.variant() + "/" + systems + "/" + humans + "+" + computers + "/" + seed;
            Random combat = new Random(seed * 31 + 7);
            Random order = new Random(seed * 17 + 3);
            people = new Random(seed * 13 + 11);
            var economy = new DefaultEconomyService();
            RoundPipeline pipeline = new RoundPipeline(new DefaultCombatService(ledger.reports, combat::nextDouble), ledger.reports,
                    ledger.fleets, ledger.navigation, ledger.diplomacy,
                    new DefaultCoalitionCombatService(ledger.reports, ledger.navigation, _ -> combat.nextDouble()),
                    () -> order).countingCosmicLosses(ledger::countCosmicLoss);
            engine = rules.isExtended()
                    ? new ExtendedTurnEngine(pipeline, economy, new DefaultExtendedPlanning(economy, views),
                    ledger.fleets, ledger.reports)
                    : new DefaultTurnEngine(pipeline, new DefaultAiService(), ledger.reports, ledger.fleets);
            orders = new HumanOrders(ledger.fleets, economy, new DefaultDiplomacyService(), people);
            create(rules, systems, humans, computers, seed);
        }

        private void create(RulesetRef rules, int systems, int humans, int computers, long seed) {
            GameSetup setup = GameSetup.defaults().seatPlayers(humans, computers)
                    .chooseGalaxy(systems, seed % 2 == 0 ? GalaxyLayout.EVEN : GalaxyLayout.RANDOM)
                    .selectRuleset(rules).chooseRandomness(RandomnessLevel.HIGH)
                    .chooseEvents(EnumSet.allOf(CosmicEvent.class)).normalized();
            GalaxyGenesis.populate(state, setup, new Random(seed));
            List<AiStrategy> profiles = AiStrategy.forRules(rules);
            for (int index = 0; index < state.players().size(); index++) {
                Player player = state.players().get(index);
                if (player.ai()) {
                    AiStrategy profile = profiles.get((int) ((seed + index) % profiles.size()));
                    state.players().set(index, new Player(player.id(), player.name(), true, player.colorHex(),
                            player.empireName(), profile));
                } else {
                    state.joinedHumanPlayerIds().add(player.id());
                    state.seatByUser().put("account-" + player.id(), player.id());
                }
            }
            state.start();
        }

        void play() {
            while (!state.gameOver() && state.turn() <= ROUND_LIMIT) {
                // Now and then a person leaves; the computer takes over the seat with its ships.
                if (rounds == 25 && people.nextBoolean()) { state.releaseAccount("account-1"); }
                List<ScheduledDeparture> planned = List.copyOf(state.scheduledDepartures());
                for (Player player : List.copyOf(state.players())) {
                    if (!player.ai()) { orders.plan(state, player); }
                }
                checkPlanning(planned);
                ledger.beginRound(label, state);
                engine.advanceTurn(state);
                ledger.endRound(state);
                state.replayFrames().clear();
                rounds++;
            }
        }

        /**
         * Orders of this round and planned starts never book more ships than a garrison holds, except for a start
         * planned earlier that a battle has since outgrown; the map shows every ship exactly once.
         */
        private void checkPlanning(List<ScheduledDeparture> plannedBefore) {
            List<ScheduledDeparture> earlier = new ArrayList<>(plannedBefore);
            List<ScheduledDeparture> fresh = new ArrayList<>();
            for (ScheduledDeparture departure : state.scheduledDepartures()) {
                if (!earlier.remove(departure)) { fresh.add(departure); }
            }
            for (Player player : state.players()) {
                if (player.ai()) { continue; }
                Map<Integer, Integer> sent = sendsBySystem(player.id());
                for (StarSystem system : state.systems()) {
                    if (!Objects.equals(system.ownerId(), player.id())) { continue; }
                    int reserved = state.reservedShips(player.id(), system.id());
                    int sends = sent.getOrDefault(system.id(), 0);
                    boolean newlyBooked = sends > 0 || fresh.stream().anyMatch(departure ->
                            departure.ownerId() == player.id() && departure.fromSystemId() == system.id());
                    assertThat(!newlyBooked || sends + reserved <= system.garrison())
                            .as("%s turn %d: %s books %d sent and %d reserved ships", label, state.turn(), system, sends, reserved)
                            .isTrue();
                }
                checkMap(player.id(), sent);
            }
        }

        private void checkMap(int player, Map<Integer, Integer> sent) {
            var view = views.forPlayer(state, player);
            long shown = 0;
            for (VisibleSystem visible : view.systems()) {
                if (!visible.fullyVisible()) { continue; }
                StarSystem real = state.getSystem(visible.id());
                int sends = sent.getOrDefault(real.id(), 0);
                int reserved = state.reservedShips(player, real.id());
                assertThat(visible.garrison()).as("%s garrison label of %s", label, real).isEqualTo(real.garrison() - sends);
                assertThat(visible.availableShips()).as("%s ships free to send at %s", label, real)
                        .isEqualTo(Math.max(0, real.garrison() - sends - reserved));
                shown += Objects.requireNonNull(visible.garrison());
            }
            long planned = view.plannedOrders().stream().filter(PlannedOrder::isSend)
                    .mapToLong(order -> Objects.requireNonNull(order.ships())).sum();
            long flying = view.ownFleets().stream().mapToLong(Fleet::ships).sum();
            long empire = state.systems().stream().filter(system -> Objects.equals(system.ownerId(), player))
                    .mapToLong(StarSystem::garrison).sum()
                    + state.fleets().stream().filter(fleet -> fleet.ownerId() == player).mapToLong(Fleet::ships).sum();
            assertThat(shown + planned + flying).as("%s ships on the map of player %d", label, player).isEqualTo(empire);
            long arriving = ArrivalPreview.from(view).stream().mapToLong(ArrivalPreview::ships).sum();
            long expected = view.ownFleets().stream().filter(fleet -> ArrivalPreview.arrivalOf(fleet, view).isPresent())
                    .mapToLong(Fleet::ships).sum() + view.plannedOrders().stream()
                    .filter(order -> order.isSend() && order.arrivalTurn() != null)
                    .mapToLong(order -> Objects.requireNonNull(order.ships())).sum()
                    + view.plannedDepartures().stream().mapToLong(PlannedDeparture::ships).sum();
            assertThat(arriving).as("%s announced arrivals of player %d", label, player).isEqualTo(expected);
        }

        private Map<Integer, Integer> sendsBySystem(int player) {
            Map<Integer, Integer> sent = new HashMap<>();
            for (FleetOrder order : state.pendingOrders().getOrDefault(player, List.of())) {
                if (order instanceof FleetOrder.Send send) { sent.merge(send.fromSystemId(), send.ships(), Integer::sum); }
            }
            return sent;
        }
    }
}
