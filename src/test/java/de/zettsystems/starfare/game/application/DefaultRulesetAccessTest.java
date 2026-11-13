package de.zettsystems.starfare.game.application;

import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.identity.values.AccountName;
import de.zettsystems.identity.values.Scope;
import de.zettsystems.identity.values.ScopedRole;
import de.zettsystems.identity.values.UserAccountDto;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.RulesetRef;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultRulesetAccessTest {
    private static final RulesetRef SPACEWARD = RulesetCatalog.ECONOMY_CANDIDATE;
    private static final RulesetRef SECTOR_FORCES = RulesetRef.SECTOR_FORCES;

    private final UserAccountService accounts = mock(UserAccountService.class);
    private final RulesetAccess host = new DefaultRulesetAccess(RulesetCatalog.builtIn(), accounts, false);

    @Test
    void sectorForcesStaysOpenForEveryoneWithoutReadingTheAccount() {
        assertThat(host.mayUse(SECTOR_FORCES, "1")).isTrue();
        assertThat(host.mayUse(SECTOR_FORCES, null)).isTrue();
        verifyNoInteractions(accounts);
    }

    @Test
    void globalAdministratorMayUseTheTestVariant() {
        account(1L, true, ScopedRole.global(DefaultRulesetAccess.SYSTEM_ADMIN));
        assertThat(host.mayUse(SPACEWARD, "1")).isTrue();
    }

    @Test
    void testerOfStarfareMayUseTheTestVariant() {
        account(2L, true, ScopedRole.of(DefaultRulesetAccess.GAME_TESTER, Scope.of("game", "starfare")));
        assertThat(host.mayUse(SPACEWARD, "2")).isTrue();
        assertThat(host.mayUse(RulesetRef.SPACEWARD, "2")).as("stored older Spaceward versions count as Spaceward").isTrue();
    }

    @Test
    void testerOfAnotherGameIsNoStarfareTester() {
        account(3L, true, ScopedRole.of(DefaultRulesetAccess.GAME_TESTER, Scope.of("game", "frontier-lords")));
        assertThat(host.mayUse(SPACEWARD, "3")).isFalse();
    }

    @Test
    void globalTesterRoleWithoutGameScopeIsNotEnough() {
        account(4L, true, ScopedRole.global(DefaultRulesetAccess.GAME_TESTER));
        assertThat(host.mayUse(SPACEWARD, "4")).isFalse();
    }

    @Test
    void administratorRoleInAScopeIsNoGlobalAdministrator() {
        account(5L, true, ScopedRole.of(DefaultRulesetAccess.SYSTEM_ADMIN, Scope.of("game", "starfare")));
        assertThat(host.mayUse(SPACEWARD, "5")).isFalse();
    }

    @Test
    void disabledTesterLosesAccess() {
        account(6L, false, ScopedRole.global(DefaultRulesetAccess.SYSTEM_ADMIN),
                ScopedRole.of(DefaultRulesetAccess.GAME_TESTER, Scope.of("game", "starfare")));
        assertThat(host.mayUse(SPACEWARD, "6")).isFalse();
    }

    @Test
    void ordinaryUnknownAndAnonymousAccountsAreRefused() {
        account(7L, true);
        when(accounts.findById(8L)).thenReturn(Optional.empty());
        assertThat(host.mayUse(SPACEWARD, "7")).isFalse();
        assertThat(host.mayUse(SPACEWARD, "8")).isFalse();
        assertThat(host.mayUse(SPACEWARD, null)).isFalse();
        assertThat(host.mayUse(SPACEWARD, "")).isFalse();
        assertThat(host.mayUse(SPACEWARD, "host")).as("no numeric account id").isFalse();
    }

    @Test
    void ownStartLeavesEveryVariantOpen() {
        RulesetAccess standalone = new DefaultRulesetAccess(RulesetCatalog.builtIn(), accounts, true);
        assertThat(standalone.mayUse(SPACEWARD, null)).isTrue();
        assertThat(standalone.mayUse(SPACEWARD, "7")).isTrue();
        assertThat(standalone.usableBy(null).test(SPACEWARD)).isTrue();
        verifyNoInteractions(accounts);
    }

    @Test
    void listingReadsTheAccountAtMostOnceAndOnlyWhenNeeded() {
        account(9L, true);
        var usable = host.usableBy("9");
        assertThat(usable.test(SECTOR_FORCES)).isTrue();
        verifyNoInteractions(accounts);
        assertThat(usable.test(SPACEWARD)).isFalse();
        assertThat(usable.test(SPACEWARD)).isFalse();
        verify(accounts, times(1)).findById(anyLong());
    }

    private void account(long id, boolean enabled, ScopedRole... roles) {
        when(accounts.findById(id)).thenReturn(Optional.of(new UserAccountDto(id, "user" + id + "@example.org",
                AccountName.display("User " + id), enabled, true, Instant.EPOCH, Set.of(roles), false, Locale.GERMAN)));
    }
}
