package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.economy.domain.RecyclingOrders;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.GameStateSnapshot;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.testsupport.MetalStates;
import de.zettsystems.starfare.testsupport.RecyclingStates;
import de.zettsystems.starfare.turn.application.TurnEngine;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import static org.assertj.core.api.Assertions.*;

class RecyclingSnapshotTest extends AbstractIntegrationTest {
    @Autowired private ObjectMapper mapper;
    @Autowired private GameSessionStore store;
    @Autowired private GameSessionRepository repository;
    @Autowired private GameArchiveStore archives;
    @Autowired private GameRegistry registry;
    @Autowired private GameService games;
    @Autowired private TurnEngine turns;

    @Test void restartPreservesAnOrderAndFractionThenResolvesExactlyOnce() {
        var state = RecyclingStates.running();
        assertThat(RecyclingOrders.plan(state, 1, 1, 1)).isTrue(); turns.advanceTurn(state);
        assertThat(RecyclingOrders.plan(state, 1, 1, 9)).isTrue();
        var id = GameId.newId();
        try {
            store.save(new GameSession(id, "W3 restart", null, Instant.now(), state));
            var restarted = new JpaGameSessionStore(repository, archives, mapper); restarted.loadFromDatabase();
            var restored = restarted.load(id).orElseThrow().readState(GameState::copyOf);
            assertThat(GameState.toSnapshot(restored)).isEqualTo(GameState.toSnapshot(state));
            assertThat(restored.metals().orElseThrow().recycling().fractions()).containsEntry(1, 7);
            assertThat(restored.recyclingShips(1)).isEqualTo(9);
            turns.advanceTurn(restored);
            assertThat(restored.metals().orElseThrow().recycling().recyclingLossTenths()).isEqualTo(30);
            assertThat(restored.metals().orElseThrow().recycling().fractions()).containsEntry(1, 0);
            turns.advanceTurn(restored);
            assertThat(restored.metals().orElseThrow().recycling().recyclingLossTenths()).isEqualTo(30);
            assertThat(RecyclingOrders.balanceTenths(restored)).isEqualTo(restored.metals().orElseThrow().recycling().initialTenths());
        } finally { store.delete(id); }
    }
    @Test void missingRecyclingIsToleratedOnlyForOlderVersionsAndNeverRegenerated() {
        ObjectNode old = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(MetalStates.running(0, 0)));
        ((ObjectNode) old.path("metals")).remove("recycling");
        assertThat(GameState.fromSnapshot(mapper.treeToValue(old, GameStateSnapshot.class)).metals().orElseThrow().recycling()).isNull();
        var state = RecyclingStates.running();
        ObjectNode current = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(state));
        ((ObjectNode) current.path("metals")).remove("recycling");
        var input1 = mapper.treeToValue(current, GameStateSnapshot.class);
        assertThatThrownBy(() -> GameState.fromSnapshot(input1)).isInstanceOf(IllegalArgumentException.class);
        ObjectNode historical = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(state));
        ((ObjectNode) historical.path("replayFrames").path("1").path("metals")).remove("recycling");
        var input2 = mapper.treeToValue(historical, GameStateSnapshot.class);
        assertThatThrownBy(() -> GameState.fromSnapshot(input2)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void corruptedBalanceAndCombinedReservationsAreRejectedOnRestoration() {
        var state = RecyclingStates.running();
        ObjectNode broken = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(state));
        ObjectNode ledger = (ObjectNode) broken.path("metals").path("recycling");
        ledger.put("initialTenths", ledger.path("initialTenths").asLong() + 1);
        var input3 = mapper.treeToValue(broken, GameStateSnapshot.class);
        assertThatThrownBy(() -> GameState.fromSnapshot(input3)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unbalanced");
        assertThat(new de.zettsystems.starfare.fleet.application.DefaultFleetService().queueSend(state, 1, 1, 2, 9)).isTrue();
        assertThat(RecyclingOrders.plan(state, 1, 1, 3)).isTrue();
        ObjectNode reservation = (ObjectNode) mapper.valueToTree(GameState.toSnapshot(state));
        ((ObjectNode) reservation.path("metals").path("recycling").path("orders")).put("1", 4);
        var input4 = mapper.treeToValue(reservation, GameStateSnapshot.class);
        assertThatThrownBy(() -> GameState.fromSnapshot(input4)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("reservation");
    }
    @Test void orchestratorRejectsStaleSubmittedAndForeignRecyclingCommands() {
        var state = RecyclingStates.running(); var id = GameId.newId();
        try {
            store.save(new GameSession(id, "W3 commands", null, Instant.now(), state));
            assertThat(games.planRecycling(id, 1, 0, 1, 1)).isFalse();
            assertThat(games.planRecycling(id, 2, 1, 1, 1)).isFalse();
            assertThat(games.planRecycling(id, 1, 1, 1, 1)).isTrue();
            assertThat(games.planRecycling(id, 1, 1, 1, 0)).isTrue();
            assertThat(games.submitTurn(id, 1)).isTrue();
            var before = registry.readState(id, GameState::toSnapshot);
            assertThat(games.planRecycling(id, 1, 1, 1, 1)).isFalse();
            assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(before);
        } finally { store.delete(id); }
    }
}
