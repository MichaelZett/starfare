package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.domain.*;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.fleet.application.FleetService;
import de.zettsystems.starfare.navigation.application.NavigationService;
import de.zettsystems.starfare.navigation.values.FlightJourney.Phase;
import de.zettsystems.starfare.testsupport.NavigationStates;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class NavigationIntegrationTest extends AbstractIntegrationTest {
    @Autowired private GameService games;
    @Autowired private GameRegistry registry;
    @Autowired private GameSessionStore store;
    @Autowired private GameSessionRepository sessions;
    @Autowired private GameArchiveStore archives;
    @Autowired private GameArchiveRepository archiveRepository;
    @Autowired private RulesetCatalog catalog;
    @Autowired private ObjectMapper mapper;
    @Autowired private PlayerViewBuilder views;
    @Autowired private FleetService fleets;
    @Autowired private NavigationService navigation;
    @Autowired private de.zettsystems.starfare.ai.application.SpacewardPlanning ai;
    private final List<GameId> created = new ArrayList<>();
    private GameId save(GameState state) {
        GameId id = GameId.newId(); created.add(id);
        store.save(new GameSession(id, "Navigation test", "host", Instant.now(), state)); return id;
    }
    @AfterEach void cleanup() { created.forEach(id -> { store.delete(id); archiveRepository.deleteById(id.value()); }); }
    @Test void journeyAndFrozenRangeSurviveCopyJsonRestartAndArchivedReplay() {
        GameState state = NavigationStates.running(); fleets.sendFleet(state, 1, 1, 4, 20);
        state.replaceFleet(state.fleets().getFirst().landForRefueling()); state.nextTurn(); state.nextTurn();
        state.captureReplayFrame(); var fleet = state.fleets().getFirst();
        assertThat(GameState.copyOf(state).fleets()).containsExactly(fleet);
        GameState jsonCopy = GameState.fromSnapshot(mapper.readValue(mapper.writeValueAsString(GameState.toSnapshot(state)), GameStateSnapshot.class));
        assertThat(jsonCopy.navigationSettings()).isEqualTo(state.navigationSettings()); assertThat(jsonCopy.fleets()).containsExactly(fleet);
        GameId id = save(state); var restarted = new JpaGameSessionStore(sessions, archives, mapper, catalog).load(id).orElseThrow();
        assertThat(restarted.readState(current -> current.fleets().getFirst()).journey().phase()).isEqualTo(Phase.STATION);
        registry.writeState(id, current -> { current.endGame(1); return null; });
        assertThat(archives.load(id).orElseThrow().state().fleets()).containsExactly(fleet);
        assertThat(views.forReplay(state, state.replayFrames().get(3), 1).ownFleets()).containsExactly(fleet);
    }
    @Test void optionalNavigationReadsOldClassicAndOldInternalSpacewardSnapshots() {
        ObjectNode classic = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(new GameState())); classic.remove("navigation");
        assertThat(GameState.fromSnapshot(mapper.treeToValue(classic, GameStateSnapshot.class)).navigationSettings()).isEmpty();
        GameState internal = NavigationStates.running();
        internal.fleets().add(new Fleet(1, 1, 1, 1, 4, 3, 1, 8));
        ObjectNode old = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(internal)); old.remove("navigation");
        ((ObjectNode) old.get("fleets").get(0)).remove("journey");
        GameState restored = GameState.fromSnapshot(mapper.treeToValue(old, GameStateSnapshot.class));
        assertThat(restored.navigationSettings()).isPresent(); assertThat(restored.fleets().getFirst().journey()).isNull();
        assertThatThrownBy(() -> restored.configureNavigation(new de.zettsystems.starfare.navigation.values.NavigationSettings(10))).isInstanceOf(IllegalStateException.class);
    }
    @Test void routeViewCommandsAndPausedIndustryUseTheSameRulesWithoutMutatingReads() {
        GameState state = NavigationStates.running(); GameId id = save(state);
        assertThat(games.routeFor(id, 1, 1, 4).orElseThrow().rounds()).isEqualTo(8);
        assertThat(games.sendFleet(id, 1, 1, 4, 10)).isTrue();
        assertThat(games.viewFor(id, 1).plannedOrders().getFirst().arrivalTurn()).isEqualTo(9);
        assertThat(games.addStandingOrder(id, 1, 1, 4, 3)).isTrue();
        registry.writeState(id, current -> { current.updateSystem(2, system -> system.captureBy(2, 1)); return null; });
        assertThat(games.routeFor(id, 1, 1, 4)).isEmpty(); assertThat(games.sendFleet(id, 1, 1, 4, 1)).isFalse();
        var before = registry.readState(id, GameState::toSnapshot); var view = games.viewFor(id, 1);
        assertThat(view.plannedOrders().getFirst().arrivalTurn()).isNull();
        assertThat(view.standingOrders().getFirst().route()).isNull(); assertThat(view.standingOrders().getFirst().rangeLimited()).isTrue();
        assertThat(view.systems().getFirst().industry().nextDelivery()).isZero();
        assertThat(view.systems().getFirst().industry().deliveryBottleneck()).isTrue();
        assertThat(view.systems().get(1).industry()).isNull(); assertThat(ArrivalPreview.from(view)).isEmpty();
        assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(before);
    }
    @Test void aiCanUseOwnedStopsButCannotFlyAcrossALostStation() {
        GameState state = NavigationStates.running(); state.updatePlayer(1, Player::asAi);
        ai.plan(state, fleets);
        var first = (de.zettsystems.starfare.fleet.values.FleetOrder.Send) state.pendingOrders().get(1).getFirst();
        assertThat(first.toSystemId()).isEqualTo(4);
        state.pendingOrders().clear(); state.updateSystem(2, system -> system.captureBy(2, 1));
        ai.plan(state, fleets);
        var second = (de.zettsystems.starfare.fleet.values.FleetOrder.Send) state.pendingOrders().get(1).getFirst();
        assertThat(second.toSystemId()).isEqualTo(2);
        assertThat(de.zettsystems.starfare.navigation.domain.Routes.plan(state, 1, second.fromSystemId(), second.toSystemId())).isPresent();
    }
    @Test void incompleteAndInconsistentItinerariesAreRejectedInsteadOfBeingRewritten() {
        GameState state = NavigationStates.running(); fleets.sendFleet(state, 1, 1, 4, 10);
        ObjectNode missing = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(state)); missing.remove("navigation");
        assertBadSnapshot(missing);
        ObjectNode range = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(state)); ((ObjectNode) range.get("navigation")).put("range", 99);
        assertBadSnapshot(range);
        ObjectNode mismatch = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(state)); ((ObjectNode) mismatch.get("fleets").get(0)).put("toSystemId", 4);
        assertBadSnapshot(mismatch);
        ObjectNode classic = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(new GameState()));
        classic.set("navigation", mapper.valueToTree(new de.zettsystems.starfare.navigation.values.NavigationSettings(100)));
        assertBadSnapshot(classic);
    }
    private void assertBadSnapshot(ObjectNode json) {
        assertThatThrownBy(() -> GameState.fromSnapshot(mapper.treeToValue(json, GameStateSnapshot.class))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void blockedFleetHasNoInventedArrivalOrHiddenEnemyData() {
        GameState state = NavigationStates.running(); fleets.sendFleet(state, 1, 1, 4, 10);
        state.replaceFleet(state.fleets().getFirst().landForRefueling());
        state.updateSystem(2, system -> system.captureBy(2, 100)); navigation.refreshStationAccess(state);
        var view = views.forPlayer(state, 1);
        assertThat(ArrivalPreview.from(view)).isEmpty();
        assertThat(view.ownFleets().getFirst().journey().phase()).isEqualTo(Phase.BLOCKED);
        assertThat(view.systems().get(1).industry()).isNull();
        assertThat(view.systems().get(1).garrison()).isNotEqualTo(100);
    }
}
