package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RulesetRegistryTest {
    @Test void acceptanceGamesAreCreatedWithCompleteSavedEconomicsBeforeTheirFirstRound() {
        var store = new InMemoryGameSessionStore();
        GameRegistry preview = new DefaultGameRegistry(store, new DefaultGameAccessPolicy(), RulesetCatalog.builtIn(true));
        var id = preview.createGame(GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD_ECONOMY_PREVIEW));
        var original = preview.readState(id, GameState::toSnapshot);
        assertThat(original.colonies()).hasSameSizeAs(original.systems());
        assertThat(original.metals()).isNotNull();
        var restored = GameState.fromSnapshot(original);
        restored.start();
        assertThat(de.zettsystems.starfare.economy.domain.RecyclingOrders.balanceTenths(restored))
                .isEqualTo(restored.metals().orElseThrow().recycling().initialTenths());
        assertThat(restored.ruleset()).isEqualTo(RulesetRef.SPACEWARD_ECONOMY_PREVIEW);
        assertThat(RulesetCatalog.builtIn().find("spaceward").orElseThrow().defaultRef()).isEqualTo(RulesetRef.SPACEWARD_ALLIANCE);
    }
    @Test
    void unavailableVariantsAreRejectedBeforeAnyGameIsSaved() {
        GameSessionStore store = mock(GameSessionStore.class);
        RulesetRef unreleased = new RulesetRef("test-unreleased", "1.0.0");
        var entries = new java.util.ArrayList<>(RulesetCatalog.builtIn().definitions());
        entries.add(new RulesetDefinition(unreleased, "test.name", "test.description", Set.of("1.0.0"), false));
        GameRegistry registry = new DefaultGameRegistry(store, new DefaultGameAccessPolicy(), new RulesetCatalog(entries));
        var input1 = GameSetup.defaults().selectRuleset(unreleased);
        assertThatThrownBy(() -> registry.createGame(input1)).isInstanceOf(IllegalArgumentException.class);
        var input2 = GameSetup.defaults().selectRuleset(new RulesetRef("classic", "9"));
        assertThatThrownBy(() -> registry.createGame(input2)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(store);
    }

    @Test
    void releasedSpacewardGamesStartWithIndustryAndFixedRange() {
        GameSessionStore store = new InMemoryGameSessionStore();
        GameRegistry registry = new DefaultGameRegistry(store, new DefaultGameAccessPolicy());
        GameId id = registry.createGame(GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD));
        registry.readState(id, state -> {
            assertThat(state.ruleset()).isEqualTo(RulesetRef.SPACEWARD);
            assertThat(state.industries()).hasSameSizeAs(state.systems());
            assertThat(state.navigationSettings()).isPresent();
            return null;
        });
        GameId classic = registry.createGame(GameSetup.defaults());
        registry.readState(classic, state -> {
            assertThat(state.industries()).isEmpty();
            assertThat(state.navigationSettings()).isEmpty();
            return null;
        });
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
        var input3 = GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD);
        assertThatThrownBy(() -> state.rememberSetup(input3)).isInstanceOf(IllegalStateException.class);
        state.start();
        var input4 = GameSetup.defaults();
        assertThatThrownBy(() -> state.rememberSetup(input4)).isInstanceOf(IllegalStateException.class);
        assertThat(state.ruleset()).isEqualTo(RulesetRef.SECTOR_FORCES);
    }
}
