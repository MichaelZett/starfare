package de.zettsystems.starfare.standalone.platform;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.PlatformAccounts;
import de.zettsystems.gamesplatformapi.PlatformSettings;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Starfare's own start keeps the settings of its account in {@code user_display_settings}, one row per
 * account and key (platform decision E20). The own start runs only Starfare, so the table has no game
 * column; another game's key reads nothing and cannot be written. A host brings its own implementation
 * and takes the rows over once.
 */
@Component
class SoloPlatformSettings implements PlatformSettings {
    private static final String GAME_KEY = "starfare";
    private static final String READ = "SELECT setting_key, setting_value FROM user_display_settings WHERE player_id = ?";
    private static final String WRITE = """
            INSERT INTO user_display_settings (player_id, setting_key, setting_value) VALUES (?, ?, ?)
            ON CONFLICT (player_id, setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value""";
    private static final String RESET = "DELETE FROM user_display_settings WHERE player_id = ? AND setting_key = ?";

    private final JdbcTemplate jdbc;
    private final PlatformAccounts accounts;

    SoloPlatformSettings(JdbcTemplate jdbc, PlatformAccounts accounts) {
        this.jdbc = jdbc;
        this.accounts = accounts;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> value(String gameKey, String key) {
        return Optional.ofNullable(read(gameKey, List.of(key)).get(key));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> values(String gameKey, Collection<String> keys) {
        return read(gameKey, keys);
    }

    private Map<String, String> read(String gameKey, Collection<String> keys) {
        Optional<AccountId> account = accounts.current();
        if (account.isEmpty() || !GAME_KEY.equals(gameKey)) {
            return Map.of();
        }
        Map<String, String> stored = new LinkedHashMap<>();
        jdbc.query(READ, row -> {
            stored.put(row.getString(1), row.getString(2));
        }, playerId(account.get()));
        Map<String, String> values = new LinkedHashMap<>();
        keys.forEach(key -> {
            String value = stored.get(key);
            if (value != null) {
                values.put(key, value);
            }
        });
        return values;
    }

    @Override
    @Transactional
    public void set(String gameKey, String key, String value) {
        String player = signedIn();
        requireGame(gameKey);
        checked(key, MAX_KEY_LENGTH, "key");
        checked(value, MAX_VALUE_LENGTH, "value");
        jdbc.update(WRITE, player, key, value);
    }

    @Override
    @Transactional
    public void reset(String gameKey, String key) {
        String player = signedIn();
        requireGame(gameKey);
        jdbc.update(RESET, player, key);
    }

    private String signedIn() {
        return accounts.current().map(SoloPlatformSettings::playerId)
                .orElseThrow(() -> new IllegalStateException("Nobody is signed in"));
    }

    private static void requireGame(String gameKey) {
        if (!GAME_KEY.equals(gameKey)) {
            throw new IllegalArgumentException("Starfare's own start keeps only Starfare's settings, not " + gameKey);
        }
    }

    private static void checked(String text, int maxLength, String name) {
        if (text.isBlank() || text.length() > maxLength) {
            throw new IllegalArgumentException(name + " must not be blank nor longer than " + maxLength + " characters");
        }
    }

    private static String playerId(AccountId account) {
        return Long.toString(account.value());
    }
}
