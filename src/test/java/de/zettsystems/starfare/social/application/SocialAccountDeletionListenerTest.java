package de.zettsystems.starfare.social.application;

import de.zettsystems.identity.values.AccountDeleted;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class SocialAccountDeletionListenerTest {
    @Test void removesBothDirectionsOfSocialLinksAndTheAccountsPreferences() {
        var jdbc = mock(JdbcTemplate.class);
        assertThatCode(() -> new SocialAccountDeletionListener(jdbc).accountDeleted(new AccountDeleted(42L, null)))
                .doesNotThrowAnyException();
        verify(jdbc).update("DELETE FROM friendships WHERE player_a = ? OR player_b = ?", "42", "42");
        verify(jdbc).update("DELETE FROM direct_messages WHERE sender_player_id = ? OR recipient_player_id = ?", "42", "42");
        verify(jdbc).update("DELETE FROM user_preferences WHERE player_id = ?", "42");
        verifyNoMoreInteractions(jdbc);
    }
}
