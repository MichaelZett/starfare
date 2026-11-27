package de.zettsystems.starfare.game.application;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.LifecycleStage;
import de.zettsystems.gamesplatformapi.PlatformAccounts;
import de.zettsystems.starfare.game.values.RulesetRef;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Who may use a variant is the platform's rule (stage of the variant and role); see the host's tests. */
class DefaultRulesetAccessTest {
    private static final RulesetRef EXTENDED = RulesetRef.EXTENDED;
    private static final RulesetRef SECTOR_FORCES = RulesetRef.SECTOR_FORCES;

    private final PlatformAccounts accounts = mock(PlatformAccounts.class);
    private final RulesetAccess host = new DefaultRulesetAccess(accounts, false);

    @Test
    void asksThePlatformForTheVariantOfTheRules() {
        when(accounts.mayUseVariant(AccountId.of(2), "starfare", "extended")).thenReturn(true);
        when(accounts.mayUseVariant(AccountId.of(2), "starfare", "classic")).thenReturn(true);

        assertThat(host.mayUse(EXTENDED, "2")).isTrue();
        assertThat(host.mayUse(RulesetRef.EXTENDED, "2")).as("stored older Extended versions count as Extended").isTrue();
        assertThat(host.mayUse(SECTOR_FORCES, "2")).isTrue();
    }

    @Test
    void followsTheRefusalOfThePlatform() {
        when(accounts.mayUseVariant(AccountId.of(7), "starfare", "classic")).thenReturn(true);

        assertThat(host.mayUse(EXTENDED, "7")).isFalse();
        assertThat(host.mayUse(SECTOR_FORCES, "7")).isTrue();
    }

    @Test
    void refusesEveryVariantWithoutAPlatformAccount() {
        assertThat(host.mayUse(SECTOR_FORCES, null)).isFalse();
        assertThat(host.mayUse(EXTENDED, "")).isFalse();
        assertThat(host.mayUse(EXTENDED, "host")).as("no numeric account id").isFalse();
        assertThat(host.mayUse(EXTENDED, "0")).as("no positive account id").isFalse();
        assertThat(host.usableBy(null).test(SECTOR_FORCES)).isFalse();
        verifyNoInteractions(accounts);
    }

    @Test
    void ownStartLeavesEveryVariantOpen() {
        RulesetAccess standalone = new DefaultRulesetAccess(accounts, true);
        assertThat(standalone.mayUse(EXTENDED, null)).isTrue();
        assertThat(standalone.mayUse(EXTENDED, "7")).isTrue();
        assertThat(standalone.usableBy(null).test(EXTENDED)).isTrue();
        verifyNoInteractions(accounts);
    }

    @Test
    void marksVariantsInAlphaOrBetaOnly() {
        when(accounts.variantStage("starfare", "extended")).thenReturn(LifecycleStage.ALPHA);
        when(accounts.variantStage("starfare", "classic")).thenReturn(LifecycleStage.PRODUCTION);

        assertThat(host.marking(EXTENDED)).contains(LifecycleStage.ALPHA);
        assertThat(host.marking(SECTOR_FORCES)).isEmpty();

        when(accounts.variantStage("starfare", "extended")).thenReturn(LifecycleStage.BETA);
        assertThat(host.marking(EXTENDED)).contains(LifecycleStage.BETA);
    }

    @Test
    void ownStartMarksNothing() {
        assertThat(new DefaultRulesetAccess(accounts, true).marking(EXTENDED)).isEmpty();
        verifyNoInteractions(accounts);
    }

    @Test
    void listingAsksThePlatformOncePerVariant() {
        when(accounts.mayUseVariant(AccountId.of(9), "starfare", "classic")).thenReturn(true);
        var usable = host.usableBy("9");

        assertThat(usable.test(SECTOR_FORCES)).isTrue();
        assertThat(usable.test(EXTENDED)).isFalse();
        assertThat(usable.test(RulesetRef.EXTENDED)).isFalse();
        assertThat(usable.test(SECTOR_FORCES)).isTrue();

        verify(accounts, times(1)).mayUseVariant(AccountId.of(9), "starfare", "classic");
        verify(accounts, times(1)).mayUseVariant(AccountId.of(9), "starfare", "extended");
    }
}
