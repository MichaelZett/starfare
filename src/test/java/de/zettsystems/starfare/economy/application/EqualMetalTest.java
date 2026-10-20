package de.zettsystems.starfare.economy.application;

import de.zettsystems.starfare.combat.application.DefaultCombatService;
import de.zettsystems.starfare.economy.domain.MetalEconomy;
import de.zettsystems.starfare.economy.domain.RecyclingOrders;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.application.DefaultPlayerViewBuilder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.testsupport.ColonyStates;
import de.zettsystems.starfare.turn.application.EqualMetalSpacewardTurnEngine;
import de.zettsystems.starfare.turn.application.RoundPipeline;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;

class EqualMetalTest {
    private final DefaultEconomyService economy = new DefaultEconomyService();
    private final DefaultPlayerViewBuilder views = new DefaultPlayerViewBuilder();

    private static GameState ready(RulesetRef rules, long stock) {
        return ColonyStates.ready(rules, original -> {
            var stocks = new HashMap<>(original.stocks());
            stocks.put(1, stock);
            return new MetalEconomy(original.deposits(), stocks, original.mining(), original.recycling());
        });
    }

    @ParameterizedTest
    @CsvSource({"0,0,0", "1,1,0", "7,4,3", "8,4,4", "10,6,4", "14,10,4", "100,10,4"})
    void equallySharesScarceMetalCapsDemandAndKeepsUnusedStock(long stock, int alpha, int beta) {
        var state = ready(RulesetRef.SPACEWARD_EQUAL_METAL, stock);
        state.start();
        var before = GameState.toSnapshot(state);
        var forecast = state.metalPreview();
        assertThat(forecast.ships()).containsEntry(1, alpha).containsEntry(2, beta);
        assertThat(forecast.economy().stocks()).containsEntry(1, stock - alpha - beta);
        assertThat(GameState.toSnapshot(state)).isEqualTo(before);
        Collections.reverse(state.systems());
        assertThat(state.metalPreview()).isEqualTo(forecast);
        long balance = RecyclingOrders.balanceTenths(state);
        economy.produce(state, Map.of());
        assertThat(state.getSystem(1).garrison()).isEqualTo(12 + alpha);
        assertThat(state.getSystem(2).garrison()).isEqualTo(3 + beta);
        assertThat(RecyclingOrders.balanceTenths(state)).isEqualTo(balance);
    }

    @Test void satisfiedAndInactiveYardsLeaveTheirMetalForTheOthers() {
        var state = ready(RulesetRef.SPACEWARD_EQUAL_METAL, 8);
        assertThat(economy.allocateProduction(state, 1, 2, 3, 0)).isTrue();
        assertThat(state.metalPreview().ships()).containsEntry(1, 7).containsEntry(2, 1);
        assertThat(economy.allocateProduction(state, 1, 2, 4, 0)).isTrue();
        assertThat(state.metalPreview().ships()).containsEntry(1, 8).containsEntry(2, 0);
        assertThat(economy.allocateProduction(state, 1, 1, 10, 0)).isTrue();
        assertThat(state.metalPreview().ships()).containsEntry(1, 0).containsEntry(2, 0);
        assertThat(state.metalPreview().economy().stocks()).containsEntry(1, 8L);
    }

    @Test void w5KeepsProportionalSharesAndBothVersionsSurviveRestoration() {
        for (var rules : new RulesetRef[] {RulesetRef.SPACEWARD_DIRECT_ECONOMY, RulesetRef.SPACEWARD_EQUAL_METAL}) {
            var state = ready(rules, 8);
            state.start();
            var restored = GameState.fromSnapshot(GameState.toSnapshot(state));
            assertThat(restored.ruleset()).isEqualTo(rules);
            assertThat(restored.metalPreview()).isEqualTo(state.metalPreview());
            assertThat(restored.metalPreview().ships()).containsEntry(1, rules.sharesMetalEqually() ? 4 : 6)
                    .containsEntry(2, rules.sharesMetalEqually() ? 4 : 2);
        }
    }

    @Test void liveHistoricalAndRestoredViewsMatchTheRealW6Round() {
        var state = ready(RulesetRef.SPACEWARD_EQUAL_METAL, 8);
        state.start();
        var frame = state.replayFrames().get(1);
        var view = views.forPlayer(state, 1);
        assertThat(view.systems().stream().filter(system -> Integer.valueOf(1).equals(system.ownerId()))
                .map(system -> system.industry().metal().actualShips())).containsExactly(4, 4);
        var fleets = new DefaultFleetService();
        var reports = new DefaultReportService();
        var engine = new EqualMetalSpacewardTurnEngine(new RoundPipeline(new DefaultCombatService(reports), reports, fleets),
                economy, (_, _) -> { }, fleets, reports);
        engine.advanceTurn(state);
        assertThat(state.getSystem(1).garrison()).isEqualTo(16);
        assertThat(state.getSystem(2).garrison()).isEqualTo(7);
        assertThat(state.metals().orElseThrow().stocks()).containsEntry(1, 0L);
        assertThat(views.forReplay(state, frame, 1).systems().stream().filter(system -> Integer.valueOf(1).equals(system.ownerId()))
                .map(system -> system.industry().metal().actualShips())).containsExactly(4, 4);
        assertThat(GameState.fromSnapshot(GameState.toSnapshot(state)).metalPreview()).isEqualTo(state.metalPreview());
    }
}
