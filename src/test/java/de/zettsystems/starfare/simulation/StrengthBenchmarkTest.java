package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.game.application.DefaultPlayerViewBuilder;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.ComputerStrength;
import de.zettsystems.starfare.game.values.GameConfig;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.RandomnessLevel;
import de.zettsystems.starfare.game.values.RulesetRef;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class StrengthBenchmarkTest {
    @Test
    void lobbyGamesPutTheReferenceOnTheHumanSeatAndAreRepeatable() {
        var options = StrengthOptions.parse(new String[]{"--systems=16", "--players=2", "--seeds=2", "--rounds=40",
                "--strengths=EASY,HARD", "--threads=2"});
        var first = StrengthBenchmark.run(options);
        assertThat(StrengthBenchmark.run(options)).isEqualTo(first);
        assertThat(first).hasSize(4).allSatisfy(match -> assertThat(match.computers()).hasSize(1));
        assertThat(first).filteredOn(match -> match.strength() == ComputerStrength.EASY)
                .allSatisfy(match -> assertThat(match.computers()).containsOnly(Strategy.BASELINE));
        var state = StrengthBenchmark.create(options, StrengthBenchmark.setup(options, ComputerStrength.EASY,
                RandomnessLevel.LOW, true, 16, 2), 1);
        assertThat(state.players().getFirst().ai()).isFalse();
        assertThat(StrengthBenchmark.strategies(options, state, 1)).containsExactly(Strategy.BASELINE, Strategy.BASELINE);
        int computerHome = state.systems().stream().filter(system -> Objects.equals(system.ownerId(), 2)).findFirst()
                .orElseThrow().productionPerTurn();
        assertThat(computerHome).isEqualTo(ComputerStrength.EASY.computerStart(RulesetRef.SECTOR_FORCES, GameConfig.START_PRODUCTION));
    }

    @Test
    void withoutRandomnessTheCombatSeedChangesNothing() {
        var options = StrengthOptions.parse(new String[]{"--systems=16", "--players=2", "--seeds=1", "--rounds=60",
                "--strengths=NORMAL", "--randomness=NONE", "--combat-seeds=2"});
        var matches = StrengthBenchmark.run(options);
        assertThat(matches).hasSize(2);
        assertThat(matches.get(1).digest()).isEqualTo(matches.get(0).digest());
    }

    @Test
    void overridesReplaceTheStartShareAndTheComputerProfiles() {
        var options = StrengthOptions.parse(new String[]{"--systems=24", "--players=4", "--start-share=2",
                "--computer-profiles=RUSH,DEFENSE", "--reference=EXPANSION"});
        var state = StrengthBenchmark.create(options, StrengthBenchmark.setup(options, ComputerStrength.EASY,
                RandomnessLevel.HIGH, false, 24, 4), 3);
        assertThat(state.sectorOptions().enabledEvents()).isEmpty();
        assertThat(state.combatRandomnessPercent()).isEqualTo(RandomnessLevel.HIGH.combatRandomnessPercent());
        assertThat(state.systems().stream().filter(system -> Objects.equals(system.ownerId(), 4)).findFirst().orElseThrow()
                .productionPerTurn()).isEqualTo(2 * GameConfig.START_PRODUCTION);
        var strategies = StrengthBenchmark.strategies(options, state, 3);
        assertThat(strategies.getFirst()).isEqualTo(Strategy.EXPANSION);
        assertThat(strategies.subList(1, 4)).isSubsetOf(Strategy.RUSH, Strategy.DEFENSE);
    }

    @Test
    void theBaselineIgnoresTheProfileTheGameStoredForTheSeat() {
        var options = StrengthOptions.parse(new String[]{"--systems=24", "--players=2", "--computer-profiles=BASELINE"});
        var state = StrengthBenchmark.create(options, StrengthBenchmark.setup(options, ComputerStrength.HARD,
                RandomnessLevel.LOW, true, 24, 2), 1);
        var stored = GameState.copyOf(state);
        state.players().replaceAll(player -> player.id() == 2 ? new Player(player.id(), player.name(), true,
                player.colorHex(), player.empireName(), AiStrategy.RUSH) : player);
        stored.players().replaceAll(player -> player.id() == 2 ? new Player(player.id(), player.name(), true,
                player.colorHex(), player.empireName(), AiStrategy.BASELINE) : player);
        var views = new DefaultPlayerViewBuilder();
        for (int round = 0; round < 6; round++) {
            assertThat(BaselinePolicy.plan(state, 2, views)).isEqualTo(BaselinePolicy.plan(stored, 2, views));
            new SimulationEngine(true).advance(state, List.of(Strategy.BASELINE, Strategy.BASELINE), 1, SimulationEngine.Intervention.NONE);
            new SimulationEngine(true).advance(stored, List.of(Strategy.BASELINE, Strategy.BASELINE), 1, SimulationEngine.Intervention.NONE);
        }
    }

    @Test
    void reportsShowRatesLengthsLuckAndMaps(@TempDir Path output) throws IOException {
        StrengthBenchmark.main(new String[]{"--mode=strength", "--systems=16", "--players=2", "--seeds=2", "--rounds=30",
                "--strengths=NORMAL", "--events=on,off", "--combat-seeds=2", "--output=" + output});
        String summary = Files.readString(output.resolve("strength.md"));
        assertThat(summary).contains("| NORMAL | LOW | an | 16 | 2 | 4 |", "## Nach Computerprofil", "## Kampfstartwert",
                "## Ereignisse", "## Karten");
        assertThat(Files.readAllLines(output.resolve("matches.csv"))).hasSize(9);
        assertThat(output.resolve("invocation.txt")).exists();
    }

    @Test
    void invalidDimensionsFailBeforeRunning() {
        for (var arguments : List.of(new String[]{"--systems=20"}, new String[]{"--systems=16", "--players=9"},
                new String[]{"--computer-profiles=INDUSTRY"}, new String[]{"--start-share=5"}, new String[]{"--events=maybe"},
                new String[]{"--seeds=1", "--seeds=2"}, new String[]{"--unknown=1"})) {
            assertThatIllegalArgumentException().isThrownBy(() -> StrengthOptions.parse(arguments));
        }
    }

    @Test
    void wilsonIntervalsAndQuantilesStayWithinTheirRange() {
        assertThat(StrengthReport.wilson(0, 10)).startsWith("0.0%");
        assertThat(StrengthReport.wilson(10, 10)).endsWith("100.0%");
        assertThat(StrengthReport.wilson(50, 100)).isEqualTo("40.4% .. 59.6%");
        assertThat(StrengthReport.quantile(new int[]{1, 2, 3, 4}, 0.5)).isEqualTo("3");
        assertThat(StrengthReport.quantile(new int[0], 0.5)).isEqualTo("-");
    }
}
