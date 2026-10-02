package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.ai.application.DefaultAiService;
import de.zettsystems.starfare.combat.application.DefaultCombatService;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.report.values.TurnEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Fixed Classic reference outcomes, independent of random rolls and wall-clock timestamps. */
class ClassicTurnContractTest {
    private final DefaultFleetService fleets = new DefaultFleetService();
    private final DefaultReportService reports = new DefaultReportService();
    private final TurnEngine engine = new DefaultTurnEngine(new DefaultCombatService(reports),
            new DefaultAiService(), reports, fleets);

    private static GameState players(int count) {
        GameState state = new GameState();
        state.configureLobby(true, true, true, 0);
        state.configureRoundRules(new RoundRules(null, null, AttackOrder.STRONGEST_FIRST));
        for (int id = 1; id <= count; id++) {
            state.players().add(new Player(id, "P" + id, false, "#ffffff"));
            state.intel().put(id, new HashMap<>());
        }
        return state;
    }

    @Test
    void productionRoutingReserveAndWaitHaveFixedThreeRoundOutcomes() {
        GameState state = players(2);
        state.systems().addAll(List.of(
                new StarSystem(1, "Home", 0, 0, 1, 10, 4, false, 6),
                new StarSystem(2, "Relay", 100, 0, 1, 5, 1, false),
                new StarSystem(3, "Enemy", 1000, 0, 2, 10, 2, false),
                new StarSystem(4, "Neutral", 2000, 0, null, 7, 5, true)));
        state.start();

        assertThat(fleets.addStandingOrder(state, 1, 1, 2, 4)).isOne();
        assertThat(fleets.queueSend(state, 1, 1, 2, 4)).isTrue();
        assertThat(fleets.queueSend(state, 1, 1, 2, 1)).isFalse();
        assertThat(state.getSystem(1).garrison()).isEqualTo(10);
        assertThat(state.travelRounds(1, 2)).isEqualTo(2);
        assertThat(state.travelRounds(1, 4)).isEqualTo(20);

        engine.advanceTurn(state);
        assertRound(state, 2, 6, 6, 12, 7);
        assertThat(state.fleets()).containsExactly(
                new Fleet(1, 1, 1, 1, 2, 4, 1, 3), new Fleet(2, 1, 2, 1, 2, 4, 1, 3));
        assertThat(state.pendingOrders()).isEmpty();
        assertThat(state.getSystem(1).availableShips()).isZero();

        assertThat(fleets.queueWait(state, 1, 1)).isTrue();
        engine.advanceTurn(state);
        assertRound(state, 3, 6, 11, 14, 7);
        assertThat(state.fleets()).containsExactly(
                new Fleet(1, 1, 1, 1, 2, 4, 1, 4), new Fleet(3, 1, 3, 1, 2, 4, 2, 4));
        assertThat(state.waitThisTurn()).isEmpty();
        assertThat(state.reports().get(1).events()).containsExactly(
                new TurnEvent.Production(1, 1, "Home", 4),
                new TurnEvent.Production(1, 2, "Relay", 1),
                new TurnEvent.Reinforcement(1, 2, "Relay", 4, 11, "F2"));

        engine.advanceTurn(state);
        assertRound(state, 4, 6, 20, 16, 7);
        assertThat(state.fleets()).containsExactly(new Fleet(4, 1, 4, 1, 2, 4, 3, 5));
        assertThat(state.reports().get(1).events()).containsExactly(
                new TurnEvent.Production(1, 1, "Home", 4),
                new TurnEvent.Production(1, 2, "Relay", 1),
                new TurnEvent.Reinforcement(1, 2, "Relay", 8, 20, "F1,3"));
        assertThat(state.replayFrames().get(1).systems()).extracting(StarSystem::garrison)
                .containsExactly(6, 6, 12, 7);
        assertThat(state.replayFrames()).containsOnlyKeys(1, 2, 3);
    }

    private static void assertRound(GameState state, int turn, Integer... garrisons) {
        assertThat(state.turn()).isEqualTo(turn);
        assertThat(state.systems()).extracting(StarSystem::garrison).containsExactly(garrisons);
        assertThat(state.systems()).extracting(StarSystem::productionPerTurn).containsExactly(4, 1, 2, 5);
        assertThat(state.getSystem(1).garrisonReserve()).isEqualTo(6);
        assertThat(state.gameOver()).isFalse();
    }

