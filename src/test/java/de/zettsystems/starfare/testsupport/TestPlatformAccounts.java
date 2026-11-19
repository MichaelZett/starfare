package de.zettsystems.starfare.testsupport;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.PlatformAccounts;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Accounts as a platform host knows them, for tests with several players. Starfare's own start
 * knows only its standard account; tests create accounts per e-mail address and sign one in by
 * setting its sign-in token (plan Z3 b: "Tests setzen Anmeldezeichen aus dem Testbaukasten").
 * Vaadin hands the security context to background UI access, so pushed updates see the account.
 */
@Component
@Primary
public class TestPlatformAccounts implements PlatformAccounts {

    /** The principal of a signed-in test account. */
    public record Account(long id, String email, String displayName) {
        public AccountId accountId() {
            return AccountId.of(id);
        }

        /** The player id the game stores: the account id as text. */
        public String playerId() {
            return String.valueOf(id);
        }
    }

    private final AtomicLong ids = new AtomicLong(1_000);
    private final Map<String, Account> byEmail = new ConcurrentHashMap<>();
    private final Map<Long, Account> byId = new ConcurrentHashMap<>();

    /** Creates the account for this address, or returns the one that already exists. */
    public Account createAccount(String email, String displayName) {
        return byEmail.computeIfAbsent(email, _ -> {
            Account account = new Account(ids.incrementAndGet(), email, displayName);
            byId.put(account.id(), account);
            return account;
        });
    }

    public Optional<Account> findByEmail(String email) {
        return Optional.ofNullable(byEmail.get(email));
    }

    public Account require(String email) {
        return findByEmail(email).orElseThrow(() -> new IllegalArgumentException("No test account " + email));
    }

    /** A security context holding the account's sign-in token. */
    public SecurityContext signedIn(String email) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(require(email), null, List.of()));
        return context;
    }

    @Override
    public Optional<AccountId> current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof Account account
                ? Optional.of(account.accountId()) : Optional.empty();
    }

    @Override
    public Optional<String> displayName(AccountId account) {
        return Optional.ofNullable(byId.get(account.value())).map(Account::displayName);
    }

    /** No test account is a tester; {@code DefaultRulesetAccessTest} covers the tester role. */
    @Override
    public boolean isTester(AccountId account, String gameKey) {
        return false;
    }
}
