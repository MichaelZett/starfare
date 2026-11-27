package de.zettsystems.starfare.game.ui;

import de.zettsystems.gamesplatformapi.PlatformSettings;
import de.zettsystems.starfare.game.platform.StarfareMatches;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The choices of the last new game of an account, kept by the platform ({@link PlatformSettings}) as one value
 * {@code key=value;key=value} under {@value #KEY}. The form checks every recalled value against the current rules
 * and falls back to its default where one no longer fits. Without a platform that keeps settings nothing is kept.
 */
final class NewGameMemory {
    static final String KEY = "create.setup";

    private final @Nullable PlatformSettings settings;
    private final String account;

    private NewGameMemory(@Nullable PlatformSettings settings, String account) {
        this.settings = settings;
        this.account = account;
    }

    static NewGameMemory of(@Nullable PlatformSettings settings, @Nullable String account) {
        return new NewGameMemory(settings, account == null ? "" : account);
    }

    static NewGameMemory none() {
        return new NewGameMemory(null, "");
    }

    /** The kept choices; empty when nothing is kept or the value cannot be read. */
    Map<String, String> recall() {
        if (settings == null || account.isBlank()) {
            return Map.of();
        }
        return settings.value(StarfareMatches.GAME_KEY, KEY).map(NewGameMemory::parse).orElse(Map.of());
    }

    void remember(Map<String, String> choices) {
        if (settings == null || account.isBlank() || choices.isEmpty()) {
            return;
        }
        String value = format(choices);
        if (value.length() <= PlatformSettings.MAX_VALUE_LENGTH) {
            settings.set(StarfareMatches.GAME_KEY, KEY, value);
        }
    }

    static String format(Map<String, String> choices) {
        return choices.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(";"));
    }

    static Map<String, String> parse(String value) {
        Map<String, String> choices = new LinkedHashMap<>();
        for (String pair : value.split(";", -1)) {
            int separator = pair.indexOf('=');
            if (separator > 0) {
                choices.put(pair.substring(0, separator), pair.substring(separator + 1));
            }
        }
        return choices;
    }
}
