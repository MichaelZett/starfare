package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.ai.application.AiService;
import de.zettsystems.starfare.ai.application.DefaultAiService;
import de.zettsystems.starfare.combat.application.CombatService;
import de.zettsystems.starfare.combat.application.DefaultCombatService;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.fleet.application.FleetService;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.report.application.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FleetOrdersTurnTest {

    private TurnEngine engine;
    private GameState state;

    @BeforeEach
    void setUp() {
        ReportService report = new DefaultReportService();
        FleetService fleet = new DefaultFleetService();
        CombatService combat = new DefaultCombatService(report);
        AiService ai = new DefaultAiService();
        engine = new DefaultTurnEngine(combat, ai, report, fleet);

        state = new GameState();
        state.players().add(new Player(1, "P1", false, "#fff"));
        state.players().add(new Player(2, "P2", false, "#000"));
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 10, 2, false));
        state.systems().add(new StarSystem(2, "S2", 2000, 0, 2, 10, 2, false));
        state.intel().put(1, new java.util.HashMap<>());
        state.intel().put(2, new java.util.HashMap<>());
    }

    @Test
    void sendOrderCreatesFleetAndReducesGarrison() {
        state.pendingOrders().put(1, new ArrayList<>(List.of(new FleetOrder.Send(1, 1, 2, 4))));

        engine.advanceTurn(state);

        assertThat(state.getSystem(1).garrison()).isEqualTo(6 + 2); // -4 ships + production
        assertThat(state.fleets()).isNotEmpty();
    }

    @Test
    void waitOrderDelaysFleet() {
        // Create a fleet first (immediate, as AI would)
        FleetService fleet = new DefaultFleetService();
        fleet.sendFleet(state, 1, 1, 2, 3);
        int fleetId = state.fleets().getFirst().globalId();
        int originalArrival = state.fleets().getFirst().arrivalTurn();

        state.pendingOrders().put(1, new ArrayList<>(List.of(new FleetOrder.Wait(1, fleetId))));

        engine.advanceTurn(state);

        // Fleet should still exist, arrival pushed by 1
        assertThat(state.fleets()).hasSize(1);
        assertThat(state.fleets().getFirst().arrivalTurn()).isEqualTo(originalArrival + 1);
    }

    @Test
    void disbandOrderRemovesFleetAndRestoresGarrison() {
        FleetService fleet = new DefaultFleetService();
        fleet.sendFleet(state, 1, 1, 2, 3);
        int fleetId = state.fleets().getFirst().globalId();
        int garrisonAfterSend = state.getSystem(1).garrison(); // 7

        state.pendingOrders().put(1, new ArrayList<>(List.of(new FleetOrder.Disband(1, fleetId))));

        engine.advanceTurn(state);

        assertThat(state.fleets()).isEmpty();
        // garrison = restored(+3) + production(+2)
        assertThat(state.getSystem(1).garrison()).isEqualTo(garrisonAfterSend + 3 + 2);
    }

    @Test
    void ordersAreClearedAfterTick() {
        state.pendingOrders().put(1, new ArrayList<>(List.of(new FleetOrder.Send(1, 1, 2, 2))));

        engine.advanceTurn(state);

        assertThat(state.pendingOrders()).isEmpty();
    }

    @Test
    void overcommittedSendOrderIsSilentlyRejected() {
        // Queue a send that exceeds garrison at apply time (garrison was mutated before apply, e.g. by another order)
        // Simulate: queue sends 100 ships from S1 which only has 10
        state.pendingOrders().put(1, new ArrayList<>(List.of(new FleetOrder.Send(1, 1, 2, 100))));

        engine.advanceTurn(state); // must not throw

        // Fleet was not created; garrison still there (+ production)
        assertThat(state.getSystem(1).garrison()).isEqualTo(10 + 2);
        assertThat(state.fleets()).isEmpty();
        assertThat(state.pendingOrders()).isEmpty();
    }
}
