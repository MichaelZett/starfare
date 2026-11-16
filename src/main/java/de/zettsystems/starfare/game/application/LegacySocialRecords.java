package de.zettsystems.starfare.game.application;

import de.zettsystems.identity.values.AccountDeleted;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the privacy promises for Starfare's social tables wherever Starfare runs, also in a host
 * that keeps friends and messages itself (plan M13) and only reads these tables once: an account's
 * records go with the account, messages after a year. Display settings belong to the game.
 */
@Component
public class LegacySocialRecords {
    private static final String FRIENDSHIPS = "DELETE FROM friendships WHERE player_a = ? OR player_b = ?";
    private static final String MESSAGES = "DELETE FROM direct_messages WHERE sender_player_id = ? OR recipient_player_id = ?";
    private static final String DISPLAY_SETTINGS = "DELETE FROM user_display_settings WHERE player_id = ?";
    private static final String PREFERENCES = "DELETE FROM user_preferences WHERE player_id = ?";
    private static final String EXPIRED = "DELETE FROM direct_messages WHERE sent_at < now() - interval '365 days'";
    private final JdbcTemplate jdbc;

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected repositories and collaborators are shared Spring beans") // rationale: dependency injection shares collaborators.
    public LegacySocialRecords(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** In the transaction that deletes the account, so that a failure keeps everything. */
    @EventListener
    @Order(0)
    @Transactional(propagation = Propagation.MANDATORY)
    public void accountDeleted(AccountDeleted event) {
        String account = event.userId().toString();
        jdbc.update(FRIENDSHIPS, account, account);
        jdbc.update(MESSAGES, account, account);
        jdbc.update(DISPLAY_SETTINGS, account);
        jdbc.update(PREFERENCES, account);
    }

    /** Messages are kept for a year (privacy policy). */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void removeExpiredMessages() {
        jdbc.update(EXPIRED);
    }
}
