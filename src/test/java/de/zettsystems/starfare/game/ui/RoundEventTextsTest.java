package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.report.values.TurnEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoundEventTextsTest {
    private static final String READY = "Schlacht bei Vega — Wiedergabe starten";

    @Test
    void pendingBattlesHideTheirOutcome() {
        assertThat(RoundEventTexts.text(new TurnEvent.BattleWon(1, 7, "Vega", 10, 4, 6, false), true)).isEqualTo(READY);
        assertThat(RoundEventTexts.text(new TurnEvent.BattleLost(1, 7, "Vega", 3, 9, 5), true)).isEqualTo(READY);
        assertThat(RoundEventTexts.text(new TurnEvent.SystemLost(1, 2, 7, "Vega"), true)).isEqualTo(READY);
        assertThat(RoundEventTexts.text(new TurnEvent.DefenseHeld(1, 7, "Vega", 8, 2), true)).isEqualTo(READY);
        assertThat(RoundEventTexts.text(coalition(), true)).isEqualTo(READY);
    }

    @Test
    void acknowledgedBattlesShowTheirOutcome() {
        assertThat(RoundEventTexts.text(new TurnEvent.BattleLost(1, 7, "Vega", 3, 9, 5), false))
                .isEqualTo("Niederlage bei Vega — 3 vs. 9, 5 Verteidiger übrig");
        assertThat(RoundEventTexts.text(new TurnEvent.SystemLost(1, 2, 7, "Vega"), false)).isEqualTo("Vega gefallen");
        assertThat(RoundEventTexts.text(new TurnEvent.DefenseHeld(1, 7, "Vega", 8, 2), false))
                .isEqualTo("Vega gehalten — 8 Angreifer abgewehrt, 2 übrig");
        assertThat(RoundEventTexts.text(coalition(), false)).isEqualTo(CoalitionPresentation.report(coalition()));
    }

    @Test
    void aWonBattleNamesWhetherTheSystemWasNeutral() {
        String neutral = RoundEventTexts.text(new TurnEvent.BattleWon(1, 7, "Vega", 10, 4, 6, true), false);
        String enemy = RoundEventTexts.text(new TurnEvent.BattleWon(1, 7, "Vega", 10, 4, 6, false), false);

        assertThat(neutral).isEqualTo(de.zettsystems.starfare.i18n.I18n.t(UiTexts.ROUND_EVENT_BATTLE_WON_NEUTRAL, "Vega", 10, 4, 6));
        assertThat(enemy).isEqualTo(de.zettsystems.starfare.i18n.I18n.t(UiTexts.ROUND_EVENT_BATTLE_WON_ENEMY, "Vega", 10, 4, 6));
        assertThat(neutral).isNotEqualTo(enemy).contains("Vega");
    }

    @Test
    void economyAndEndOfGameEventsAreWordedForThePlayer() {
        assertThat(RoundEventTexts.text(new TurnEvent.Production(1, 7, "Vega", 3), true)).isEqualTo("Vega: +3 Schiffe");
        assertThat(RoundEventTexts.text(new TurnEvent.Reinforcement(1, 7, "Vega", 4, 12, "Flotte 2"), false))
                .isEqualTo("Vega: +4 → 12 (Flotte 2)");
        assertThat(RoundEventTexts.text(new TurnEvent.Victory(1, 70), false))
                .isEqualTo("Sieg! Mindestens 70 % der Systeme kontrolliert.");
        assertThat(RoundEventTexts.text(new TurnEvent.Victory(1, 60, 4, List.of("Ann", "Bob")), false))
                .isEqualTo("Bündnissieg 4: Ann, Bob – mindestens 60% aller Systeme.");
        assertThat(RoundEventTexts.text(new TurnEvent.Defeat(2, "Ann"), false)).isEqualTo("Niederlage: Ann gewinnt die Partie.");
        assertThat(RoundEventTexts.text(new TurnEvent.EconomySummary(1, 1), false)).isEmpty();
    }

    @Test
    void replaySidesPutThePlayerOnTheRightSide() {
        assertThat(RoundEventTexts.sides(won(false, "Ann", "Bob"))).isEqualTo(sides("Du · Ann", "Bob"));
        assertThat(RoundEventTexts.sides(won(true, "Ann", "Bob"))).isEqualTo(sides("Du · Ann", "Neutrales System"));
        assertThat(RoundEventTexts.sides(won(false, "", ""))).isEqualTo(sides("Du", "Gegner"));
        assertThat(RoundEventTexts.sides(new TurnEvent.BattleLost(1, 7, "Vega", 3, 9, 5, 3, 9, "Ann", "Bob")))
                .isEqualTo(sides("Du · Ann", "Bob"));
        assertThat(RoundEventTexts.sides(new TurnEvent.SystemLost(1, 2, 7, "Vega", 9, 3, 6, 9, 3, "Bob", "Ann")))
                .isEqualTo(sides("Bob", "Du · Ann"));
        assertThat(RoundEventTexts.sides(new TurnEvent.DefenseHeld(1, 7, "Vega", 8, 4, 2, 8, 4, "", "")))
                .isEqualTo(sides("Gegner", "Du"));
        assertThat(RoundEventTexts.sides(new TurnEvent.Production(1, 7, "Vega", 3))).isEqualTo(sides("", ""));
    }

    private static TurnEvent.BattleWon won(boolean neutral, String attacker, String defender) {
        return new TurnEvent.BattleWon(1, 7, "Vega", 10, 4, 6, neutral, 10, 4, attacker, defender);
    }

    private static BattleReplayDialog.BattleSides sides(String left, String right) {
        return new BattleReplayDialog.BattleSides(left, right);
    }

    private static TurnEvent.CoalitionBattle coalition() {
        return new TurnEvent.CoalitionBattle(7, "Vega", null, null, "", List.of(), List.of());
    }
}
