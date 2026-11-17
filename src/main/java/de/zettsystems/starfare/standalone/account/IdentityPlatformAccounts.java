package de.zettsystems.starfare.standalone.account;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.PlatformAccounts;
import de.zettsystems.identity.application.IdentityUserDetails;
import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.identity.values.Scope;
import de.zettsystems.identity.values.UserAccountDto;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * The platform's accounts in Starfare's own start, from zs-identity like in a platform host and
 * with the host's tester rule: an enabled account with the global role {@code SYSTEM_ADMIN} or
 * with {@code GAME_TESTER} scoped to exactly this game. A host brings its own implementation;
 * this one stays out of the game artifact.
 */
@Service
class IdentityPlatformAccounts implements PlatformAccounts {
    static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";
    static final String GAME_TESTER = "GAME_TESTER";

    private final UserAccountService accounts;
    private final Supplier<@Nullable Authentication> authentication;

    @Autowired
    IdentityPlatformAccounts(UserAccountService accounts) {
        this(accounts, () -> SecurityContextHolder.getContext().getAuthentication());
    }

    IdentityPlatformAccounts(UserAccountService accounts, Supplier<@Nullable Authentication> authentication) {
        this.accounts = accounts;
        this.authentication = authentication;
    }

    @Override
    public Optional<AccountId> current() {
        Authentication current = authentication.get();
        if (current != null && current.isAuthenticated() && current.getPrincipal() instanceof IdentityUserDetails user) {
            return Optional.of(AccountId.of(user.userId()));
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> displayName(AccountId account) {
        return accounts.findById(account.value()).map(UserAccountDto::displayName);
    }

    @Override
    public boolean isTester(AccountId account, String gameKey) {
        Scope scope = Scope.of("game", gameKey);
        return accounts.findById(account.value()).filter(UserAccountDto::enabled)
                .filter(user -> user.hasRole(SYSTEM_ADMIN) || user.roleAssignments().stream()
                        .anyMatch(role -> role.roleCode().equals(GAME_TESTER) && scope.equals(role.scope())))
                .isPresent();
    }
}
