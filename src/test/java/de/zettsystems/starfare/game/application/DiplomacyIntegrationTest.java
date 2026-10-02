package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.diplomacy.application.DiplomacyService;
import de.zettsystems.starfare.diplomacy.values.*;
import de.zettsystems.starfare.game.domain.*;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.testsupport.NavigationStates;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import static org.assertj.core.api.Assertions.*;

class DiplomacyIntegrationTest extends AbstractIntegrationTest {
    @Autowired private GameService games;
    @Autowired private GameRegistry registry;
    @Autowired private GameSessionStore store;
    @Autowired private GameSessionRepository sessions;
    @Autowired private GameArchiveStore archives;
    @Autowired private GameArchiveRepository archiveRepository;
    @Autowired private RulesetCatalog catalog;
    @Autowired private ObjectMapper mapper;
    @Autowired private DiplomacyService diplomacy;
    @Autowired private PlayerViewBuilder views;
    private final List<GameId> created = new ArrayList<>();
    private GameId save(GameState state) {
        state.seatByUser().put("host", 1); state.seatByUser().put("partner", 2);
        var id = GameId.newId(); created.add(id); store.save(new GameSession(id, "Treaty test", "host", Instant.now(), state)); return id;
    }
    private static DiplomacyOrder action(DiplomacyOrder.Action action, int subject, int notice) { return new DiplomacyOrder(action, subject, notice); }
    private void found(GameState state) {
        assertThat(diplomacy.act(state, 1, action(DiplomacyOrder.Action.FOUND, 2, 3))).isTrue();
        assertThat(diplomacy.act(state, 2, action(DiplomacyOrder.Action.APPROVE, state.diplomacy().proposals().getFirst().id(), 0))).isTrue();
    }
    @AfterEach void cleanup() { created.forEach(id -> { store.delete(id); archiveRepository.deleteById(id.value()); }); }
    @Test void accountRoundSubmissionAndClassicGuardsRejectUnauthorizedWrites() {
        var id = save(NavigationStates.running()); var order = action(DiplomacyOrder.Action.FOUND, 2, 3);
        assertThat(games.diplomacyFor(id, "stranger")).isEmpty();
        assertThat(games.diplomacyOrder(id, "stranger", 1, order)).isFalse();
        assertThat(games.diplomacyOrder(id, "host", 2, order)).isFalse();
        assertThat(games.diplomacyOrder(id, "host", 1, order)).isTrue();
        var own = games.diplomacyFor(id, "partner").orElseThrow(); int proposal = own.treaties().proposals().getFirst().id();
        assertThat(games.diplomacyOrder(id, "partner", 1, action(DiplomacyOrder.Action.APPROVE, proposal, 0))).isTrue();
        assertThat(games.submitTurn(id, 1)).isTrue();
        assertThat(games.diplomacyFor(id, "host").orElseThrow().canAct()).isFalse();
        assertThat(games.diplomacyOrder(id, "host", 1, action(DiplomacyOrder.Action.LEAVE, 0, 0))).isFalse();
        var classic = new GameState(); classic.players().addAll(NavigationStates.running().players()); classic.joinedHumanPlayerIds().addAll(List.of(1, 2)); classic.start();
        var classicId = save(classic); assertThat(games.diplomacyFor(classicId, "host")).isEmpty();
        assertThat(games.diplomacyOrder(classicId, "host", 1, order)).isFalse();
    }
    @Test void announcedDepartureSurvivesCopyJsonRestartAndPermanentArchive() {
        var state = NavigationStates.running(); found(state); diplomacy.act(state, 1, action(DiplomacyOrder.Action.LEAVE, 0, 0));
        assertThat(GameState.copyOf(state).diplomacy()).isEqualTo(state.diplomacy());
        var json = mapper.writeValueAsString(GameState.toSnapshot(state));
        var restored = GameState.fromSnapshot(mapper.readValue(json, GameStateSnapshot.class));
        assertThat(restored.diplomacy().groups().getFirst().departures()).containsEntry(1, 4);
        var id = save(restored); var restarted = new JpaGameSessionStore(sessions, archives, mapper, catalog).load(id).orElseThrow();
        assertThat(restarted.readState(GameState::diplomacy)).isEqualTo(state.diplomacy());
        registry.writeState(id, current -> { current.endGame(1); return null; });
        assertThat(archives.load(id).orElseThrow().state().diplomacy()).isEqualTo(state.diplomacy());
        assertThat(games.diplomacyOrder(id, "host", 1, action(DiplomacyOrder.Action.LEAVE, 0, 0))).isFalse();
    }
    @Test void pendingConsentPersistsAndReadsDoNotShareMilitaryIntelligenceOrWriteSnapshots() {
        var state = NavigationStates.running(); found(state); var id = save(state);
        var before = registry.readState(id, GameState::toSnapshot); var version = sessions.findById(id.value()).orElseThrow().getVersion();
        assertThat(games.diplomacyFor(id, "host").orElseThrow().treaties().allied(1, 2)).isTrue();
        var alliedView = views.forPlayer(state, 1);
        var solo = GameState.copyOf(state); solo.agreeTreaties(DiplomacyState.EMPTY); var soloView = views.forPlayer(solo, 1);
        assertThat(alliedView.systems()).isEqualTo(soloView.systems());
        assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(before);
        assertThat(sessions.findById(id.value()).orElseThrow().getVersion()).isEqualTo(version);
        var pending = NavigationStates.running(); diplomacy.act(pending, 1, action(DiplomacyOrder.Action.FOUND, 2, 2));
        var copy = GameState.fromSnapshot(mapper.readValue(mapper.writeValueAsString(GameState.toSnapshot(pending)), GameStateSnapshot.class));
        assertThat(copy.diplomacy().proposals()).isEqualTo(pending.diplomacy().proposals());
    }
    @Test void missingTreatiesRemainContractlessAndMalformedCrossVariantDataAreRejected() {
        var old = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(NavigationStates.running())); old.remove("diplomacy");
        assertThat(GameState.fromSnapshot(mapper.treeToValue(old, GameStateSnapshot.class)).diplomacy()).isEqualTo(DiplomacyState.EMPTY);
        var state = NavigationStates.running(); found(state);
        var classic = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(new GameState())); classic.set("diplomacy", mapper.valueToTree(state.diplomacy()));
        assertThatThrownBy(() -> GameState.fromSnapshot(mapper.treeToValue(classic, GameStateSnapshot.class))).isInstanceOf(IllegalArgumentException.class);
        var unknown = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(state));
        ((ObjectNode) unknown.get("diplomacy").get("groups").get(0)).set("members", mapper.valueToTree(List.of(1, 99)));
        assertThatThrownBy(() -> GameState.fromSnapshot(mapper.treeToValue(unknown, GameStateSnapshot.class))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void partnerContingentsAndBlockedReturnsSurviveRestartWithTheirOwner() {
        var state = NavigationStates.running(); found(state);
        assertThat(new de.zettsystems.starfare.fleet.application.DefaultFleetService().sendFleet(state, 1, 3, 4, 3)).isTrue();
        state.replaceFleet(state.fleets().getFirst().dockAtPartner());
        var id = save(state);
        var restarted = new JpaGameSessionStore(sessions, archives, mapper, catalog).load(id).orElseThrow();
        assertThat(restarted.readState(current -> current.fleets().getFirst().journey().stationed()).booleanValue()).isTrue();
        registry.writeState(id, current -> { current.replaceFleet(current.fleets().getFirst().requireReturn()); return null; });
        var returning = new JpaGameSessionStore(sessions, archives, mapper, catalog).load(id).orElseThrow();
        var fleet = returning.readState(current -> current.fleets().getFirst());
        assertThat(fleet.ownerId()).isEqualTo(1); assertThat(fleet.ships()).isEqualTo(3);
        assertThat(fleet.evacuating()).isTrue(); assertThat(fleet.journey().blocked()).isTrue();
        assertThat(returning.readState(current -> current.getSystem(4).ownerId()).intValue()).isEqualTo(2);
    }
}
