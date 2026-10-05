package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.diplomacy.application.DefaultDiplomacyService;
import de.zettsystems.starfare.diplomacy.values.*;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.report.values.TurnEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AllianceVictoryTest {
    @Test void legacyVersionsRetainSimultaneousIndividualVictoryEvaluation() {
        for (var ref : List.of(RulesetRef.SECTOR_FORCES, RulesetRef.SPACEWARD)) {
            var state = new GameState();
            state.rememberSetup(GameSetup.defaults().selectRuleset(ref)
                    .chooseVictoryRules(VictoryRules.defaults(ref, 50)));
            state.players().addAll(List.of(new Player(1, "One", true, "#56b4e9"), new Player(2, "Two", true, "#d55e00")));
            state.systems().addAll(List.of(new StarSystem(1, "S1", 0, 0, 1, 1, 1, false),
                    new StarSystem(2, "S2", 100, 0, 2, 1, 1, false)));
            resolve(state);
            assertThat(state.winnerId()).isEqualTo(2);
            assertThat(state.reports().get(1).events()).anyMatch(TurnEvent.Victory.class::isInstance)
                    .anyMatch(TurnEvent.Defeat.class::isInstance);
            assertThat(state.reports().get(2).events()).anyMatch(TurnEvent.Victory.class::isInstance)
                    .anyMatch(TurnEvent.Defeat.class::isInstance);
        }
    }
    @Test void elevenAndFiveWinTogetherOnlyWhenEnabled() {
        var enabled = state(70, true, 70, 11, 5, 0, 0);
        var disabled = state(70, false, 70, 11, 5, 0, 0);
        resolve(enabled); resolve(disabled);
        assertThat(enabled.outcome().winnerIds()).containsExactly(1, 2);
        assertThat(enabled.outcome().allianceId()).isEqualTo(1);
        assertThat(enabled.winnerId()).isNull();
        assertThat(disabled.gameOver()).isFalse();
        assertThat(enabled.reports().get(2).events()).anySatisfy(event -> {
            assertThat(event).isInstanceOf(TurnEvent.Victory.class);
            assertThat(((TurnEvent.Victory) event).allianceId()).isEqualTo(1);
        });
        assertThat(enabled.reports().get(3).events()).anyMatch(TurnEvent.Defeat.class::isInstance);
    }

    @Test void thresholdsAreIndependentAndIncludeNeutralSystemsWithoutRoundingDown() {
        var below = state(90, true, 70, 6, 5, 0, 5);
        resolve(below); assertThat(below.gameOver()).isFalse();
        var sufficient = state(90, true, 70, 7, 5, 0, 4);
        resolve(sufficient); assertThat(sufficient.outcome().allianceVictory()).isTrue();
        var stricter = state(90, true, 80, 7, 5, 0, 4);
        resolve(stricter); assertThat(stricter.gameOver()).isFalse();
    }

    @Test void individualVictoryTakesPriority() {
        var state = state(70, true, 70, 12, 4, 0, 0); resolve(state);
        assertThat(state.outcome().winnerIds()).containsExactly(1);
        assertThat(state.outcome().allianceVictory()).isFalse();
        assertThat(state.reports().get(2).events()).anyMatch(TurnEvent.Defeat.class::isInstance);
    }

    @Test void membershipWithoutSystemsStillWinsButOnlyEffectiveMembershipCounts() {
        var state = state(100, true, 70, 12, 0, 0, 4);
        resolve(state); assertThat(state.outcome().wonBy(2)).isTrue();
        var departed = state(100, true, 70, 8, 4, 0, 4);
        departed.agreeTreaties(new DiplomacyState(2,
                List.of(new AllianceGroup(1, Set.of(1, 2), 3, Map.of(2, 2))), List.of()));
        new DefaultDiplomacyService().beginRound(departed, 2);
        resolve(departed); assertThat(departed.gameOver()).isFalse();
        var leaving = state(100, true, 70, 8, 4, 0, 4);
        leaving.agreeTreaties(new DiplomacyState(2,
                List.of(new AllianceGroup(1, Set.of(1, 2), 3, Map.of(2, 3))), List.of()));
        new DefaultDiplomacyService().beginRound(leaving, 2);
        resolve(leaving); assertThat(leaving.outcome().winnerIds()).containsExactly(1, 2);
    }

    @Test void strongestGroupWinsAndGroupIdBreaksTies() {
        var stronger = state(100, true, 30, 4, 2, 10, 0);
        stronger.agreeTreaties(new DiplomacyState(3, List.of(
                new AllianceGroup(1, Set.of(1, 2), 3, Map.of()),
                new AllianceGroup(2, Set.of(3, 4), 3, Map.of())), List.of()));
        resolve(stronger); assertThat(stronger.outcome().winnerIds()).containsExactly(3, 4);
        var tied = state(100, true, 30, 4, 4, 8, 0);
        tied.agreeTreaties(stronger.diplomacy()); resolve(tied);
        assertThat(tied.outcome().allianceId()).isEqualTo(1);
    }

    @Test void optionsAreFixedAndDisabledDiplomacyRejectsOrders() {
        var setup = GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD_ALLIANCE)
                .chooseVictoryRules(new VictoryRules(70, false, true, 80));
        var state = new GameState(); state.rememberSetup(setup);
        state.players().addAll(List.of(new Player(1, "One", true, "#56b4e9"), new Player(2, "Two", true, "#d55e00")));
        state.start();
        assertThat(state.victoryRules().allianceVictoryAllowed()).isFalse();
        assertThat(new DefaultDiplomacyService().act(state, 1, new DiplomacyOrder(DiplomacyOrder.Action.FOUND, 2, 3))).isFalse();
        var allowed = new GameState(); allowed.rememberSetup(GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD_ALLIANCE));
        allowed.players().addAll(state.players()); allowed.start();
        assertThat(new DefaultDiplomacyService().act(allowed, 1, new DiplomacyOrder(DiplomacyOrder.Action.FOUND, 2, 3))).isTrue();
        assertThatThrownBy(() -> state.configureVictoryRules(new VictoryRules(80, true, true, 90)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD)
                .chooseVictoryRules(new VictoryRules(70, true, true, 70))).isInstanceOf(IllegalArgumentException.class);
        assertThat(GameSetup.defaults().chooseVictoryRules(new VictoryRules(70, true, true, 70))
                .victoryRules().alliancesAllowed()).isFalse();
    }

    @Test void frozenOutcomeSurvivesSnapshotsCopiesAndLaterTreatyChanges() {
        var state = state(70, true, 70, 11, 5, 0, 0); resolve(state);
        var outcome = state.outcome();
        assertThat(GameState.fromSnapshot(GameState.toSnapshot(state)).outcome()).isEqualTo(outcome);
        assertThat(GameState.copyOf(state).outcome()).isEqualTo(outcome);
        state.agreeTreaties(DiplomacyState.EMPTY); resolve(state);
        assertThat(state.outcome()).isEqualTo(outcome);
        state.resumeForFullConquest(java.time.Instant.now());
        assertThat(state.victoryRules().individualSystemPercent()).isEqualTo(100);
        assertThat(state.victoryRules().allianceSystemPercent()).isEqualTo(100);
        assertThat(state.outcome().hasWinner()).isFalse();
    }

    private static void resolve(GameState state) { new VictoryEvaluation(new DefaultReportService()).resolve(state); }

    static GameState state(int individual, boolean enabled, int group, int one, int two, int three, int neutral) {
        var state = new GameState(); state.resetForNewGame();
        state.rememberSetup(GameSetup.defaults().selectRuleset(RulesetRef.SPACEWARD_ALLIANCE)
                .chooseVictoryRules(new VictoryRules(individual, true, enabled, group)));
        for (int player = 1; player <= 4; player++) {
            state.players().add(new Player(player, "Empire " + player, true, GameConfig.PLAYER_PALETTE.get(player - 1)));
            state.intel().put(player, new HashMap<>());
        }
        int id = 1;
        for (int owner = 1; owner <= 4; owner++) {
            int count = List.of(one, two, three, neutral).get(owner - 1);
            for (int i = 0; i < count; i++) {
                state.systems().add(new StarSystem(id, "S" + id, id * 100, 100, owner == 4 ? null : owner, 1, 1, false)); id++;
            }
        }
        state.start();
        state.agreeTreaties(new DiplomacyState(2, List.of(new AllianceGroup(1, Set.of(1, 2), 3, Map.of())), List.of()));
        return state;
    }
}
