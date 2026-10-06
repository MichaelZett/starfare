package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.game.values.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AiStrategySetupTest {
    @Test
    void selectionsSurviveNormalizationAndRuleOptionsWhileUnsupportedProfilesFallBack() {
        var setup = setup().selectRuleset(RulesetRef.SPACEWARD_ALLIANCE)
                .chooseAiStrategies(List.of(AiStrategy.INDUSTRY_ADAPTIVE, AiStrategy.RUSH))
                .chooseVictoryRules(new VictoryRules(80, true, true, 90)).normalized();
        assertThat(setup.aiStrategies()).containsExactly(AiStrategy.INDUSTRY_ADAPTIVE, AiStrategy.RUSH);
        assertThat(setup.selectRuleset(RulesetRef.SECTOR_FORCES).normalized().aiStrategies())
                .containsExactly(AiStrategy.BASELINE, AiStrategy.RUSH);
        assertThat(setup.strategyForAi(99)).isEqualTo(AiStrategy.BASELINE);
        assertThat(setup().normalized().aiStrategies()).containsExactly(AiStrategy.BASELINE, AiStrategy.BASELINE);
        assertThat(AiStrategy.forRules(RulesetRef.SECTOR_FORCES)).hasSize(5).noneMatch(AiStrategy::requiresIndustry);
        assertThat(AiStrategy.forRules(RulesetRef.SPACEWARD_ALLIANCE)).containsExactly(AiStrategy.values());
    }

    @Test
    void registryAssignsEachAiSeatAndTemplatesKeepProfiles() {
        var registry = new DefaultGameRegistry(new InMemoryGameSessionStore(), new DefaultGameAccessPolicy());
        var requested = setup().chooseAiStrategies(List.of(AiStrategy.RUSH, AiStrategy.DEFENSE));
        var id = registry.createGame(requested, "host", "Profiles");
        List<Player> originalPlayers = registry.readState(id, state -> List.copyOf(state.players()));
        assertThat(originalPlayers).extracting(Player::aiStrategy)
                .containsExactly(AiStrategy.BASELINE, AiStrategy.RUSH, AiStrategy.DEFENSE);
        var templates = new DefaultGameTemplateService(registry, org.mockito.Mockito.mock(GameArchiveStore.class),
                new DefaultGameAccessPolicy(), org.mockito.Mockito.mock(Broadcaster.class));
        var copy = templates.create(id, "host", "Copy", true).orElseThrow();
        List<AiStrategy> copiedProfiles = registry.readState(copy, state -> state.originalSetup().orElseThrow().aiStrategies());
        assertThat(copiedProfiles)
                .containsExactly(AiStrategy.RUSH, AiStrategy.DEFENSE);
        List<Player> copiedPlayers = registry.readState(copy, state -> List.copyOf(state.players()));
        assertThat(copiedPlayers).extracting(Player::aiStrategy)
                .containsExactly(AiStrategy.BASELINE, AiStrategy.RUSH, AiStrategy.DEFENSE);
    }

    @Test
    void humanAiHandoverKeepsTheSeatsProfile() {
        var player = new Player(2, "Pilot", true, "#fff", "Empire", AiStrategy.CONCENTRATION);
        assertThat(player.asHuman().identifiedAs("New pilot", "New empire").asAi().aiStrategy())
                .isEqualTo(AiStrategy.CONCENTRATION);
    }

    private static GameSetup setup() {
        var defaults = GameSetup.defaults();
        return new GameSetup(16, 1, 2, List.of(5, 5, 5), 1, 10, 20, true, true,
                defaults.seatColorHexes(), defaults.productionDistribution(), defaults.galaxyLayout(), false,
                10, defaults.roundRules(), 70);
    }
}
