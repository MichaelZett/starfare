package de.zettsystems.starfare.game.ui;

import org.junit.jupiter.api.Test;
import java.util.Locale;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class CombatStrengthFormatTest {
    @Test
    void showsTheDecidingDifferenceInTheSelectedLocale() {
        assertThat(CombatStrengthFormat.format(3.24, 2.76, Locale.GERMAN)).isEqualTo("3,24");
        assertThat(CombatStrengthFormat.format(2.76, 3.24, Locale.ENGLISH)).isEqualTo("2.76");
        assertThat(CombatStrengthFormat.format(3.000001, 3.000002, Locale.ENGLISH)).isEqualTo("3.000001");
        assertThat(CombatStrengthFormat.format(3.000002, 3.000001, Locale.ENGLISH)).isEqualTo("3.000002");
        assertThat(CombatStrengthFormat.format(3, 3, Locale.GERMAN)).isEqualTo("3");
    }
    @Test void coalitionStrengthsRemainDistinguishableFromTheirNearestOpponent() {
        var strengths = List.of(100.0, 3.000001, 3.000002, 0.0);
        assertThat(CombatStrengthFormat.forCoalition(3.000001, strengths, Locale.GERMAN)).isEqualTo("3,000001");
        assertThat(CombatStrengthFormat.forCoalition(3.000002, strengths, Locale.ENGLISH)).isEqualTo("3.000002");
        assertThat(CombatStrengthFormat.forCoalition(100, strengths, Locale.ENGLISH)).isEqualTo("100");
    }
}
