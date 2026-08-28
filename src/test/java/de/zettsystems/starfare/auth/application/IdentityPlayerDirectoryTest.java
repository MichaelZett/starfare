package de.zettsystems.starfare.auth.application;

import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.identity.values.UserAccountDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityPlayerDirectoryTest {

    private static final Instant START = Instant.parse("2026-08-28T10:00:00Z");

    private final UserAccountService accounts = mock(UserAccountService.class);
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
        UserAccountDto alice = mock(UserAccountDto.class);
        when(alice.displayName()).thenReturn("Alice");
        when(accounts.findById(42L)).thenReturn(Optional.of(alice));

        assertThat(directory.displayName("42")).isEqualTo("Alice");
    }

    @Test
    void fallsBackToIdForUnknownAccount() {
        when(accounts.findById(anyLong())).thenReturn(Optional.empty());

        assertThat(directory.displayName("7")).isEqualTo("7");
    }

    @Test
    void fallsBackToIdForNonNumericId() {
        assertThat(directory.displayName("legacy-name")).isEqualTo("legacy-name");
        verify(accounts, times(0)).findById(anyLong());
    }

    @Test
    void cachesWithinTtlAndRefreshesAfterwards() {
        when(accounts.findById(1L)).thenReturn(Optional.empty());

        directory.displayName("1");
        directory.displayName("1");
        verify(accounts, times(1)).findById(1L);

        now.set(START.plus(IdentityPlayerDirectory.TTL).plusSeconds(1));
        directory.displayName("1");
        verify(accounts, times(2)).findById(1L);
    }

    @Test
    void resolvesSeveralIdsKeepingOrder() {
        when(accounts.findById(anyLong())).thenReturn(Optional.empty());

        assertThat(directory.displayNames(List.of("3", "1", "2")).keySet())
                .containsExactly("3", "1", "2");
    }
}
