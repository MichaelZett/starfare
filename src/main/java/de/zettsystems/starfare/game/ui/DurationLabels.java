package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.i18n.I18n;

import java.time.Duration;

/** Kurze Beschriftung einer Frist in der größten glatten Einheit: „30 s", „5 min", „2 Tage". */
final class DurationLabels {

    private DurationLabels() {
    }

    static String label(Duration duration) {
        long seconds = duration.toSeconds();
        if (seconds % 86_400 == 0) {
            return I18n.t(UiTexts.DURATION_DAYS, seconds / 86_400);
        }
        if (seconds % 3_600 == 0) {
            return I18n.t(UiTexts.DURATION_HOURS, seconds / 3_600);
        }
        if (seconds % 60 == 0) {
            return I18n.t(UiTexts.DURATION_MINUTES, seconds / 60);
        }
        return I18n.t(UiTexts.DURATION_SECONDS, seconds);
    }
}
