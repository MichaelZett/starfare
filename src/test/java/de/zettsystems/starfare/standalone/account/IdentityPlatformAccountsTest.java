package de.zettsystems.starfare.standalone.account;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.identity.application.IdentityUserDetails;
import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.identity.values.AccountName;
import de.zettsystems.identity.values.Scope;
import de.zettsystems.identity.values.ScopedRole;
import de.zettsystems.identity.values.UserAccountDto;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentityPlatformAccountsTest {
    private final UserAccountService accounts = mock(UserAccountService.class);
    private final AtomicReference<@Nullable Authentication> authentication = new AtomicReference<>();
    private final IdentityPlatformAccounts platformAccounts = new IdentityPlatformAccounts(accounts, authentication::get);

    @Test
    void currentIsTheSignedInIdentityAccountOnly() {
        assertThat(platformAccounts.current()).isEmpty();

        authentication.set(new AnonymousAuthenticationToken("key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThat(platformAccounts.current()).isEmpty();

        authentication.set(UsernamePasswordAuthenticationToken.authenticated("someone", null,
                AuthorityUtils.createAuthorityList("ROLE_USER")));
        assertThat(platformAccounts.current()).as("foreign principal type").isEmpty();

        IdentityUserDetails principal = mock(IdentityUserDetails.class);
        when(principal.userId()).thenReturn(42L);
        authentication.set(UsernamePasswordAuthenticationToken.authenticated(principal, null,
                AuthorityUtils.createAuthorityList("ROLE_USER")));
        assertThat(platformAccounts.current()).contains(AccountId.of(42));
    }

    @Test
    void displayNameOfKnownAccountsOnly() {
        account(1L, true);
        assertThat(platformAccounts.displayName(AccountId.of(1))).contains("User 1");
        assertThat(platformAccounts.displayName(AccountId.of(2))).isEmpty();
    }

    @Test
    void globalAdministratorsAndTestersOfExactlyThisGameAreTesters() {
        account(1L, true, ScopedRole.global(IdentityPlatformAccounts.SYSTEM_ADMIN));
        account(2L, true, ScopedRole.of(IdentityPlatformAccounts.GAME_TESTER, Scope.of("game", "starfare")));
        account(3L, true, ScopedRole.of(IdentityPlatformAccounts.GAME_TESTER, Scope.of("game", "frontier-lords")));
        account(4L, true, ScopedRole.global(IdentityPlatformAccounts.GAME_TESTER));
        account(5L, true, ScopedRole.of(IdentityPlatformAccounts.SYSTEM_ADMIN, Scope.of("game", "starfare")));
        account(6L, false, ScopedRole.global(IdentityPlatformAccounts.SYSTEM_ADMIN),
                ScopedRole.of(IdentityPlatformAccounts.GAME_TESTER, Scope.of("game", "starfare")));
        account(7L, true);

        assertThat(platformAccounts.isTester(AccountId.of(1), "starfare")).isTrue();
        assertThat(platformAccounts.isTester(AccountId.of(2), "starfare")).isTrue();
        assertThat(platformAccounts.isTester(AccountId.of(3), "starfare")).as("tester of another game").isFalse();
        assertThat(platformAccounts.isTester(AccountId.of(4), "starfare")).as("global tester role").isFalse();
        assertThat(platformAccounts.isTester(AccountId.of(5), "starfare")).as("scoped administrator").isFalse();
        assertThat(platformAccounts.isTester(AccountId.of(6), "starfare")).as("disabled").isFalse();
        assertThat(platformAccounts.isTester(AccountId.of(7), "starfare")).as("ordinary").isFalse();
        assertThat(platformAccounts.isTester(AccountId.of(8), "starfare")).as("unknown").isFalse();
    }

    private void account(long id, boolean enabled, ScopedRole... roles) {
        when(accounts.findById(id)).thenReturn(Optional.of(new UserAccountDto(id, "user" + id + "@example.org",
                AccountName.display("User " + id), enabled, true, Instant.EPOCH, Set.of(roles), false, Locale.GERMAN)));
    }
}
