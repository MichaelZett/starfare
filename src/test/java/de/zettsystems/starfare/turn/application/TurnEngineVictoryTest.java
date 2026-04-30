package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.ai.application.DefaultAiService;
import de.zettsystems.starfare.combat.application.DefaultCombatService;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.report.application.ReportService;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class TurnEngineVictoryTest {

    @Test
    void victoryReportWhenOwningMajority() {
        GameState state = new GameState();
        state.players().add(new Player(1, "P1", false, "#fff"));
        state.players().add(new Player(2, "P2", false, "#000"));
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 5, 0, false));
        state.systems().add(new StarSystem(2, "S2", 50, 0, 1, 5, 0, false));
        state.systems().add(new StarSystem(3, "S3", 100, 0, 2, 5, 0, false));
        state.intel().put(1, new HashMap<>());
        state.intel().put(2, new HashMap<>());

        ReportService reportService = new DefaultReportService();
        TurnEngine engine = new DefaultTurnEngine(new DefaultCombatService(reportService), new DefaultAiService(), reportService, new DefaultFleetService());

        engine.advanceTurn(state);

        assertThat(state.reports().get(1).lines().stream().anyMatch(l -> l.contains("Sieg"))).isTrue();
    }

    @Test
    void gameOverStopsFurtherTurns() {
        GameState state = new GameState();
        state.players().add(new Player(1, "P1", false, "#fff"));
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 5, 0, false));
        state.systems().add(new StarSystem(2, "S2", 50, 0, 1, 5, 0, false));
        state.intel().put(1, new HashMap<>());

        ReportService reportService = new DefaultReportService();
        TurnEngine engine = new DefaultTurnEngine(new DefaultCombatService(reportService), new DefaultAiService(), reportService, new DefaultFleetService());
        int before = state.turn();

        engine.advanceTurn(state);
        engine.advanceTurn(state);

        assertThat(state.gameOver()).isTrue();
        assertThat(state.turn()).isEqualTo(before + 1);
    }
}
