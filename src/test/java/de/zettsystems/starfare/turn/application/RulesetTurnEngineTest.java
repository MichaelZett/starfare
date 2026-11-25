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
                Map.ofEntries(Map.entry(RulesetRef.SECTOR_FORCES, classic), Map.entry(RulesetRef.EXTENDED, mock(TurnEngine.class)), Map.entry(third, other)));
        GameState state = new GameState();
        state.rememberSetup(GameSetup.defaults().selectRuleset(third));
        assertThatCode(() -> engine.advanceTurn(state)).doesNotThrowAnyException();
        verify(other).advanceTurn(state);
        verifyNoInteractions(classic);
    }

    @Test
    void unsupportedRulesCannotInvokeAnyEngineOrMutateState() {
        TurnEngine classic = mock(TurnEngine.class);
        var engine = new RulesetTurnEngine(RulesetCatalog.builtIn(), Map.ofEntries(Map.entry(RulesetRef.SECTOR_FORCES, classic), Map.entry(RulesetRef.EXTENDED, mock(TurnEngine.class))));
        GameState state = new GameState();
        state.rememberSetup(GameSetup.defaults().selectRuleset(new RulesetRef("classic", "9.0.0")));
        var before = GameState.toSnapshot(state);
        assertThatThrownBy(() -> engine.advanceTurn(state)).isInstanceOf(IllegalArgumentException.class);
        assertThat(GameState.toSnapshot(state)).isEqualTo(before);
        verifyNoInteractions(classic);
    }

    @Test
    void anEngineRegisteredForARulesetOutsideTheCatalogIsNeverInvoked() {
        RulesetRef retired = new RulesetRef("classic", "9.0.0");
        TurnEngine stray = mock(TurnEngine.class);
        var engine = new RulesetTurnEngine(RulesetCatalog.builtIn(), Map.ofEntries(Map.entry(retired, stray), Map.entry(RulesetRef.SECTOR_FORCES, mock(TurnEngine.class)), Map.entry(RulesetRef.EXTENDED, mock(TurnEngine.class))));
        GameState state = new GameState();
        state.rememberSetup(GameSetup.defaults().selectRuleset(retired));
        assertThatThrownBy(() -> engine.advanceTurn(state)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported ruleset");
        verifyNoInteractions(stray);
    }

    @Test
    void providerRegistrationRoutesClassicAndRejectsMissingImplementations() {
        RulesetRoundImplementation provider = mock(RulesetRoundImplementation.class);
        when(provider.ruleset()).thenReturn(RulesetRef.SECTOR_FORCES);
        RulesetRoundImplementation spaceward = mock(RulesetRoundImplementation.class);
        when(spaceward.ruleset()).thenReturn(RulesetRef.EXTENDED);
        var engine = new RulesetTurnEngine(RulesetCatalog.builtIn(), List.of(provider, spaceward));
        GameState state = new GameState();
        assertThatCode(() -> engine.advanceTurn(state)).doesNotThrowAnyException();
        verify(provider).advanceTurn(state);
        var input1 = RulesetCatalog.builtIn();
        Map<RulesetRef, TurnEngine> input2 = Map.of();
        assertThatThrownBy(() -> new RulesetTurnEngine(input1, input2)).isInstanceOf(IllegalArgumentException.class);
    }
}
