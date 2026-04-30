package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.ai.application.DefaultAiService;
import de.zettsystems.starfare.combat.application.DefaultCombatService;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.report.application.ReportService;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class TurnEngineTest {

    @Test
    void waitDelaysArrivalAndClearsMarker() {
        GameState state = new GameState();
        state.players().add(new Player(1, "P1", false, "#fff"));
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 5, 2, false));
        state.intel().put(1, new HashMap<>());

        Fleet fleet = new Fleet(1, 1, 1, 1, 1, 3, state.turn(), state.turn() + 1);
        state.fleets().add(fleet);
        state.waitThisTurn().add(fleet.globalId());

        TurnEngine engine = new DefaultTurnEngine(new DefaultCombatService(new DefaultReportService()), new DefaultAiService(), new DefaultReportService(), new DefaultFleetService());
        engine.advanceTurn(state);

        assertThat(state.waitThisTurn()).isEmpty();
        assertThat(state.fleets()).hasSize(1);
        assertThat(state.fleets().getFirst().arrivalTurn()).isEqualTo(state.turn() + 1);
    }

    @Test
    void reinforcementAddsGarrisonAndReport() {
        GameState state = new GameState();
        state.players().add(new Player(1, "P1", false, "#fff"));
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 5, 2, false));
        state.systems().add(new StarSystem(2, "S2", 100, 0, 1, 4, 0, false));
        state.intel().put(1, new HashMap<>());

        Fleet fleet = new Fleet(1, 1, 1, 1, 2, 3, state.turn(), state.turn() + 1);
        state.fleets().add(fleet);

        ReportService reportService = new DefaultReportService();
        TurnEngine engine = new DefaultTurnEngine(new DefaultCombatService(reportService), new DefaultAiService(), reportService, new DefaultFleetService());
        engine.advanceTurn(state);

        assertThat(state.getSystem(2).garrison()).isEqualTo(7);
        assertThat(state.reports().get(1).lines().stream().anyMatch(l -> l.contains("Verstärkung"))).isTrue();
        assertThat(state.fleets()).hasSize(0);
    }
}
