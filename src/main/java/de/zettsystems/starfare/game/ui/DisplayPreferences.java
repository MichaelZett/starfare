package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.server.VaadinSession;
import de.zettsystems.gamesplatformapi.PlatformSettings;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.platform.StarfareMatches;
import de.zettsystems.starfare.game.values.DisplaySetting;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Account-bound cache of the display choices, read once while the map is built. Every explicit choice
 * is also kept by the platform ({@link PlatformSettings}), so that it follows the account from device to
 * device. Without a platform that keeps settings the choices last for the session only.
 */
final class DisplayPreferences {
    private final @Nullable PlatformSettings settings;
    private final String account;
    private final Map<DisplaySetting, String> values;

    private DisplayPreferences(@Nullable PlatformSettings settings, String account) {
        this.settings = settings;
        this.account = account;
        values = new EnumMap<>(DisplaySetting.class);
        if (settings != null && !account.isBlank()) {
            var keys = Arrays.stream(DisplaySetting.values()).map(DisplaySetting::name).toList();
            // Only the keys asked for count: a value kept for a setting that no longer exists is ignored.
            settings.values(StarfareMatches.GAME_KEY, keys).forEach((key, value) -> {
                if (keys.contains(key)) { values.put(DisplaySetting.valueOf(key), value); }
            });
        }
    }

    static void load(@Nullable PlatformSettings settings) {
        VaadinSession.getCurrent().setAttribute(DisplayPreferences.class,
                new DisplayPreferences(settings, UserContext.currentPlayerId().orElse("")));
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
        // The platform keeps no blank values: a blank choice is the default again.
        if (preferences.settings != null && value.isBlank()) {
            preferences.settings.reset(StarfareMatches.GAME_KEY, key.name());
        } else if (preferences.settings != null && value.length() <= PlatformSettings.MAX_VALUE_LENGTH) {
            preferences.settings.set(StarfareMatches.GAME_KEY, key.name(), value);
        }
        preferences.values.put(key, value);
    }

}
