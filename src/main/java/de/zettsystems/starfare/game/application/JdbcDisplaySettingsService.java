package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.DisplaySetting;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

/** Stores display settings in {@code user_display_settings}, one row per player and setting. */
@Service
public class JdbcDisplaySettingsService implements DisplaySettingsService {
    private static final int MAX_VALUE = 512;
    private static final String READ = "SELECT setting_key, setting_value FROM user_display_settings WHERE player_id = ?";
    private static final String WRITE = """
            INSERT INTO user_display_settings (player_id, setting_key, setting_value) VALUES (?, ?, ?)
            ON CONFLICT (player_id, setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value""";
    private final JdbcTemplate jdbc;

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected repositories and collaborators are shared Spring beans") // rationale: dependency injection shares collaborators.
    public JdbcDisplaySettingsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<DisplaySetting, String> displaySettings(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return Map.of();
        }
        Map<DisplaySetting, String> settings = new EnumMap<>(DisplaySetting.class);
        jdbc.query(READ, row -> {
            known(row.getString(1)).ifPresent(key -> {
                try {
                    settings.put(key, row.getString(2));
                } catch (java.sql.SQLException e) {
                    throw new IllegalStateException(e);
                }
            });
        }, playerId.strip());
        return Map.copyOf(settings);
    }

    @Override
    @Transactional
    public void chooseDisplaySetting(String playerId, DisplaySetting key, String value) {
        if (playerId == null || playerId.isBlank() || value.length() > MAX_VALUE) {
            return;
        }
        jdbc.update(WRITE, playerId.strip(), key.name(), value);
    }

    private static java.util.Optional<DisplaySetting> known(String key) {
        try {
            return java.util.Optional.of(DisplaySetting.valueOf(key));
        } catch (IllegalArgumentException _) {
            return java.util.Optional.empty();
        }
    }
}
