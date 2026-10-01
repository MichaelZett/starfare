package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GameTemplateServiceTest {
    private final GameRegistry registry = new DefaultGameRegistry(new InMemoryGameSessionStore(), new DefaultGameAccessPolicy());
    private final GameTemplateService templates = new DefaultGameTemplateService(registry, mock(GameArchiveStore.class),
            new DefaultGameAccessPolicy(), mock(Broadcaster.class));

    @Test
    void rematchRetainsRulesAndReservesSeatsWithoutJoiningPeople() {
        GameSetup defaults = GameSetup.defaults();
        GameSetup setup = new GameSetup(12, 2, 0, List.of(4, 7), 2, 5, 15, true, true,
                defaults.seatColorHexes(), defaults.productionDistribution(), defaults.galaxyLayout(), false,
                20, defaults.roundRules(), 90).normalized();
        GameId source = registry.createGame(setup, "host", "Original");
        registry.writeState(source, state -> {
            state.seatByUser().put("host", 1);
            state.seatByUser().put("guest", 2);
            state.endGame(1);
            return null;
        });
        GameId rematch = templates.create(source, "host", "Again", true).orElseThrow();
        assertThat(rematch).isNotEqualTo(source);
        GameState state = registry.readState(rematch, GameState::copyOf);
        assertThat(state.originalSetup()).hasValue(setup);
        assertThat(state.systems()).isNotEqualTo(registry.readState(source, s -> List.copyOf(s.systems())));
        assertThat(state.invitedSeats()).containsEntry("host", 1).containsEntry("guest", 2);
        assertThat(state.joinedHumanPlayerIds()).isEmpty();
        assertThat(state.started()).isFalse();
        assertThat(state.visibility()).isEqualTo(GameVisibility.PRIVATE);
        assertThat(registry.claimSeat(rematch, "guest")).hasValue(2);
    }

    @Test
    void templateWithoutParticipantsIsPrivateAndOutsidersCannotCopy() {
        GameId source = registry.createGame(GameSetup.defaults(), "host", "Original");
        assertThat(templates.create(source, "outsider", "Stolen", true)).isEmpty();
        assertThat(templates.create(source, "host", " ", false)).isEmpty();
        GameId copy = templates.create(source, "host", "Copy", false).orElseThrow();
        assertThat(registry.readState(copy, state -> state.invitedSeats().isEmpty()).booleanValue()).isTrue();
        registry.writeState(source, state -> { state.resetForNewGame(); return null; });
        assertThat(templates.template(source, "host")).isEmpty();
    }
}