    @Test
    void mergedFriendlyReinforcementPrecedesSequentialEqualSizeAttackers() {
        GameState state = players(3);
        state.systems().addAll(List.of(
                new StarSystem(1, "Target", 0, 0, 1, 5, 2, false, 4),
                new StarSystem(2, "Second", 100, 0, 2, 4, 1, false),
                new StarSystem(3, "Third", 200, 0, 3, 4, 1, false),
                new StarSystem(4, "Neutral", 2000, 0, null, 8, 4, true)));
        state.fleets().addAll(List.of(
                new Fleet(1, 1, 1, 1, 1, 2, 1, 2), new Fleet(2, 1, 2, 1, 1, 3, 1, 2),
                new Fleet(3, 2, 1, 2, 1, 4, 1, 2), new Fleet(4, 2, 2, 2, 1, 8, 1, 2),
                new Fleet(5, 3, 1, 3, 1, 12, 1, 2)));
        state.start();

        engine.advanceTurn(state);

        assertThat(state.getSystem(1)).isEqualTo(new StarSystem(1, "Target", 0, 0, 3, 12, 2, false, 0));
        assertThat(state.systems()).extracting(StarSystem::garrison).containsExactly(12, 5, 5, 8);
        assertThat(state.fleets()).isEmpty();
        assertThat(state.gameOver()).isFalse();
        assertThat(state.reports().get(1).events()).containsExactly(
                new TurnEvent.Production(1, 1, "Target", 2),
                new TurnEvent.Reinforcement(1, 1, "Target", 5, 12, "F1,2"),
                new TurnEvent.DefenseHeld(1, 1, "Target", 12, 12, 0, 12, 12, "P2", "P1"),
                new TurnEvent.SystemLost(1, 3, 1, "Target", 12, 0, 12, 12, 0, "P3", "P1"));
        assertThat(state.reports().get(2).events()).contains(
                new TurnEvent.BattleLost(2, 1, "Target", 12, 12, 0, 12, 12, "P2", "P1"));
        assertThat(state.reports().get(3).events()).contains(
                new TurnEvent.BattleWon(3, 1, "Target", 12, 0, 12, false, 12, 0, "P3", "P1"));
    }

    @Test
    void victoryStopsTurnsAndContinuationPreservesTheOriginalThreshold() {
        GameState state = players(2);
        for (int id = 1; id <= 4; id++) {
            state.systems().add(new StarSystem(id, "S" + id, id * 100, 0,
                    id <= 3 ? 1 : 2, 5, 1, false));
        }
        state.start();
        state.submittedThisTurn().add(1);
        engine.advanceTurn(state);

        assertThat(state.turn()).isEqualTo(2);
        assertThat(state.winnerId()).isOne();
        assertThat(state.systems()).extracting(StarSystem::garrison).containsExactly(6, 6, 6, 6);
        assertThat(state.reports().get(1).events()).contains(new TurnEvent.Victory(1, 70));
        assertThat(state.reports().get(2).events()).contains(new TurnEvent.Defeat(1, "P1"));
        var finished = GameState.toSnapshot(state);
        var firstFrame = state.replayFrames().get(1);
        engine.advanceTurn(state);
        assertThat(GameState.toSnapshot(state)).isEqualTo(finished);

        Instant resumedAt = Instant.parse("2026-10-02T08:00:00Z");
        state.resumeForFullConquest(resumedAt);
        assertThat(state.victorySystemPercent()).isEqualTo(100);
        assertThat(state.turnStartedAt()).isEqualTo(resumedAt);
        assertThat(state.submittedThisTurn()).isEmpty();
        assertThat(state.gameOver()).isFalse();
        assertThat(state.winnerId()).isNull();
        assertThat(state.finishedAt()).isNull();
        state.fleets().add(new Fleet(1, 1, 1, 1, 4, 10, 2, 3));
        engine.advanceTurn(state);

        assertThat(state.turn()).isEqualTo(3);
        assertThat(state.gameOver()).isTrue();
        assertThat(state.winnerId()).isOne();
        assertThat(state.systems()).extracting(StarSystem::garrison).containsExactly(7, 7, 7, 3);
        assertThat(state.systems()).extracting(StarSystem::ownerId).containsOnly(1);
        assertThat(state.reports().get(1).events()).contains(new TurnEvent.Victory(1, 100));
        assertThat(state.replayFrames().get(1)).isEqualTo(firstFrame);
        assertThat(firstFrame.reports().get(1).events()).contains(new TurnEvent.Victory(1, 70));
    }
}
