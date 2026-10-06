package de.zettsystems.starfare.economy.application;

import de.zettsystems.starfare.ai.application.DefaultSpacewardPlanning;
import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.combat.application.DefaultCombatService;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.application.DefaultPlayerViewBuilder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.testsupport.ColonyStates;
import de.zettsystems.starfare.turn.application.ColonySpacewardTurnEngine;
import de.zettsystems.starfare.turn.application.RoundPipeline;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.time.Instant;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class ColonyEconomyTest {
    private final EconomyService economy = new DefaultEconomyService();
    private final DefaultPlayerViewBuilder views = new DefaultPlayerViewBuilder();
    @Test void growthIncreasesNextRoundsOutputButNeverTheCurrentProduction() {
        var state = ColonyStates.running();
        assertThat(state.usableIndustrialCapacity(1)).isEqualTo(6);
        assertThat(state.getSystem(1).productionPerTurn()).isEqualTo(6);
        assertThat(economy.produce(state, Map.of()).stream().filter(result -> result.systemId() == 1).findFirst().orElseThrow().ships()).isEqualTo(6);
        assertThat(state.getSystem(1).garrison()).isEqualTo(18);
        assertThat(state.getSystem(1).productionPerTurn()).isEqualTo(7);
        assertThat(state.colonies().get(1).colony().population()).isEqualTo(711);
        assertThat(state.industries().get(1).capacity()).isEqualTo(10);
    }
    @Test void allocationUsesLaborAndInvalidRequestsPreserveTheWholeState() {
        var state = ColonyStates.running();
        var before = GameState.toSnapshot(state);
        assertThat(economy.allocateExpansion(state, 1, 1, 7)).isFalse();
        assertThat(GameState.toSnapshot(state)).isEqualTo(before);
        assertThat(economy.allocateExpansion(state, 1, 1, 3)).isTrue();
        assertThat(state.getSystem(1).productionPerTurn()).isEqualTo(3);
        economy.produce(state, Map.of());
        assertThat(state.getSystem(1).productionPerTurn()).isEqualTo(4);
        assertThat(state.industries().get(1).expansionProgress()).isEqualTo(3);
    }
    @Test void conquestPreservesColonyAndEnvironmentAndClearsAllocation() {
        var state = ColonyStates.running();
        var colony = state.colonies().get(1);
        economy.allocateExpansion(state, 1, 1, 3);
        state.updateSystem(1, system -> system.captureBy(2, 5));
        assertThat(state.colonies().get(1)).isEqualTo(colony);
        assertThat(state.industries().get(1).expansionAllocation()).isZero();
        assertThat(state.getSystem(1).productionPerTurn()).isEqualTo(6);
        assertThat(state.getSystem(1).garrisonReserve()).isZero();
    }
    @Test void neutralColoniesStayStillAndCopiesGrowIndependently() {
        var state = ColonyStates.running();
        var before = GameState.toSnapshot(state);
        var copy = GameState.copyOf(state);
        economy.produce(copy, Map.of());
        assertThat(copy.colonies().get(4)).isEqualTo(state.colonies().get(4));
        assertThat(copy.colonies().get(1)).isNotEqualTo(state.colonies().get(1));
        assertThat(GameState.toSnapshot(state)).isEqualTo(before);
        copy.resetForNewGame();
        assertThat(copy.colonies()).isEmpty();
    }
    @Test void fogOfWarAndReplayUseOnlyPermittedAndHistoricallySavedProperties() {
        var state = ColonyStates.running();
        var view = views.forPlayer(state, 1);
        var own = view.systems().stream().filter(s -> s.id() == 1).findFirst().orElseThrow();
        var enemy = view.systems().stream().filter(s -> s.id() == 3).findFirst().orElseThrow();
        assertThat(own.industry().colony().population()).isEqualTo(690);
        assertThat(own.industry().usableCapacity()).isEqualTo(6);
        assertThat(enemy.industry()).isNull();
        var illustration = SystemComposition.forVisible(new GameId("first"), own);
        assertThat(SystemComposition.forVisible(new GameId("other"), own)).isEqualTo(illustration);
        assertThat(illustration.orElseThrow().stars()).containsExactly(state.colonies().get(1).quality().star());
        assertThat(SystemComposition.forVisible(new GameId("first"), enemy)).isEmpty();
        var frame = state.replayFrames().get(1);
        economy.produce(state, Map.of());
        var replay = views.forReplay(state, frame, 1).systems().getFirst();
        assertThat(replay.industry().colony().population()).isEqualTo(690);
        assertThat(views.forObserver(state).systems().getFirst().industry().colony().population()).isEqualTo(711);
    }
    @Test void productionRelocationsRespectLimitedLaborAndGarrisonReserve() {
        var state = ColonyStates.running();
        var fleets = new DefaultFleetService();
        assertThat(fleets.addStandingOrder(state, 1, 1, 2, 6)).isPositive();
        assertThat(fleets.addStandingOrder(state, 1, 1, 2, 7)).isEqualTo(-1);
        economy.allocateExpansion(state, 1, 1, 3);
        var shipped = fleets.applyStandingOrdersForProduction(state);
        assertThat(shipped.get(1)).isEqualTo(5);
        economy.produce(state, shipped);
        assertThat(state.getSystem(1).garrison()).isEqualTo(10);
        assertThat(state.getSystem(1).productionPerTurn()).isEqualTo(4);
    }
    @ParameterizedTest @EnumSource(AiStrategy.class)
    void everyAiProfileUsesLegalColonyAllocationsAndTheRealRoundCanContinue(AiStrategy profile) {
        var state = ColonyStates.running();
        state.players().replaceAll(player -> new Player(player.id(), player.name(), true,
                player.colorHex(), player.empireName(), profile));
        var fleets = new DefaultFleetService();
        var reports = new DefaultReportService();
        var combat = new DefaultCombatService(reports);
        var ai = new DefaultSpacewardPlanning(economy, views);
        var engine = new ColonySpacewardTurnEngine(new RoundPipeline(combat, reports, fleets), economy, ai, fleets, reports);
        engine.advanceTurn(state);
        assertThat(state.turn()).isEqualTo(2);
        assertThat(state.getSystem(1).productionPerTurn()).isNotNegative();
        assertThat(state.industries().get(1).expansionAllocation()).isLessThanOrEqualTo(state.usableIndustrialCapacity(1));
        var restored = GameState.fromSnapshot(GameState.toSnapshot(state));
        engine.advanceTurn(restored);
        assertThat(restored.turn()).isEqualTo(3);
    }
    @Test void unfinishedRulesCannotBeCreatedPubliclyAndMissingPropertiesAreNeverRegenerated() {
        assertThatCode(() -> RulesetCatalog.builtIn().requireSupported(RulesetRef.SPACEWARD_COLONIES)).doesNotThrowAnyException();
        assertThatThrownBy(() -> RulesetCatalog.builtIn().requireCreatable(RulesetRef.SPACEWARD_COLONIES))
                .isInstanceOf(IllegalArgumentException.class);
        var state = new GameState();
        state.rememberSetup(GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD_COLONIES));
        state.systems().add(new StarSystem(1, "Home", 0, 0, 1, 5, 4, false));
        assertThatThrownBy(state::start).isInstanceOf(IllegalArgumentException.class);
        assertThat(state.started()).isFalse();
        assertThat(state.colonies()).isEmpty();
    }
    @Test void colonyVersionPreservesConfigurableVictoryAndFullConquestRules() {
        var state = ColonyStates.ready();
        state.configureVictoryRules(new VictoryRules(70, true, true, 70));
        state.start(); state.endGame(1);
        state.resumeForFullConquest(Instant.now());
        assertThat(state.victoryRules().allianceVictoryAllowed()).isTrue();
        assertThat(state.victoryRules().allianceSystemPercent()).isEqualTo(100);
    }
}
