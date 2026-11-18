package de.zettsystems.starfare.standalone.platform;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.PlatformPresence;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Starfare's own start tracks nobody online and hides nobody; presence and visibility belong to
 * the platform (plan M13).
 */
@Component
class SoloPlatformPresence implements PlatformPresence {

    @Override
    public boolean canSee(AccountId viewer, AccountId other) {
        return true;
    }

    @Override
    public boolean isOnlineFor(AccountId viewer, AccountId other) {
        return false;
    }

    @Override
    public List<AccountId> onlineFor(AccountId viewer) {
        return List.of();
    }
}
