package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.combat.application.CoalitionCombatService;
import de.zettsystems.starfare.diplomacy.values.*;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.domain.*;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.report.values.*;
import de.zettsystems.starfare.testsupport.NavigationStates;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import static org.assertj.core.api.Assertions.*;

class CoalitionIntegrationTest extends AbstractIntegrationTest {
    @Autowired private GameService games;
    @Autowired private GameRegistry registry;
    @Autowired private GameSessionStore store;
    @Autowired private GameSessionRepository sessions;
    @Autowired private GameArchiveStore archives;
    @Autowired private GameArchiveRepository archiveRepository;
    @Autowired private RulesetCatalog catalog;
    @Autowired private ObjectMapper mapper;
    @Autowired private CoalitionCombatService combat;
    @Autowired private PlayerViewBuilder views;
    private final List<GameId> created = new ArrayList<>();
    private static GameState state() {
        var state = NavigationStates.running(); state.configureLobby(true, true, true, 0);
        state.players().add(new Player(3, "Third", false, "#fff")); state.intel().put(3, new HashMap<>());
        state.updateSystem(4, system -> system.captureBy(3, 60));
        state.agreeTreaties(new DiplomacyState(2, List.of(new AllianceGroup(1, Set.of(1, 2), 3, Map.of())), List.of()));
        state.seatByUser().put("host", 1); state.seatByUser().put("partner", 2); return state;
    }
    private GameId save(GameState state) {
        var id = GameId.newId(); created.add(id); store.save(new GameSession(id, "Coalition test", "host", Instant.now(), state)); return id;
    }
    private static void arrival(GameState state, int player, int ships) {
        int id = state.addFleet(1, 3, 4, ships);
        var fleet = state.fleets().stream().filter(candidate -> candidate.globalId() == id).findFirst().orElseThrow();
        state.replaceFleet(new Fleet(id, player, fleet.localNo(), 3, 4, ships, state.turn(), state.turn() + 1));
    }
    @AfterEach void cleanup() { created.forEach(id -> { store.delete(id); archiveRepository.deleteById(id.value()); }); }

    @Test void claimIsRoundBoundAndSurvivesQueueEditingUndoAndRestart() {
        var id = save(state());
        assertThat(games.conquestCandidates(id, 1)).extracting(Player::id).containsExactly(1, 2);
        assertThat(games.sendFleet(id, 1, 2, 3, 4, 3, 2)).isFalse();
        assertThat(games.sendFleet(id, 1, 1, 3, 4, 3, 3)).isFalse();
        assertThat(games.sendFleet(id, 1, 1, 3, 4, 3, 2)).isTrue();
        var old = games.viewFor(id, 1).plannedOrders().getFirst(); assertThat(old.beneficiaryId()).isEqualTo(2);
        assertThat(games.editOrder(id, 1, 1, old, 4, 4)).isTrue();
        assertThat(games.undoOrders(id, 1, 1)).isTrue();
        var restart = new JpaGameSessionStore(sessions, archives, mapper, catalog).load(id).orElseThrow();
        FleetOrder restoredOrder = restart.readState(current -> current.pendingOrders().get(1).getFirst());
        assertThat(restoredOrder).isEqualTo(new FleetOrder.Send(1, 3, 4, 3, 2));
        assertThat(games.submitTurn(id, 1)).isTrue(); assertThat(games.sendFleet(id, 1, 1, 3, 4, 1, 2)).isFalse();
        assertThat(games.submitTurn(id, 2)).isTrue();
        assertThat(games.viewFor(id, 1).ownFleets()).singleElement().satisfies(fleet -> assertThat(fleet.beneficiaryId()).isEqualTo(2));
        var inFlight = new JpaGameSessionStore(sessions, archives, mapper, catalog).load(id).orElseThrow();
        assertThat(inFlight.readState(current -> current.fleets().getFirst()).beneficiaryId()).isEqualTo(2);
    }

