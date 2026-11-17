package de.zettsystems.starfare.auth.application;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.PlatformAccounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityPlayerDirectoryTest {

    private static final Instant START = Instant.parse("2026-08-28T10:00:00Z");

    private final PlatformAccounts accounts = mock(PlatformAccounts.class);
    private final AtomicReference<Instant> now = new AtomicReference<>(START);
    private IdentityPlayerDirectory directory;

    @BeforeEach
    void setUp() {
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenAnswer(_ -> now.get());
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        directory = new IdentityPlayerDirectory(accounts, clock);
    }

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
        verify(accounts, times(0)).displayName(any());
    }

    @Test
    void cachesWithinTtlAndRefreshesAfterwards() {
        when(accounts.displayName(AccountId.of(1))).thenReturn(Optional.empty());

        directory.displayName("1");
        directory.displayName("1");
        verify(accounts, times(1)).displayName(AccountId.of(1));

        now.set(START.plus(IdentityPlayerDirectory.TTL).plusSeconds(1));
        directory.displayName("1");
        verify(accounts, times(2)).displayName(AccountId.of(1));
    }

    @Test
    void resolvesSeveralIdsKeepingOrder() {
        when(accounts.displayName(any())).thenReturn(Optional.empty());

        assertThat(directory.displayNames(List.of("3", "1", "2")).keySet())
                .containsExactly("3", "1", "2");
    }
}
