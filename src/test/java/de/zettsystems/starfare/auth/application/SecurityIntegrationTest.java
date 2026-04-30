package de.zettsystems.starfare.auth.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserService userService;

    @Autowired
    private UserStore userStore;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        if (!userStore.exists("alice")) {
            userService.register("alice", "secret123", "Alice");
        }
    }

    @Test
    void anonymousRequestToProtectedPathRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/map"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));
    }

    @Test
    void validCredentialsLogIn() throws Exception {
        mockMvc.perform(formLogin("/login").user("alice").password("secret123"))
                .andExpect(authenticated().withUsername("alice"));
    }

    @Test
    void invalidCredentialsAreRejected() throws Exception {
        mockMvc.perform(formLogin("/login").user("alice").password("wrong-password"))
                .andExpect(unauthenticated());
    }

    @Test
    void unknownUserIsRejected() throws Exception {
        mockMvc.perform(formLogin("/login").user("ghost").password("whatever1"))
                .andExpect(unauthenticated());
    }
}