    @Test void oldFleetAndOrderJsonDefaultClaimsToTheirOwnerAndKeepClassicBattlesPlayable() {
        var state = state(); arrival(state, 1, 4);
        state.pendingOrders().put(1, new ArrayList<>(List.of(new FleetOrder.Send(1, 3, 4, 1, 2))));
        state.reports().put(1, new TurnReport(1, List.of(), List.of(new TurnEvent.BattleWon(1, 4, "Delta", 10, 4, 6, true))));
        var tree = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(state));
        ((ObjectNode) tree.get("fleets").get(0)).remove("beneficiaryId");
        ((ObjectNode) tree.get("pendingOrders").get("1").get(0)).remove("beneficiaryId");
        var old = GameState.fromSnapshot(mapper.treeToValue(tree, GameStateSnapshot.class));
        assertThat(old.fleets().getFirst().conquestOwner()).isEqualTo(1);
        assertThat(((FleetOrder.Send) old.pendingOrders().get(1).getFirst()).conquestOwner()).isEqualTo(1);
        assertThat(BattleReplay.from(old.reports().get(1).events().getFirst()).orElseThrow().attackingRemaining()).isEqualTo(6);
        var coalition = (ObjectNode) mapper.valueToTree(new TurnEvent.CoalitionBattle(4, "Delta", 3, 1, "Commander", List.of()));
        coalition.remove("fleetIds");
        assertThat(mapper.treeToValue(coalition, TurnEvent.CoalitionBattle.class).fleetIds()).isEmpty();
    }

    @Test void combatFactsAndPersonalTotalsSurviveRestartAndPermanentArchive() {
        var state = state(); arrival(state, 1, 50); arrival(state, 2, 50); combat.resolveArrivals(state);
        state.captureReplayFrame(); state.nextTurn(); state.endGame(1); var id = save(state);
        var restart = new JpaGameSessionStore(sessions, archives, mapper, catalog).load(id).orElseThrow();
        var event = (TurnEvent.CoalitionBattle) restart.readState(current -> current.reports().get(1).events().getLast());
        assertThat(event.owner()).isEqualTo(1); assertThat(event.sides()).hasSize(2);
        assertThat(event.lostBy(1)).isEqualTo(30); assertThat(event.destroyedBy(1)).isEqualTo(30);
        var archived = archives.load(id).orElseThrow().state();
        assertThat(archived.replayFrames().get(1).reports().get(1).events().getLast()).isEqualTo(event);
        assertThat(BattleReplay.from(event).orElseThrow().coalitions()).isEqualTo(event.sides());
        var totals = games.outcomeStatisticsFor(id, "host").orElseThrow();
        assertThat(totals.shipsLost()).isEqualTo(30); assertThat(totals.shipsDestroyed()).isEqualTo(30);
        assertThat(games.outcomeStatisticsFor(id, "partner").orElseThrow().shipsLost()).isEqualTo(30);
        assertThat(games.replayFor(id, "host", 1, 1).orElseThrow().report().events().getLast()).isEqualTo(event);
    }

    @Test void pendingCoalitionPlaybackConcealsStationedSurvivorsAndIndustryWithoutMutatingState() {
        var state = state(); state.updateSystem(2, system -> system.relocateTo(400, 200));
        arrival(state, 1, 40); arrival(state, 2, 60);
        state.fleets().add(new Fleet(77, 1, 77, 3, 4, 7, 1, 1,
                new de.zettsystems.starfare.navigation.values.FlightJourney(List.of(3, 4, 2), 1,
                        de.zettsystems.starfare.navigation.values.FlightJourney.Phase.BLOCKED, 2, 10)));
        combat.resolveArrivals(state);
        state.captureReplayFrame(); state.nextTurn();
        var view = views.forPlayer(state, 1); var before = GameState.toSnapshot(state);
        assertThat(view.ownFleets()).hasSize(2);
        var concealed = CoalitionConcealment.hide(view, Set.of(4));
        assertThat(concealed.ownFleets()).singleElement().satisfies(fleet -> {
            assertThat(fleet.globalId()).isEqualTo(77); assertThat(fleet.ships()).isEqualTo(7);
        });
        assertThat(concealed.systems().stream().filter(system -> system.id() == 4).findFirst().orElseThrow().ownerId()).isNull();
        assertThat(concealed.systems().stream().filter(system -> system.id() == 4).findFirst().orElseThrow().industry()).isNull();
        assertThat(CoalitionConcealment.hide(view, Set.of())).isSameAs(view);
        assertThat(GameState.toSnapshot(state)).isEqualTo(before);
        assertThat(concealed.report()).isSameAs(view.report());
    }
}
