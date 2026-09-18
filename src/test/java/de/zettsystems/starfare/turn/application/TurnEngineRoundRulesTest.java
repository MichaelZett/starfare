package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.ai.application.DefaultAiService;
import de.zettsystems.starfare.combat.application.CombatService;
import de.zettsystems.starfare.combat.application.DefaultCombatService;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.AttackOrder;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.RoundRules;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.report.application.DefaultReportService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TurnEngineRoundRulesTest {

    private static final int TARGET = 9;

    /** Drei Angreifer mit 5, 9 und 5 Schiffen auf dasselbe neutrale System. */
    private static GameState threeAttackers(AttackOrder order) {
        GameState state = new GameState();
        state.configureRoundRules(new RoundRules(null, null, order));
        state.systems().add(new StarSystem(TARGET, "Ziel", 0, 0, null, 3, 1, true));
        int[] ships = {5, 9, 5};
        for (int i = 0; i < ships.length; i++) {
            int playerId = i + 2;
            state.players().add(new Player(playerId, "P" + playerId, false, "#fff"));
            state.intel().put(playerId, new HashMap<>());
            state.fleets().add(new Fleet(playerId, playerId, 1, TARGET, TARGET, ships[i], state.turn(), state.turn() + 1));
        }
        return state;
    }

    private static List<Integer> attackSequence(GameState state) {
        List<Integer> attackers = new ArrayList<>();
        CombatService recording = (_, attackerId, _, _, _) -> attackers.add(attackerId);
        new DefaultTurnEngine(recording, new DefaultAiService(), new DefaultReportService(), new DefaultFleetService())
                .advanceTurn(state);
        return attackers;
    }

    @Test
    void strongestFirstOrdersBySizeThenByPlayerId() {
        assertThat(attackSequence(threeAttackers(AttackOrder.STRONGEST_FIRST))).containsExactly(3, 2, 4);
    }

    @Test
    void randomOrderIsDrawnAnewEachRound() {
        Set<List<Integer>> seen = new HashSet<>();
        for (int i = 0; i < 200 && seen.size() < 6; i++) {
            seen.add(attackSequence(threeAttackers(AttackOrder.RANDOM)));
        }
        assertThat(seen).as("alle sechs Reihenfolgen treten auf").hasSize(6);
    }

    @Test
    void aiOrdersLaunchInTheSameRoundAsHumanOrders() {
        GameState state = new GameState();
        state.players().add(new Player(1, "Mensch", false, "#fff"));
        state.players().add(new Player(2, "KI", true, "#000"));
        state.systems().add(new StarSystem(1, "Heim", 0, 0, 1, 10, 2, false));
        state.systems().add(new StarSystem(2, "KI-Heim", 400, 0, 2, 10, 2, false));
        state.systems().add(new StarSystem(3, "Neutral", 200, 0, null, 4, 2, true));
        state.intel().put(1, new HashMap<>());
        state.intel().put(2, new HashMap<>());
        new DefaultFleetService().queueSend(state, 1, 1, 3, 4);
        int round = state.turn();

        var reportService = new DefaultReportService();
        new DefaultTurnEngine(new DefaultCombatService(reportService), new DefaultAiService(), reportService,
                new DefaultFleetService()).advanceTurn(state);

        assertThat(state.fleets()).extracting(Fleet::ownerId).containsExactlyInAnyOrder(1, 2);
        assertThat(state.fleets()).extracting(Fleet::launchTurn).containsOnly(round);
    }
}
