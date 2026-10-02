package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.ai.application.SpacewardPlanning;
import de.zettsystems.starfare.economy.application.EconomyService;
import de.zettsystems.starfare.economy.domain.Industry;
import de.zettsystems.starfare.fleet.application.FleetService;
import de.zettsystems.starfare.game.domain.*;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.testsupport.SpacewardStates;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class SpacewardIntegrationTest extends AbstractIntegrationTest {
    @Autowired private EconomyService economy;
    @Autowired private GameService games;
    @Autowired private GameRegistry registry;
    @Autowired private GameSessionStore store;
    @Autowired private GameSessionRepository sessions;
    @Autowired private GameArchiveRepository archiveRepository;
    @Autowired private GameArchiveStore archives;
    @Autowired private RulesetCatalog catalog;
    @Autowired private ObjectMapper mapper;
    @Autowired private PlayerViewBuilder views;
    @Autowired private FleetService fleets;
    @Autowired private SpacewardPlanning ai;
    private final List<GameId> created = new ArrayList<>();

    @AfterEach void cleanup() {
        created.forEach(id -> { store.delete(id); archiveRepository.deleteById(id.value()); });
    }
    private GameId save(GameState state) {
        GameId id = GameId.newId(); created.add(id);
        store.save(new GameSession(id, "Spaceward test", "host", Instant.now(), state));
        return id;
    }
    @Test void industrySurvivesJsonCopyRestartAndArchiveWithoutChangingHistoricalFrames() {
        GameState state = SpacewardStates.running();
        economy.allocateExpansion(state, 1, 1, 4); economy.produce(state, Map.of());
        state.captureReplayFrame();
        var frame = state.replayFrames().get(1);
        economy.produce(state, Map.of());
        assertThat(frame.industries().get(1)).isEqualTo(new Industry(10, 4, 4));
        assertThat(views.forReplay(state, frame, 1).systems().getFirst().industry().expansionProgress()).isEqualTo(4);
        assertThat(GameState.copyOf(state).industries().get(1)).isEqualTo(new Industry(10, 4, 8));
        String json = mapper.writeValueAsString(GameState.toSnapshot(state));
        assertThat(GameState.fromSnapshot(mapper.readValue(json, GameStateSnapshot.class)).industries()).isEqualTo(state.industries());
        GameId id = save(state);
        var restarted = new JpaGameSessionStore(sessions, archives, mapper, catalog).load(id).orElseThrow();
        Industry restored = restarted.readState(snap -> snap.industries().get(1));
        assertThat(restored).isEqualTo(new Industry(10, 4, 8));
        registry.writeState(id, snap -> { snap.endGame(1); return null; });
        assertThat(archives.load(id).orElseThrow().state().industries().get(1)).isEqualTo(restored);
    }
    @Test void missingOrInconsistentSpacewardEconomyIsRejectedButOldClassicDataStillLoads() {
        GameState state = SpacewardStates.running();
        ObjectNode json = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(state));
        json.remove("industries");
        ObjectNode missing = json;
        assertThatThrownBy(() -> GameState.fromSnapshot(mapper.treeToValue(missing, GameStateSnapshot.class)))
                .isInstanceOf(IllegalArgumentException.class);
        json = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(state));
        ((ObjectNode) json.get("industries").get("1")).put("expansionAllocation", 4);
        ObjectNode mismatched = json;
        assertThatThrownBy(() -> GameState.fromSnapshot(mapper.treeToValue(mismatched, GameStateSnapshot.class)))
                .isInstanceOf(IllegalArgumentException.class);
        ObjectNode classic = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(new GameState()));
        classic.remove("industries");
        assertThat(GameState.fromSnapshot(mapper.treeToValue(classic, GameStateSnapshot.class)).industries()).isEmpty();
    }
    @Test void planningChecksOwnershipRoundAndSubmissionBeforeChangingTheSavedBudget() {
        GameState state = SpacewardStates.running(); GameId id = save(state);
        assertThat(games.allocateExpansion(id, 1, 1, 1, 4)).isTrue();
        assertThat(games.allocateExpansion(id, 2, 1, 1, 2)).isFalse();
        assertThat(games.allocateExpansion(id, 1, 0, 1, 2)).isFalse();
        assertThat(games.allocateExpansion(id, 1, 1, 1, 11)).isFalse();
        registry.writeState(id, current -> { current.submittedThisTurn().add(1); return null; });
        assertThat(games.allocateExpansion(id, 1, 1, 1, 2)).isFalse();
        Industry stored = registry.readState(id, current -> current.industries().get(1));
        assertThat(stored).isEqualTo(new Industry(10, 4, 0));
        registry.writeState(id, current -> { current.endGame(1); return null; });
        assertThat(games.allocateExpansion(id, 1, 1, 1, 0)).isFalse();
    }
    @Test void visibleDeliveryMatchesResolutionAndForeignIndustryDoesNotLeak() {
        GameState state = SpacewardStates.running();
        fleets.addStandingOrder(state, 1, 1, 3, 7); fleets.addStandingOrder(state, 1, 1, 4, 3);
        economy.allocateExpansion(state, 1, 1, 4);
        var view = views.forPlayer(state, 1); var own = view.systems().getFirst();
        assertThat(own.industry().nextDelivery()).isEqualTo(8);
        assertThat(own.industry().deliveryBottleneck()).isTrue();
        assertThat(view.systems().get(2).industry()).isNull();
        var before = GameState.toSnapshot(state);
        views.forObserver(state);
        assertThat(GameState.toSnapshot(state)).isEqualTo(before);
        assertThat(views.forObserver(state).systems().get(2).industry().capacity()).isEqualTo(8);
        Map<Integer, Integer> routed = fleets.applyStandingOrdersForProduction(state);
        assertThat(routed.get(1)).isEqualTo(own.industry().nextDelivery());
        economy.produce(state, routed);
        assertThat(state.getSystem(1).garrison()).isEqualTo(10);
    }
    @Test void queuedLaunchReducesDeliveryPreviewWithoutConsumingTheGarrisonReserve() {
        GameState state = SpacewardStates.running();
        fleets.addStandingOrder(state, 1, 1, 3, 7);
        fleets.addStandingOrder(state, 1, 1, 4, 3);
        assertThat(fleets.queueSend(state, 1, 1, 2, 2)).isTrue();
        economy.allocateExpansion(state, 1, 1, 4);
        var own = views.forPlayer(state, 1).systems().getFirst();
        assertThat(own.availableShips()).isZero();
        assertThat(own.industry().nextDelivery()).isEqualTo(6);
        assertThat(fleets.sendFleet(state, 1, 1, 2, 2)).isTrue();
        Map<Integer, Integer> routed = fleets.applyStandingOrdersForProduction(state);
        assertThat(routed.get(1)).isEqualTo(own.industry().nextDelivery());
        economy.produce(state, routed);
        assertThat(state.getSystem(1).garrison()).isEqualTo(10);
    }

    @Test void damagedDefenseRebuildsBelowItsReserveBeforeShippingSurplus() {
        GameState state = SpacewardStates.running();
        fleets.addStandingOrder(state, 1, 1, 3, 7);
        economy.allocateExpansion(state, 1, 1, 4);
        state.updateSystem(1, system -> system.afterDefense(1));
        var industry = views.forPlayer(state, 1).systems().getFirst().industry();
        assertThat(industry.reserveShortfall()).isEqualTo(9);
        assertThat(industry.nextDelivery()).isZero();
        Map<Integer, Integer> first = fleets.applyStandingOrdersForProduction(state);
        assertThat(first.getOrDefault(1, 0)).isZero();
        economy.produce(state, first);
        assertThat(state.getSystem(1).garrison()).isEqualTo(7);
        assertThat(views.forPlayer(state, 1).systems().getFirst().industry().nextDelivery()).isEqualTo(3);
        Map<Integer, Integer> second = fleets.applyStandingOrdersForProduction(state);
        assertThat(second.get(1)).isEqualTo(3);
        economy.produce(state, second);
        assertThat(state.getSystem(1).garrison()).isEqualTo(10);
        assertThat(state.getSystem(1).garrisonReserve()).isEqualTo(10);
    }

    @Test void aiUsesKnownThreatsAndDoesNotReactToHiddenEnemyStrength() {
        GameState state = SpacewardStates.running(); state.updatePlayer(1, Player::asAi);
        ai.plan(state, fleets);
        assertThat(state.industries().get(1).expansionAllocation()).isEqualTo(3);
        assertThat(state.industries().get(2).expansionAllocation()).isEqualTo(1);
        state.updateSystem(3, current -> current.reinforce(1000));
        ai.plan(state, fleets);
        assertThat(state.industries().get(1).expansionAllocation()).isEqualTo(3);
        state.updateSystem(3, current -> current.relocateTo(300, 100));
        ai.plan(state, fleets);
        assertThat(state.industries().get(1).expansionAllocation()).isZero();
        assertThat(state.industries().get(2).expansionAllocation()).isZero();
        assertThat(state.industries().get(3).expansionAllocation()).isZero();
    }
}
