package de.zettsystems.starfare.auth.application;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.PlatformAccounts;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityPlayerDirectoryTest {

    private final PlatformAccounts accounts = mock(PlatformAccounts.class);
    private final IdentityPlayerDirectory directory = new IdentityPlayerDirectory(accounts);

    @Test
    void resolvesDisplayNameOfKnownAccount() {
        when(accounts.displayName(AccountId.of(42))).thenReturn(Optional.of("Alice"));

        assertThat(directory.displayName("42")).isEqualTo("Alice");
    }

    @Test
    void fallsBackToIdForUnknownAccount() {
        when(accounts.displayName(any())).thenReturn(Optional.empty());

        assertThat(directory.displayName("7")).isEqualTo("7");
    }

    @Test
    void fallsBackToIdForIdsThatAreNoAccounts() {
        assertThat(directory.displayName("legacy-name")).isEqualTo("legacy-name");
        assertThat(directory.displayName("0")).isEqualTo("0");
        verify(accounts, never()).displayName(any());
    }

    @Test
    void asksThePlatformEveryTimeSoRenamesShowAtOnce() {
        when(accounts.displayName(AccountId.of(1))).thenReturn(Optional.of("Old"), Optional.of("New"));

        assertThat(directory.displayName("1")).isEqualTo("Old");
        assertThat(directory.displayName("1")).isEqualTo("New");
    }

    @Test
    void resolvesAListWithOneLookupKeepingOrderAndFallingBackToTheId() {
        when(accounts.displayNames(anyCollection())).thenReturn(Map.of(AccountId.of(3), "Carol", AccountId.of(1), "Alice"));

        assertThat(directory.displayNames(List.of("3", "ai-2", "1", "9")))
                .containsExactly(Map.entry("3", "Carol"), Map.entry("ai-2", "ai-2"), Map.entry("1", "Alice"),
                        Map.entry("9", "9"));
        verify(accounts, times(1)).displayNames(List.of(AccountId.of(3), AccountId.of(1), AccountId.of(9)));
        verify(accounts, never()).displayName(any());
    }

    @Test
    void anEmptyListNeedsNoLookup() {
        assertThat(directory.displayNames(List.of("ai-1"))).containsExactly(Map.entry("ai-1", "ai-1"));
        verify(accounts, never()).displayNames(anyCollection());
    }
}
