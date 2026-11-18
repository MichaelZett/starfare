package de.zettsystems.starfare.standalone.platform;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.PlatformFriends;
import org.springframework.stereotype.Component;

import java.util.List;

/** Starfare's own start has no friends lists; friends belong to the platform (plan M13). */
@Component
class SoloPlatformFriends implements PlatformFriends {

    @Override
    public List<AccountId> friendsOf(AccountId account) {
        return List.of();
    }

    @Override
    public boolean areFriends(AccountId one, AccountId other) {
        return false;
    }

    @Override
    public boolean isBlockedBetween(AccountId one, AccountId other) {
        return false;
    }
}
