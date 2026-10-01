package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.server.VaadinSession;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.social.application.UserPreferencesService;
import de.zettsystems.starfare.social.values.DisplaySetting;
import java.util.HashMap;
import java.util.Map;

/** Account-bound cache. Every explicit choice is also saved in the database. */
final class DisplayPreferences {
    private final UserPreferencesService service;
    private final String account;
    private final Map<DisplaySetting, String> values;

    private DisplayPreferences(UserPreferencesService service, String account) {
        this.service = service;
        this.account = account;
        values = new HashMap<>(service.displaySettings(account));
    }

    static void load(UserPreferencesService service) {
        VaadinSession.getCurrent().setAttribute(DisplayPreferences.class,
                new DisplayPreferences(service, UserContext.currentPlayerId().orElse("")));
    }

    static String get(DisplaySetting key, String fallback) {
        var preferences = VaadinSession.getCurrent().getAttribute(DisplayPreferences.class);
        return preferences == null ? fallback : preferences.values.getOrDefault(key, fallback);
    }

    static double number(DisplaySetting key, double fallback) {
        try {
            double value = Double.parseDouble(get(key, String.valueOf(fallback)));
            return Double.isFinite(value) ? value : fallback;
        } catch (NumberFormatException _) { return fallback; }
    }

    static void choose(DisplaySetting key, String value) {
        var preferences = VaadinSession.getCurrent().getAttribute(DisplayPreferences.class);
        if (preferences == null || preferences.account.isBlank() || value.equals(preferences.values.get(key))) { return; }
        preferences.service.chooseDisplaySetting(preferences.account, key, value);
        preferences.values.put(key, value);
    }

}
