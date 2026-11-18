package de.zettsystems.starfare.standalone.platform;

import de.zettsystems.gamesplatformapi.AccountId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SoloPlatformAccountsTest {
    private final SoloPlatformAccounts accounts = new SoloPlatformAccounts();

    @Test
    void everybodyPlaysAsTheStandardAccountWithoutSigningIn() {
        assertThat(accounts.current()).contains(SoloPlatformAccounts.STANDARD);
        assertThat(accounts.displayName(SoloPlatformAccounts.STANDARD)).contains("Commander");
        assertThat(accounts.isTester(SoloPlatformAccounts.STANDARD, "starfare")).isTrue();
    }

    @Test
    void knowsNoOtherAccount() {
        AccountId other = AccountId.of(2);
        assertThat(accounts.displayName(other)).isEmpty();
        assertThat(accounts.isTester(other, "starfare")).isFalse();
    }
}
