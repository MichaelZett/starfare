package de.zettsystems.starfare.standalone.social.application;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.PlatformPresence;
import de.zettsystems.starfare.game.platform.StarfareMatches;
import de.zettsystems.starfare.standalone.social.values.UserPresence;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Starfare's own presence and visibility behind the platform's interface (plan M13); in a host the
 * platform provides it. Players are account ids as text.
 */
@Component
public class StandalonePlatformPresence implements PlatformPresence {
    private final PresenceTracker presence;
    private final VisibilityFilter visibility;

    public StandalonePlatformPresence(PresenceTracker presence, VisibilityFilter visibility) {
        this.presence = presence;
        this.visibility = visibility;
    }

    @Override
    public boolean canSee(AccountId viewer, AccountId other) {
        return visibility.canSee(StarfareMatches.accountName(viewer), StarfareMatches.accountName(other));
    }

    @Override
    public boolean isOnlineFor(AccountId viewer, AccountId other) {
        return presence.isOnline(StarfareMatches.accountName(other)) && canSee(viewer, other);
    }

    @Override
    public List<AccountId> onlineFor(AccountId viewer) {
        return presence.onlineUsers().stream()
                .map(UserPresence::playerId)
                .map(StarfareMatches::account)
                .flatMap(Optional::stream)
                .filter(account -> !account.equals(viewer))
                .filter(account -> canSee(viewer, account))
                .toList();
    }
}
