package de.zettsystems.starfare.ai.application;

import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiServiceTest {

    @Test
    void aiChoosesStrongestBaseAndCreatesFleet() {
        GameState state = new GameState();
        state.players().add(new Player(1, "AI1", true, "#fff"));
        state.systems().add(new StarSystem(1, "A1", 0, 0, 1, 10, 2, false));
        state.systems().add(new StarSystem(2, "A2", 50, 0, 1, 3, 2, false));
        state.systems().add(new StarSystem(3, "T1", 200, 0, null, 4, 2, true));

        AiService aiService = new DefaultAiService();
        aiService.doAiTurns(state, new DefaultFleetService());

        assertThat(state.fleets()).hasSize(1);
        assertThat(state.fleets().getFirst().fromSystemId()).isOne();
        assertThat(state.fleets().getFirst().toSystemId()).isEqualTo(3);
    }
}
