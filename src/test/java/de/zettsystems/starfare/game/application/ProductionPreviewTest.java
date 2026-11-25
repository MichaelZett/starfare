package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.economy.values.AllocationSplit;
import de.zettsystems.starfare.economy.values.ProductionAllocation;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.RelocationAdjustment;
import de.zettsystems.starfare.game.values.StandingOrder;
import de.zettsystems.starfare.testsupport.SpacewardStates;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionPreviewTest extends AbstractIntegrationTest {
    private static final String OWNER = "preview-owner";

    @Autowired GameService games;
    @Autowired GameRegistry registry;
    @Autowired GameSessionStore store;
    private GameId id;

    private void save(GameState state) {
        state.seatByUser().put(OWNER, 1);
        id = GameId.newId();
        store.save(new GameSession(id, "Production preview", OWNER, Instant.now(), state));
    }

    @AfterEach void cleanup() { if (id != null) { store.delete(id); } }

    /** Alpha (capacity 10) relocates 6 ships to Beta and 4 to the enemy home every round. */
    private static GameState withRelocations() {
        var state = SpacewardStates.running();
        var fleets = new DefaultFleetService();
        assertThat(fleets.addStandingOrder(state, 1, 1, 2, 6)).isPositive();
        assertThat(fleets.addStandingOrder(state, 1, 1, 3, 4)).isPositive();
        return state;
    }

    private Map<Integer, Integer> routes() {
        return registry.readState(id, state -> state.standingOrders().get(1).stream().collect(
                Collectors.toMap(StandingOrder::toSystemId, StandingOrder::ships)));
    }

    private AllocationSplit saved() {
        return AllocationSplit.saved(games.viewFor(id, 1).systems().stream()
                .filter(system -> system.id() == 1).findFirst().orElseThrow().industry());
    }

    @Test void optimisticProductionCommitRejectsAnExternalEditAndStaleRound() {
        save(SpacewardStates.running());
        var command = new ProductionAllocation(1, 1, 2, saved());
        assertThat(games.commitProduction(id, "stranger", command).accepted()).isFalse();
        assertThat(games.allocateExpansion(id, 1, 1, 1, 1)).isTrue();
        var before = registry.readState(id, GameState::toSnapshot);
        assertThat(games.commitProduction(id, OWNER, command).accepted()).as("plan changed meanwhile").isFalse();
        assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(before);
        var fresh = saved();
        assertThat(games.commitProduction(id, OWNER, new ProductionAllocation(1, 0, 2, fresh)).accepted()).isFalse();
        assertThat(games.commitProduction(id, OWNER, new ProductionAllocation(1, 1, 2, fresh)).accepted()).isTrue();
    }

    @Test void previewsTheSystemWithoutChangingMemoryOrStorage() {
        save(SpacewardStates.running());
        var before = registry.readState(id, GameState::toSnapshot);
        var result = games.previewProduction(id, OWNER, new ProductionAllocation(1, 1, 4)).orElseThrow();
        assertThat(result.industry().shipbuilding()).isEqualTo(6);
        assertThat(result.industry().expansionAllocation()).isEqualTo(4);
        assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(before);
        assertThat(store.load(id).orElseThrow().readState(GameState::toSnapshot)).isEqualTo(before);
        assertThat(games.allocateExpansion(id, 1, 1, 1, 4)).isTrue();
        assertThat(saved()).isEqualTo(new AllocationSplit(6, 4));
    }

    @Test void rejectsForeignSystemsUnknownAccountsInvalidBudgetsAndStaleRounds() {
        save(SpacewardStates.running());
        assertThat(games.previewProduction(id, "stranger", new ProductionAllocation(1, 1, 0))).isEmpty();
        assertThat(games.previewProduction(id, OWNER, new ProductionAllocation(3, 1, 0))).as("foreign").isEmpty();
        assertThat(games.previewProduction(id, OWNER, new ProductionAllocation(1, 0, 0))).as("stale").isEmpty();
        assertThat(games.previewProduction(id, OWNER, new ProductionAllocation(1, 1, 50))).as("budget").isEmpty();
        registry.writeState(id, state -> { state.submittedThisTurn().add(1); return null; });
        assertThat(games.previewProduction(id, OWNER, new ProductionAllocation(1, 1, 0))).as("submitted").isEmpty();
    }

    @Test void lessShipbuildingShortensOutgoingRelocationsInTheSameSave() {
        save(withRelocations());
        var preview = games.previewProduction(id, OWNER, new ProductionAllocation(1, 1, 4)).orElseThrow();
        assertThat(preview.relocations()).extracting(adjustment -> adjustment.toSystemId() + ":" + adjustment.before() + "→" + adjustment.after())
                .containsExactlyInAnyOrder("2:6→4", "3:4→2");
        assertThat(routes()).containsEntry(2, 6).containsEntry(3, 4);
        var result = games.commitProduction(id, OWNER, new ProductionAllocation(1, 1, 4, saved()));
        assertThat(result.accepted()).isTrue();
        assertThat(result.relocations()).isEqualTo(preview.relocations());
        assertThat(routes()).containsEntry(2, 4).containsEntry(3, 2);
        assertThat(games.commitProduction(id, OWNER, new ProductionAllocation(1, 1, 0, saved())).relocations()).isEmpty();
        assertThat(routes()).containsEntry(2, 4).containsEntry(3, 2);
    }

    @Test void rejectedPlansNeverTouchRelocations() {
        save(withRelocations());
        var before = registry.readState(id, GameState::toSnapshot);
        var stale = new ProductionAllocation(1, 1, 4, new AllocationSplit(2, 8));
        assertThat(games.commitProduction(id, OWNER, stale).accepted()).isFalse();
        assertThat(registry.readState(id, GameState::toSnapshot)).isEqualTo(before);
    }

    @Test void incomingRoutesAndGarrisonSurplusFollowTheRule() {
        var state = withRelocations();
        assertThat(new DefaultFleetService().addStandingOrder(state, 1, 2, 1, 2)).isPositive();
        assertThat(state.getSystem(1).garrison()).isPositive();
        save(state);
        var result = games.commitProduction(id, OWNER, new ProductionAllocation(1, 1, 6, saved()));
        assertThat(result.relocations()).extracting(RelocationAdjustment::after).containsExactlyInAnyOrder(4, 2);
        assertThat(routes()).containsEntry(2, 4).containsEntry(3, 2).containsEntry(1, 2);
    }
}
