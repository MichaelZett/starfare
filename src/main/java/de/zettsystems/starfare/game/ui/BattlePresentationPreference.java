package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.server.VaadinSession;
import de.zettsystems.starfare.game.values.GameId;

/** Per-session choice for one report round; the game setup only supplies its default. */
final class BattlePresentationPreference {
    private static final String KEY_PREFIX = "starfare.battlePresentation.";

    private BattlePresentationPreference() {
    }

    static boolean enabled(GameId gameId, int reportTurn, boolean defaultValue) {
        return speed(gameId, reportTurn, defaultValue) > 0;
    }

    static double speed(GameId gameId, int reportTurn, boolean defaultValue) {
        Object value = VaadinSession.getCurrent().getAttribute(key(gameId, reportTurn));
        if (value instanceof Double speed) {
            return speed;
        }
        if (value instanceof Boolean enabled) {
            return enabled ? 1.0 : 0.0;
        }
        return defaultValue ? 1.0 : 0.0;
    }

    static void set(GameId gameId, int reportTurn, boolean enabled) {
        VaadinSession.getCurrent().setAttribute(key(gameId, reportTurn), enabled);
    }

    static void setSpeed(GameId gameId, int reportTurn, double speed) {
        VaadinSession.getCurrent().setAttribute(key(gameId, reportTurn), speed);
        BattleReplayDialog.setSpeed(speed);
    }

    private static String key(GameId gameId, int reportTurn) {
        return KEY_PREFIX + gameId.value() + "." + reportTurn;
    }
}
