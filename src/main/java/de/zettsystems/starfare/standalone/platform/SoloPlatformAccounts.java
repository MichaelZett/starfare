package de.zettsystems.starfare.standalone.platform;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.PlatformAccounts;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Starfare's own start knows exactly one account and needs no sign-in: whoever starts it plays as
 * the standard account, games against the computer (plan Z3 b). Accounts, sign-in and tester roles
 * belong to the platform. The standard account may use every ruleset, test variants included: the
 * own start is the developer's.
 */
@Component
class SoloPlatformAccounts implements PlatformAccounts {
    static final AccountId STANDARD = AccountId.of(1);
    static final String STANDARD_NAME = "Commander";

    @Override
    public Optional<AccountId> current() {
        return Optional.of(STANDARD);
    }

    @Override
    public Optional<String> displayName(AccountId account) {
        return STANDARD.equals(account) ? Optional.of(STANDARD_NAME) : Optional.empty();
    }

    @Override
    public boolean isTester(AccountId account, String gameKey) {
        return STANDARD.equals(account);
    }
}
