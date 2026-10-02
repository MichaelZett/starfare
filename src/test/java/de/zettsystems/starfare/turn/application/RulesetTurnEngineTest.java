package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RulesetTurnEngineTest {
    @Test
    void completeReferenceDispatchesToAnIndependentThirdVariant() {
        RulesetRef third = new RulesetRef("test-third", "1.2.0");
        var entries = new java.util.ArrayList<>(RulesetCatalog.builtIn().definitions());
        entries.add(new RulesetDefinition(third, "test.name", "test.description", Set.of("1.2.0"), true));
        TurnEngine classic = mock(TurnEngine.class);
        TurnEngine other = mock(TurnEngine.class);
        var engine = new RulesetTurnEngine(new RulesetCatalog(entries),
                Map.of(RulesetRef.SECTOR_FORCES, classic, RulesetRef.SPACEWARD, mock(TurnEngine.class), third, other));
        GameState state = new GameState();
        state.rememberSetup(GameSetup.defaults().selectRuleset(third));
        assertThatCode(() -> engine.advanceTurn(state)).doesNotThrowAnyException();
        verify(other).advanceTurn(state);
        verifyNoInteractions(classic);
    }

    @Test
    void unsupportedRulesCannotInvokeAnyEngineOrMutateState() {
        TurnEngine classic = mock(TurnEngine.class);
        var engine = new RulesetTurnEngine(RulesetCatalog.builtIn(), Map.of(RulesetRef.SECTOR_FORCES, classic, RulesetRef.SPACEWARD, mock(TurnEngine.class)));
        GameState state = new GameState();
        state.rememberSetup(GameSetup.defaults().selectRuleset(new RulesetRef("classic", "9.0.0")));
        var before = GameState.toSnapshot(state);
        assertThatThrownBy(() -> engine.advanceTurn(state)).isInstanceOf(IllegalArgumentException.class);
        assertThat(GameState.toSnapshot(state)).isEqualTo(before);
        verifyNoInteractions(classic);
    }

    @Test
    void providerRegistrationRoutesClassicAndRejectsMissingImplementations() {
        RulesetRoundImplementation provider = mock(RulesetRoundImplementation.class);
        when(provider.ruleset()).thenReturn(RulesetRef.SECTOR_FORCES);
        RulesetRoundImplementation spaceward = mock(RulesetRoundImplementation.class);
        when(spaceward.ruleset()).thenReturn(RulesetRef.SPACEWARD);
        var engine = new RulesetTurnEngine(RulesetCatalog.builtIn(), List.of(provider, spaceward));
        GameState state = new GameState();
        assertThatCode(() -> engine.advanceTurn(state)).doesNotThrowAnyException();
        verify(provider).advanceTurn(state);
        assertThatThrownBy(() -> new RulesetTurnEngine(RulesetCatalog.builtIn(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
