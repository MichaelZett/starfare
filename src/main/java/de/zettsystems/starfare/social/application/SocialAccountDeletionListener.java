package de.zettsystems.starfare.social.application;

import de.zettsystems.identity.values.AccountDeleted;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Removes only the deleted account's social records in the same transaction. */
@Component
public class SocialAccountDeletionListener {
    private static final String FRIENDSHIPS = "DELETE FROM friendships WHERE player_a = ? OR player_b = ?";
    private static final String MESSAGES = "DELETE FROM direct_messages WHERE sender_player_id = ? OR recipient_player_id = ?";
    private static final String PREFERENCES = "DELETE FROM user_preferences WHERE player_id = ?";
    private final JdbcTemplate jdbc;
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected repositories and collaborators are shared Spring beans") // rationale: dependency injection shares collaborators.
    public SocialAccountDeletionListener(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @EventListener
    @org.springframework.core.annotation.Order(0)
    @Transactional(propagation = Propagation.MANDATORY)
    public void accountDeleted(AccountDeleted event) {
        String account = event.userId().toString();
        jdbc.update(FRIENDSHIPS, account, account);
        jdbc.update(MESSAGES, account, account);
        jdbc.update(PREFERENCES, account);
    }
}
