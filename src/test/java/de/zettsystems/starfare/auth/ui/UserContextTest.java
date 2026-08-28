package de.zettsystems.starfare.auth.ui;

import de.zettsystems.identity.application.IdentityUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserContextTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesAccountIdFromIdentityPrincipal() {
        IdentityUserDetails principal = mock(IdentityUserDetails.class);
        when(principal.userId()).thenReturn(42L);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null,
                        AuthorityUtils.createAuthorityList("ROLE_USER")));

        assertThat(UserContext.currentUserId()).hasValue(42L);
        assertThat(UserContext.currentPlayerId()).hasValue("42");
    }

    @Test
    void emptyWithoutAuthentication() {
        assertThat(UserContext.currentUserId()).isEmpty();
        assertThat(UserContext.currentPlayerId()).isEmpty();
    }

    @Test
    void emptyForAnonymousPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser",
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(UserContext.currentPlayerId()).isEmpty();
    }

    @Test
    void emptyForForeignPrincipalType() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("someone", null,
                        AuthorityUtils.createAuthorityList("ROLE_USER")));

        assertThat(UserContext.currentPlayerId()).isEmpty();
    }
}
