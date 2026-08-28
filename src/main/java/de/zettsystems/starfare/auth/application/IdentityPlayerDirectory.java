package de.zettsystems.starfare.auth.application;

import de.zettsystems.identity.application.UserAccountService;
import org.springframework.stereotype.Service;

@Service
class IdentityPlayerDirectory implements PlayerDirectory {

    private final UserAccountService accounts;

    IdentityPlayerDirectory(UserAccountService accounts) {
        this.accounts = accounts;
    }

    @Override
    public String displayName(String playerId) {
        try {
            return accounts.findById(Long.valueOf(playerId))
                    .map(account -> account.displayName())
                    .orElse(playerId);
        } catch (NumberFormatException _) {
            return playerId;
        }
    }
}
