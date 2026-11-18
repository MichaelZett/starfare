package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.values.DisplaySetting;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class DisplaySettingsServiceTest extends AbstractIntegrationTest {
    @Autowired
    private DisplaySettingsService service;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private TransactionTemplate transactions;

    @Test
    void displayChoicesPersistPerAccount() {
        service.chooseDisplaySetting("display-alice", DisplaySetting.SPEED, "0.0");
        service.chooseDisplaySetting(" display-alice ", DisplaySetting.SOUND, "false");
        service.chooseDisplaySetting("display-alice", DisplaySetting.SPEED, "1.5");
        service.chooseDisplaySetting("display-bob", DisplaySetting.SPEED, "2.0");

        assertThat(service.displaySettings("display-alice"))
                .containsEntry(DisplaySetting.SPEED, "1.5").containsEntry(DisplaySetting.SOUND, "false");
        assertThat(service.displaySettings("display-bob")).containsOnlyKeys(DisplaySetting.SPEED);
    }

    @Test
    void ignoresBlankAccountsTooLongValuesAndUnknownKeys() {
        service.chooseDisplaySetting(" ", DisplaySetting.SPEED, "1.0");
        service.chooseDisplaySetting("display-carol", DisplaySetting.SIDEBAR, "x".repeat(513));
        // The pool does not auto-commit, so the row needs its own transaction.
        transactions.executeWithoutResult(_ -> jdbc.update("INSERT INTO user_display_settings"
                + " (player_id, setting_key, setting_value) VALUES ('display-carol', 'RETIRED', 'x')"));

        assertThat(service.displaySettings(" ")).isEmpty();
        assertThat(service.displaySettings("display-carol")).isEmpty();
    }
}
