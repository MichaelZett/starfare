package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RulesetRegistryTest {
    @Test
    void unavailableVariantsAreRejectedBeforeAnyGameIsSaved() {
        GameSessionStore store = mock(GameSessionStore.class);
        GameRegistry registry = new DefaultGameRegistry(store, new DefaultGameAccessPolicy());
        assertThatThrownBy(() -> registry.createGame(GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.createGame(GameSetup.defaults().selectRuleset(new RulesetRef("classic", "9"))))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(store);
    }

    @Test
    void registryAndTemplatesPreserveAnIndependentlyRegisteredReference() {
        RulesetRef third = new RulesetRef("test-third", "1.2.0");
        var entries = new java.util.ArrayList<>(RulesetCatalog.builtIn().definitions());
        entries.add(new RulesetDefinition(third, "test.name", "test.description", Set.of("1.2.0"), true));
        var catalog = new RulesetCatalog(entries);
        GameRegistry registry = new DefaultGameRegistry(new InMemoryGameSessionStore(), new DefaultGameAccessPolicy(), catalog);
        var templates = new DefaultGameTemplateService(registry, mock(GameArchiveStore.class),
                new DefaultGameAccessPolicy(), mock(Broadcaster.class));
        GameId original = registry.createGame(GameSetup.defaults().selectRuleset(third), "host", "Original");
        registry.writeState(original, state -> { state.seatByUser().put("host", 1); return null; });
        GameId rematch = templates.create(original, "host", "Rematch", true).orElseThrow();
        assertThat(registry.rulesets()).isEqualTo(catalog);
        assertThat(registry.readState(rematch, GameState::ruleset)).isEqualTo(third);
        RulesetRef copiedRules = registry.readState(rematch, state -> state.originalSetup().orElseThrow().ruleset());
        Integer reservedSeat = registry.readState(rematch, state -> state.invitedSeats().get("host"));
        assertThat(copiedRules).isEqualTo(third);
        assertThat(reservedSeat).isEqualTo(1);
    }

    @Test
    void gameRulesCannotBeChangedAfterInitialization() {
        var state = new GameState();
        state.rememberSetup(GameSetup.defaults());
        assertThatThrownBy(() -> state.rememberSetup(GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD)))
                .isInstanceOf(IllegalStateException.class);
        state.start();
        assertThatThrownBy(() -> state.rememberSetup(GameSetup.defaults())).isInstanceOf(IllegalStateException.class);
        assertThat(state.ruleset()).isEqualTo(RulesetRef.SECTOR_FORCES);
    }
}
