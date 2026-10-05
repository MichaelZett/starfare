package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.diplomacy.values.*;
import de.zettsystems.starfare.game.values.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;

class AllianceBenchmarkTest {
    @Test void pairedElevenAndFiveScenarioEndsAsGroupVictoryOnlyWhenEnabled(@TempDir Path output) throws java.io.IOException {
        var base = new Scenario(RulesetRef.SPACEWARD_ALLIANCE, GalaxyLayout.EVEN, 16, 7, 123, 2, 10);
        var enabled = base.withVictoryRules(new VictoryRules(70, true, true, 70));
        var disabled = base.withVictoryRules(new VictoryRules(70, true, false, 70));
        var profiles = List.of(Strategy.DEFENSE, Strategy.DEFENSE, Strategy.DEFENSE);
        var won = MatchRunner.continueFrom(enabled, profiles, checkpoint(enabled), "group-on", SimulationEngine.Intervention.NONE);
        var open = MatchRunner.continueFrom(disabled, profiles, checkpoint(disabled), "group-off", SimulationEngine.Intervention.NONE);
        assertThat(won.outcome()).isEqualTo("WIN");
        assertThat(won.winners()).containsExactly(1, 2);
        assertThat(won.winner()).isZero();
        assertThat(won.allianceId()).isEqualTo(1);
        assertThat(open.outcome()).isEqualTo("ROUND_LIMIT");
        assertThat(open.winners()).isEmpty();
        assertThat(MatchRunner.continueFrom(enabled, profiles, checkpoint(enabled), "group-on", SimulationEngine.Intervention.NONE)).isEqualTo(won);
        BenchmarkReport.write(output, List.of(won, open), List.of());
        assertThat(Files.readString(output.resolve("matches.csv"))).contains("winners,alliance_id", "1/2,1,true,true,70,70");
        assertThat(Files.readString(output.resolve("summary.md"))).contains("group-on", "group-off");
    }

    @Test void commandLineCanPairTheNewVersionAndRejectsLegacyGroupVictory(@TempDir Path output) throws java.io.IOException {
        StrategyBenchmark.main(new String[]{"--mode=alliance", "--rules=spaceward-1.1", "--alliance-victory=both",
                "--seeds=1", "--layouts=EVEN", "--systems=8", "--rounds=3", "--output=" + output});
        var lines = Files.readAllLines(output.resolve("matches.csv"));
        assertThat(lines).hasSize(9);
        assertThat(String.join("\n", lines)).contains("group-win-true", "group-win-false", "spaceward,1.1.0");
        assertThatThrownBy(() -> BenchmarkOptions.parse(new String[]{"--rules=spaceward", "--alliance-victory=true"}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static de.zettsystems.starfare.game.domain.GameState checkpoint(Scenario scenario) {
        var state = scenario.create(3);
        for (int system = 1; system <= 16; system++) {
            int owner = system <= 11 ? 1 : 2;
            state.updateSystem(system, s -> s.captureBy(owner, 1));
        }
        state.agreeTreaties(new DiplomacyState(2, List.of(new AllianceGroup(1, Set.of(1, 2), 3, Map.of())), List.of()));
        return state;
    }
}
