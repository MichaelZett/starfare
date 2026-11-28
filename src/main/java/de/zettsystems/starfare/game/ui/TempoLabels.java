package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.game.values.GameTempo;
import de.zettsystems.starfare.i18n.I18n;

/** Names a tempo together with the deadlines it sets, so the choice shows what one agrees to. */
final class TempoLabels {
    private TempoLabels() {
    }

    static String name(GameTempo tempo) {
        return I18n.t(switch (tempo) {
            case LIVE -> UiTexts.LOBBY_TEMPO_LIVE;
            case BRISK -> UiTexts.LOBBY_TEMPO_BRISK;
            case DAILY -> UiTexts.LOBBY_TEMPO_DAILY;
            case EVERY_OTHER_DAY -> UiTexts.LOBBY_TEMPO_EVERY_OTHER_DAY;
            case RELAXED -> UiTexts.LOBBY_TEMPO_RELAXED;
        });
    }

    static String label(GameTempo tempo) {
        return I18n.t(UiTexts.LOBBY_TEMPO_OPTION, name(tempo), DurationLabels.label(tempo.roundLimit()),
                DurationLabels.label(tempo.stragglerLimit()));
    }
}
