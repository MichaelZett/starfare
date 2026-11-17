package de.zettsystems.starfare.game.application;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.PlatformAccounts;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.game.values.RulesetRef;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Who counts as a tester is the platform's rule; see the host's tests and IdentityPlatformAccountsTest. */
class DefaultRulesetAccessTest {
    private static final RulesetRef SPACEWARD = RulesetCatalog.ECONOMY_CANDIDATE;
    private static final RulesetRef SECTOR_FORCES = RulesetRef.SECTOR_FORCES;

    private final PlatformAccounts accounts = mock(PlatformAccounts.class);
    private final RulesetAccess host = new DefaultRulesetAccess(RulesetCatalog.builtIn(), accounts, false);

    @Test
    void sectorForcesStaysOpenForEveryoneWithoutAskingThePlatform() {
        assertThat(host.mayUse(SECTOR_FORCES, "1")).isTrue();
        assertThat(host.mayUse(SECTOR_FORCES, null)).isTrue();
        verifyNoInteractions(accounts);
    }

    @Test
    void testersOfStarfareMayUseTheTestVariant() {
        when(accounts.isTester(AccountId.of(2), "starfare")).thenReturn(true);
        assertThat(host.mayUse(SPACEWARD, "2")).isTrue();
        assertThat(host.mayUse(RulesetRef.SPACEWARD, "2")).as("stored older Spaceward versions count as Spaceward").isTrue();
    }

    @Test
    void othersAndAnonymousAccountsAreRefused() {
        assertThat(host.mayUse(SPACEWARD, "7")).isFalse();
        assertThat(host.mayUse(SPACEWARD, null)).isFalse();
        assertThat(host.mayUse(SPACEWARD, "")).isFalse();
        assertThat(host.mayUse(SPACEWARD, "host")).as("no numeric account id").isFalse();
        assertThat(host.mayUse(SPACEWARD, "0")).as("no positive account id").isFalse();
        verify(accounts, times(1)).isTester(any(), anyString());
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
    void listingAsksThePlatformAtMostOnceAndOnlyWhenNeeded() {
        var usable = host.usableBy("9");
        assertThat(usable.test(SECTOR_FORCES)).isTrue();
        verifyNoInteractions(accounts);
        assertThat(usable.test(SPACEWARD)).isFalse();
        assertThat(usable.test(SPACEWARD)).isFalse();
        verify(accounts, times(1)).isTester(AccountId.of(9), "starfare");
    }
}
