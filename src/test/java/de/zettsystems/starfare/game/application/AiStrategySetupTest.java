package de.zettsystems.starfare.game.application;

import static org.mockito.Mockito.mock;
import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.game.values.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AiStrategySetupTest {
    @Test
    void theComputerStrengthDecidesWhichProfilesTheComputersDraw() {
        assertThat(AiStrategy.forRules(RulesetRef.SECTOR_FORCES)).hasSize(5).noneMatch(AiStrategy::requiresIndustry);
        assertThat(AiStrategy.forRules(RulesetRef.SPACEWARD_ALLIANCE)).containsExactly(AiStrategy.values());
        var registry = new DefaultGameRegistry(new InMemoryGameSessionStore(), new DefaultGameAccessPolicy());

        var easy = registry.createGame(setup().chooseComputerStrength(ComputerStrength.EASY), "host", "Easy");
        List<Player> easyPlayers = registry.readState(easy, state -> List.copyOf(state.players()));
        assertThat(easyPlayers).extracting(Player::aiStrategy).containsOnly(AiStrategy.BASELINE);

        var normal = registry.createGame(setup(), "host", "Normal");
        List<Player> normalPlayers = registry.readState(normal, state -> List.copyOf(state.players()));
        // Normal classic computers plan like the baseline: other profiles were unfair in duels and larger games.
        assertThat(normalPlayers.subList(1, 3)).extracting(Player::aiStrategy).containsOnly(AiStrategy.BASELINE);

        var duel = registry.createGame(setup().seatPlayers(1, 1), "host", "Duel");
        List<Player> duelPlayers = registry.readState(duel, state -> List.copyOf(state.players()));
        assertThat(duelPlayers.get(1).aiStrategy()).isEqualTo(AiStrategy.BASELINE);
    }

    @Test
    void templatesKeepTheComputerStrength() {
        var registry = new DefaultGameRegistry(new InMemoryGameSessionStore(), new DefaultGameAccessPolicy());
        var id = registry.createGame(setup().chooseComputerStrength(ComputerStrength.HARD), "host", "Profiles");
        var templates = new DefaultGameTemplateService(registry, mock(GameArchiveStore.class),
                new DefaultGameAccessPolicy(), mock(Broadcaster.class));
        var copy = templates.create(id, "host", "Copy", true).orElseThrow();
        ComputerStrength copied = registry.readState(copy, state -> state.originalSetup().orElseThrow().computerStrength());
        assertThat(copied).isEqualTo(ComputerStrength.HARD);
        // Three empires with hard classic computers: the baseline led the strategy bench in multiplayer games.
        List<Player> copiedPlayers = registry.readState(copy, state -> List.copyOf(state.players()));
        assertThat(copiedPlayers.subList(1, 3)).extracting(Player::aiStrategy).containsOnly(AiStrategy.BASELINE);
    }

    @Test
    void humanAiHandoverKeepsTheSeatsProfile() {
        var player = new Player(2, "Pilot", true, "#fff", "Empire", AiStrategy.CONCENTRATION);
        assertThat(player.asHuman().identifiedAs("New pilot", "New empire").asAi().aiStrategy())
                .isEqualTo(AiStrategy.CONCENTRATION);
    }

    private static GameSetup setup() {
        return GameSetup.defaults().seatPlayers(1, 2).chooseGalaxy(16, GalaxyLayout.EVEN);
    }
}
