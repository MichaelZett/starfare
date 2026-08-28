package de.zettsystems.starfare.auth.config;

import de.zettsystems.starfare.AbstractIntegrationTest;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards the Starfare-specific part of the security chain. Login, registration and
 * password reset themselves are covered in the zs-identity repository.
 */
class SecurityChainTest extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void anonymousRequestToProtectedRouteRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/map"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));
    }

    @Test
    void identityRoutesAreAnonymouslyReachable() throws Exception {
        assertPassesSecurityChain("/login");
        assertPassesSecurityChain("/register");
        assertPassesSecurityChain("/password/forgot");
    }

    @Test
    void healthAndInfoArePublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
    }

    @Test
    void otherActuatorEndpointsRequireSystemAdmin() throws Exception {
        mockMvc.perform(get("/actuator"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));
    }

    @Test
    @WithMockUser(roles = "USER")
    void plainUserIsDeniedOnActuatorRoot() throws Exception {
        mockMvc.perform(get("/actuator")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void systemAdminReachesActuatorRoot() throws Exception {
        mockMvc.perform(get("/actuator")).andExpect(status().isOk());
    }

    /**
     * MockMvc has no Vaadin servlet, so a request that passes the security chain fails with a
     * "no servlet" {@link ServletException}; a redirect to {@code /login} fails the status check.
     */
    private void assertPassesSecurityChain(String path) throws Exception {
        try {
            mockMvc.perform(get(path)).andExpect(status().is2xxSuccessful());
        } catch (ServletException e) {
            assertThat(e.getMessage()).contains("springServlet");
        }
    }
}
