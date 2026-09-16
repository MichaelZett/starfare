package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.server.VaadinSession;
import de.zettsystems.starfare.game.values.GameId;

/** Per-session choice for one report round; the game setup only supplies its default. */
final class BattlePresentationPreference {
    private static final String KEY_PREFIX = "starfare.battlePresentation.";

    private BattlePresentationPreference() {
    }

    static boolean enabled(GameId gameId, int reportTurn, boolean defaultValue) {
        Object value = VaadinSession.getCurrent().getAttribute(key(gameId, reportTurn));
        return value instanceof Boolean enabled ? enabled : defaultValue;
    }

    static void set(GameId gameId, int reportTurn, boolean enabled) {
        VaadinSession.getCurrent().setAttribute(key(gameId, reportTurn), enabled);
    }

    private static String key(GameId gameId, int reportTurn) {
        return KEY_PREFIX + gameId.value() + "." + reportTurn;
    }
}
