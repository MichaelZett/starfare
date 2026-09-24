package de.zettsystems.starfare.game.ui;

import org.junit.jupiter.api.Test;
import java.util.Locale;
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
}
