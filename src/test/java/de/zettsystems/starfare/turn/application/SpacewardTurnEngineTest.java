package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.ai.application.SpacewardPlanning;
import de.zettsystems.starfare.combat.application.CombatService;
import de.zettsystems.starfare.economy.application.DefaultEconomyService;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.testsupport.SpacewardStates;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpacewardTurnEngineTest {
    @Test void roundProducesRoutesAndArchivesOnlyActualShipOutput() {
        var fleets = new DefaultFleetService(); var reports = new DefaultReportService();
        SpacewardPlanning ai = mock(SpacewardPlanning.class);
        var economy = new DefaultEconomyService();
        var engine = new SpacewardTurnEngine(new RoundPipeline(mock(CombatService.class), reports, fleets), economy, ai, fleets, reports);
        GameState state = SpacewardStates.running();
        fleets.addStandingOrder(state, 1, 1, 3, 7);
        fleets.addStandingOrder(state, 1, 1, 4, 3);
        economy.allocateExpansion(state, 1, 1, 4);
        assertThatCode(() -> engine.advanceTurn(state)).doesNotThrowAnyException();
        assertThat(state.turn()).isEqualTo(2);
        assertThat(state.getSystem(1).garrison()).isEqualTo(10);
        assertThat(state.fleets()).extracting(f -> f.ships()).containsExactly(7, 1);
        assertThat(state.reports().get(1).events()).contains(new TurnEvent.Production(1, 1, "Alpha", 6));
        assertThat(state.replayFrames().get(1).industries().get(1).expansionProgress()).isEqualTo(4);
        verify(ai).plan(state, fleets);
    }
    @Test void wrongRulesAndFinishedGamesNeverPlanOrProduce() {
        var fleets = new DefaultFleetService(); var reports = new DefaultReportService();
        SpacewardPlanning ai = mock(SpacewardPlanning.class);
        var engine = new SpacewardTurnEngine(new RoundPipeline(mock(CombatService.class), reports, fleets),
                new DefaultEconomyService(), ai, fleets, reports);
        assertThatThrownBy(() -> engine.advanceTurn(new GameState())).isInstanceOf(IllegalArgumentException.class);
        GameState finished = SpacewardStates.running(); finished.endGame(1);
        var before = GameState.toSnapshot(finished);
        assertThatCode(() -> engine.advanceTurn(finished)).doesNotThrowAnyException();
        assertThat(GameState.toSnapshot(finished)).isEqualTo(before);
        verifyNoInteractions(ai);
    }
}
