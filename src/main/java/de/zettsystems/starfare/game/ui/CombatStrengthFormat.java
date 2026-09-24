package de.zettsystems.starfare.game.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/** Keep the deciding difference visible without showing unnecessary decimal places. */
final class CombatStrengthFormat {
    private CombatStrengthFormat() {
    }

    static String format(double strength, double opposingStrength, Locale locale) {
        BigDecimal own = BigDecimal.valueOf(strength);
        BigDecimal other = BigDecimal.valueOf(opposingStrength);
        int decimals = 2;
        int maxDecimals = Math.max(own.scale(), other.scale());
        while (own.compareTo(other) != 0 && decimals < maxDecimals
                && own.setScale(decimals, RoundingMode.HALF_UP)
                .compareTo(other.setScale(decimals, RoundingMode.HALF_UP)) == 0) {
            decimals++;
        }
        NumberFormat format = NumberFormat.getNumberInstance(locale);
        format.setMaximumFractionDigits(decimals);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(own);
    }
}
