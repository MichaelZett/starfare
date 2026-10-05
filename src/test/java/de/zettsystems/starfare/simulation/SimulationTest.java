package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.game.application.DefaultPlayerViewBuilder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class SimulationTest {
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void wholeMatchesAreRepeatableIncludingNonzeroCombatRandomness(boolean spaceward) {
        var scenario = scenario(spaceward, 40);
        var profiles = List.of(Strategy.RUSH, Strategy.CONCENTRATION, Strategy.DEFENSE);
        var first = MatchRunner.play(scenario, profiles);
        var second = MatchRunner.play(scenario, profiles);
        assertThat(second).isEqualTo(first);
        assertThat(first.metrics()).allSatisfy(row -> {
            assertThat(row.lost()).isNotNegative();
            assertThat(row.ships()).isNotNegative();
        });
        assertThat(first.metrics()).anySatisfy(row -> assertThat(row.lost()).isPositive());
        assertThat(first.orders()).isNotEmpty();
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void baselineAndAlternativePoliciesDoNotReactToHiddenGarrisonsOrEnemyOrders(boolean spaceward) {
        var state = hiddenState(spaceward);
        var views = new DefaultPlayerViewBuilder();
        var before = Observation.capture(state, 1, views);
        assertThat(before.view().systems().get(1).garrison()).isNull();
        var baseline = BaselinePolicy.plan(state, 1, views);
        state.updateSystem(2, system -> system.reinforce(9999));
        state.pendingOrders().put(2, List.of(new FleetOrder.Send(2, 2, 1, 10)));
        var after = Observation.capture(state, 1, views);
        assertThat(after).isEqualTo(before);
        assertThat(BaselinePolicy.plan(state, 1, views)).isEqualTo(baseline);
        for (var strategy : Strategy.forRules(state.ruleset())) {
            if (strategy != Strategy.BASELINE) {
                assertThat(HeuristicPolicy.plan(strategy, after)).isEqualTo(HeuristicPolicy.plan(strategy, before));
            }
        }
    }

    @Test void planningDoesNotMutateStateAndInvestmentDiffersFromRush() {
        var state = scenario(true, 30).create(2);
        var before = GameState.toSnapshot(state);
        var engine = new SimulationEngine();
        var rush = engine.plan(state, List.of(Strategy.RUSH, Strategy.DEFENSE)).getFirst();
        var industry = engine.plan(state, List.of(Strategy.INDUSTRY, Strategy.DEFENSE)).getFirst();
        assertThat(GameState.toSnapshot(state)).isEqualTo(before);
        assertThat(rush.expansion().values()).containsOnly(0);
        assertThat(industry.expansion().values()).anySatisfy(value -> assertThat(value).isPositive());
        assertThat(industry).isNotEqualTo(rush);
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void checkpointContinuationMatchesFullRunAndPreservesSource(boolean spaceward) {
        var scenario = scenario(spaceward, 25);
        var profiles = List.of(Strategy.CONCENTRATION, Strategy.RUSH);
        var checkpoint = MatchRunner.checkpoint(scenario, profiles, 5);
        var snapshot = GameState.toSnapshot(checkpoint);
        var fork = MatchRunner.continueFrom(scenario, profiles, checkpoint, "fork", SimulationEngine.Intervention.NONE);
        var original = MatchRunner.play(scenario, profiles);
        var lastOriginal = original.metrics().getLast();
        var lastFork = fork.metrics().getLast();
        assertThat(fork.winner()).isEqualTo(original.winner());
        assertThat(fork.outcome()).isEqualTo(original.outcome());
        assertThat(lastFork.ships()).isEqualTo(lastOriginal.ships());
        assertThat(lastFork.systems()).isEqualTo(lastOriginal.systems());
        assertThat(fork.orders()).isEqualTo(original.orders().stream()
                .filter(row -> Integer.parseInt(row.substring(0, row.indexOf(','))) >= 5).toList());
        assertThat(GameState.toSnapshot(checkpoint)).isEqualTo(snapshot);
    }

    @Test void decisionsOnlyChangeTheNamedActionOfTheFirstPlayer() {
        var state = scenario(true, 20).create(2);
        var engine = new SimulationEngine();
        var profiles = List.of(Strategy.RUSH, Strategy.DEFENSE);
        var plans = engine.plan(state, profiles);
        var omitted = SimulationEngine.intervene(state, plans.getFirst(), SimulationEngine.Intervention.OMIT_FIRST_SEND);
        assertThat(omitted.sends()).isEqualTo(plans.getFirst().sends().stream().skip(1).toList());
        assertThat(omitted.expansion()).isEqualTo(plans.getFirst().expansion());
        var changed = engine.advance(state, profiles, 72, SimulationEngine.Intervention.EXPAND_FIRST);
        assertThat(changed.get(1)).isEqualTo(plans.get(1));
        assertThat(changed.getFirst().sends()).isEqualTo(plans.getFirst().sends());
        assertThat(changed.getFirst().expansion().values()).contains(5);
    }

    @Test void roundLimitIsNotReportedAsDrawAndMetricsBalanceShips() {
        var match = MatchRunner.play(scenario(false, 1), List.of(Strategy.DEFENSE, Strategy.DEFENSE));
        assertThat(match.outcome()).isEqualTo("ROUND_LIMIT");
        assertThat(match.winner()).isZero();
        assertThat(match.rounds()).isEqualTo(1);
        assertThat(match.metrics()).allSatisfy(row -> assertThat(row.ships()).isEqualTo(20 + row.produced() - row.lost()));
    }

    @Test void mapAndCombatSeedsAreIndependentAndSeatsDoNotDependOnStrategy() {
        var scenario = scenario(true, 25);
        var sameMap = new Scenario(scenario.rules(), scenario.layout(), scenario.systems(), scenario.mapSeed(), 999, 25, 10);
        assertThat(sameMap.create(2).systems()).isEqualTo(scenario.create(2).systems());
        assertThat(sameMap.create(2).navigationSettings()).isEqualTo(scenario.create(2).navigationSettings());
        assertThat(new SimulationEngine().plan(scenario.create(2), List.of(Strategy.BASELINE, Strategy.RUSH))).hasSize(2);
    }

    @Test void cachedRoutesProduceTheSameOrdersAsFreshSearchAfterConquest() {
        var state = scenario(true, 30).create(2);
        var cached = new SimulationEngine();
        var strategies = List.of(Strategy.CONCENTRATION, Strategy.RUSH);
        cached.plan(state, strategies);
        int target = state.systems().stream().filter(StarSystem::neutral).findFirst().orElseThrow().id();
        state.updateSystem(target, system -> system.captureBy(1, 40));
        assertThat(cached.plan(state, strategies)).isEqualTo(new SimulationEngine().plan(state, strategies));
    }

    @Test void reportsContainRawEvidenceAndSeparateUnfinishedMatches(@TempDir Path output) throws java.io.IOException {
        var result = MatchRunner.play(scenario(false, 1), List.of(Strategy.RUSH, Strategy.DEFENSE));
        BenchmarkReport.write(output, List.of(result), List.of("no effective intervention"));
        assertThat(Files.readString(output.resolve("matches.csv"))).contains("ROUND_LIMIT", result.digest());
        assertThat(Files.readString(output.resolve("summary.md"))).contains("nicht bestimmbar", "Limit", "RUSH", "DEFENSE");
        assertThat(Files.readString(output.resolve("orders.csv"))).contains("send");
        assertThat(Files.readString(output.resolve("rounds.csv"))).contains("produced_cumulative");
    }

    @Test void malformedCommandsAndUnsupportedProfilesFailBeforeRunning() {
        assertThatThrownBy(() -> BenchmarkOptions.parse(new String[]{"--typo=1"})).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BenchmarkOptions.parse(new String[]{"--seeds=0"})).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BenchmarkOptions.parse(new String[]{"--mode=alliance"})).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SimulationEngine().plan(scenario(false, 20).create(2), List.of(Strategy.INDUSTRY, Strategy.RUSH)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest @org.junit.jupiter.params.provider.EnumSource(value = Strategy.class)
    void eachSpacewardPolicyCanPlayWithoutInvalidOrders(Strategy strategy) {
        assertThatCode(() -> MatchRunner.play(scenario(true, 15), List.of(strategy, Strategy.RUSH))).doesNotThrowAnyException();
    }

    @Test void forkDeltasCompareOnlyMatchingRounds(@TempDir Path output) throws java.io.IOException {
        var scenario = scenario(true, 8);
        var strategies = List.of(Strategy.INDUSTRY, Strategy.RUSH);
        var state = scenario.create(2);
        var baseline = MatchRunner.continueFrom(scenario, strategies, state, "fork-1-NONE", SimulationEngine.Intervention.NONE);
        var changed = MatchRunner.continueFrom(scenario, strategies, state, "fork-1-SHIPS_FIRST", SimulationEngine.Intervention.SHIPS_FIRST);
        BenchmarkReport.write(output, List.of(baseline, changed), List.of());
        assertThat(Files.readAllLines(output.resolve("decision-deltas.csv"))).hasSize(10);
        assertThat(changed.metrics().stream().filter(row -> row.round() == 1 && row.player() == 1).findFirst().orElseThrow().produced())
                .isGreaterThan(baseline.metrics().stream().filter(row -> row.round() == 1 && row.player() == 1).findFirst().orElseThrow().produced());
    }

    @ParameterizedTest @ValueSource(strings = {"matrix", "multiplayer", "alliance", "decision"})
    void commandLineModesProduceEvidenceWithoutSpring(String mode, @TempDir Path output) throws java.io.IOException {
        StrategyBenchmark.main(new String[]{"--mode=" + mode, "--rules=spaceward", "--layouts=EVEN", "--systems=8",
                "--seeds=1", "--rounds=12", "--fork-round=1", "--output=" + output});
        assertThat(Files.readString(output.resolve("invocation.txt"))).contains("source-sha256=", "java=");
        assertThat(Files.readString(output.resolve("matches.csv"))).contains("spaceward,1.0.0");
        assertThat(Files.readString(output.resolve("summary.md"))).contains("Strategiepruefstand");
    }

    private static Scenario scenario(boolean spaceward, int rounds) {
        return new Scenario(spaceward ? RulesetRef.SPACEWARD : RulesetRef.SECTOR_FORCES, GalaxyLayout.EVEN, 8, 7, 123, rounds, 10);
    }

    private static GameState hiddenState(boolean spaceward) {
        var state = new GameState();
        state.resetForNewGame();
        state.rememberSetup(GameSetup.defaults().selectRuleset(spaceward ? RulesetRef.SPACEWARD : RulesetRef.SECTOR_FORCES));
        state.players().addAll(List.of(new Player(1, "One", true, "#ffffff"), new Player(2, "Two", true, "#ff0000")));
        state.systems().add(new StarSystem(1, "Home", 100, 100, 1, 50, 5, false));
        state.systems().add(new StarSystem(2, "Hidden", 3100, 1900, 2, 20, 5, false));
        state.start();
        return state;
    }
}
