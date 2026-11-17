package de.zettsystems.starfare.auth.ui;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.PlatformAccounts;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserContextTest {
    /** The binding of a Spring context that other tests in this JVM share; restored after each test. */
    private @Nullable PlatformAccounts previous;

    @BeforeEach
    void unbind() {
        previous = UserContext.bind(null);
    }

    @AfterEach
    void restore() {
        UserContext.bind(previous);
    }

    @Test
    void resolvesTheAccountThePlatformReportsAsSignedIn() {
        PlatformAccounts accounts = mock(PlatformAccounts.class);
        when(accounts.current()).thenReturn(Optional.of(AccountId.of(42)));
        new UserContextBinding(accounts);

        assertThat(UserContext.currentUserId()).hasValue(42L);
        assertThat(UserContext.currentPlayerId()).hasValue("42");
    }

    @Test
    void emptyWhenNobodyIsSignedIn() {
        PlatformAccounts accounts = mock(PlatformAccounts.class);
        when(accounts.current()).thenReturn(Optional.empty());
        UserContext.bind(accounts);

        assertThat(UserContext.currentPlayerId()).isEmpty();
    }

    @Test
    void emptyWithoutPlatform() {
        assertThat(UserContext.currentUserId()).isEmpty();
        assertThat(UserContext.currentPlayerId()).isEmpty();
    }
}
