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
    private static RulesetRoundImplementation allianceProvider() {
        var provider = mock(RulesetRoundImplementation.class);
        when(provider.ruleset()).thenReturn(RulesetRef.SPACEWARD_ALLIANCE);
        return provider;
    }
    private static RulesetRoundImplementation colonyProvider() {
        var provider = mock(RulesetRoundImplementation.class);
        when(provider.ruleset()).thenReturn(RulesetRef.SPACEWARD_COLONIES);
        return provider;
    }
    private static RulesetRoundImplementation metalProvider() {
        var provider = mock(RulesetRoundImplementation.class);
        when(provider.ruleset()).thenReturn(RulesetRef.SPACEWARD_METALS);
        return provider;
    }
    private static RulesetRoundImplementation recyclingProvider() {
        var provider = mock(RulesetRoundImplementation.class);
        when(provider.ruleset()).thenReturn(RulesetRef.SPACEWARD_RECYCLING);
        return provider;
    }
    private static RulesetRoundImplementation directEconomyProvider() {
        var provider = mock(RulesetRoundImplementation.class);
        when(provider.ruleset()).thenReturn(RulesetRef.SPACEWARD_DIRECT_ECONOMY);
        return provider;
    }
    private static RulesetRoundImplementation researchProvider() {
        var provider = mock(RulesetRoundImplementation.class);
        when(provider.ruleset()).thenReturn(RulesetRef.SPACEWARD_SHIP_RESEARCH);
        return provider;
    }
    private static RulesetRoundImplementation earlyIndustryProvider() {
        var provider = mock(RulesetRoundImplementation.class);
        when(provider.ruleset()).thenReturn(RulesetRef.SPACEWARD_EARLY_INDUSTRY);
        return provider;
    }
    private static RulesetRoundImplementation equalMetalProvider() {
        var provider = mock(RulesetRoundImplementation.class);
        when(provider.ruleset()).thenReturn(RulesetRef.SPACEWARD_EQUAL_METAL);
        return provider;
    }
    private static RulesetRoundImplementation economyPreviewProvider() {
        var provider = mock(RulesetRoundImplementation.class);
        when(provider.ruleset()).thenReturn(RulesetRef.SPACEWARD_ECONOMY_PREVIEW);
        return provider;
    }
    @Test
    void completeReferenceDispatchesToAnIndependentThirdVariant() {
        RulesetRef third = new RulesetRef("test-third", "1.2.0");
        var entries = new java.util.ArrayList<>(RulesetCatalog.builtIn().definitions());
        entries.add(new RulesetDefinition(third, "test.name", "test.description", Set.of("1.2.0"), true));
        TurnEngine classic = mock(TurnEngine.class);
        TurnEngine other = mock(TurnEngine.class);
        var engine = new RulesetTurnEngine(new RulesetCatalog(entries),
                Map.ofEntries(Map.entry(RulesetRef.SECTOR_FORCES, classic), Map.entry(RulesetRef.SPACEWARD, mock(TurnEngine.class)), Map.entry(RulesetRef.SPACEWARD_ALLIANCE, mock(TurnEngine.class)), Map.entry(RulesetRef.SPACEWARD_COLONIES, mock(TurnEngine.class)), Map.entry(RulesetRef.SPACEWARD_METALS, mock(TurnEngine.class)), Map.entry(RulesetRef.SPACEWARD_RECYCLING, mock(TurnEngine.class)), Map.entry(RulesetRef.SPACEWARD_ECONOMY_PREVIEW, mock(TurnEngine.class)), Map.entry(RulesetRef.SPACEWARD_DIRECT_ECONOMY, mock(TurnEngine.class)), Map.entry(RulesetRef.SPACEWARD_EQUAL_METAL, mock(TurnEngine.class)), Map.entry(third, other), Map.entry(RulesetRef.SPACEWARD_EARLY_INDUSTRY, mock(TurnEngine.class)), Map.entry(RulesetRef.SPACEWARD_SHIP_RESEARCH, mock(TurnEngine.class))));
        GameState state = new GameState();
        state.rememberSetup(GameSetup.defaults().selectRuleset(third));
        assertThatCode(() -> engine.advanceTurn(state)).doesNotThrowAnyException();
        verify(other).advanceTurn(state);
        verifyNoInteractions(classic);
    }

    @Test
    void unsupportedRulesCannotInvokeAnyEngineOrMutateState() {
        TurnEngine classic = mock(TurnEngine.class);
        var engine = new RulesetTurnEngine(RulesetCatalog.builtIn(), Map.ofEntries(Map.entry(RulesetRef.SECTOR_FORCES, classic), Map.entry(RulesetRef.SPACEWARD, mock(TurnEngine.class)), Map.entry(RulesetRef.SPACEWARD_ALLIANCE, mock(TurnEngine.class)), Map.entry(RulesetRef.SPACEWARD_COLONIES, mock(TurnEngine.class)), Map.entry(RulesetRef.SPACEWARD_METALS, mock(TurnEngine.class)), Map.entry(RulesetRef.SPACEWARD_RECYCLING, mock(TurnEngine.class)), Map.entry(RulesetRef.SPACEWARD_ECONOMY_PREVIEW, mock(TurnEngine.class)), Map.entry(RulesetRef.SPACEWARD_DIRECT_ECONOMY, mock(TurnEngine.class)), Map.entry(RulesetRef.SPACEWARD_EQUAL_METAL, mock(TurnEngine.class)), Map.entry(RulesetRef.SPACEWARD_EARLY_INDUSTRY, mock(TurnEngine.class)), Map.entry(RulesetRef.SPACEWARD_SHIP_RESEARCH, mock(TurnEngine.class))));
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
        var engine = new RulesetTurnEngine(RulesetCatalog.builtIn(), List.of(provider, spaceward, allianceProvider(), colonyProvider(), metalProvider(), recyclingProvider(), economyPreviewProvider(), directEconomyProvider(), equalMetalProvider(), earlyIndustryProvider(), researchProvider()));
        GameState state = new GameState();
        assertThatCode(() -> engine.advanceTurn(state)).doesNotThrowAnyException();
        verify(provider).advanceTurn(state);
        var input1 = RulesetCatalog.builtIn();
        Map<RulesetRef, TurnEngine> input2 = Map.of();
        assertThatThrownBy(() -> new RulesetTurnEngine(input1, input2)).isInstanceOf(IllegalArgumentException.class);
    }
}
