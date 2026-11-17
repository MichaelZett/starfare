package de.zettsystems.starfare.game.application;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.AccountRemoved;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class LegacySocialRecordsTest {
    @Test
    void removesBothDirectionsOfSocialLinksTheDisplaySettingsAndTheAccountsPreferences() {
        var jdbc = mock(JdbcTemplate.class);
        assertThatCode(() -> new LegacySocialRecords(jdbc).accountDeleted(new AccountRemoved(AccountId.of(42))))
                .doesNotThrowAnyException();
        verify(jdbc).update("DELETE FROM friendships WHERE player_a = ? OR player_b = ?", "42", "42");
        verify(jdbc).update("DELETE FROM direct_messages WHERE sender_player_id = ? OR recipient_player_id = ?", "42", "42");
        verify(jdbc).update("DELETE FROM user_display_settings WHERE player_id = ?", "42");
        verify(jdbc).update("DELETE FROM user_preferences WHERE player_id = ?", "42");
        verifyNoMoreInteractions(jdbc);
    }

    @Test
    void removesMessagesOlderThanAYear() {
        var jdbc = mock(JdbcTemplate.class);
        new LegacySocialRecords(jdbc).removeExpiredMessages();
        verify(jdbc).update("DELETE FROM direct_messages WHERE sent_at < now() - interval '365 days'");
    }
}
