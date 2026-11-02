package de.zettsystems.starfare.auth.config;

import de.zettsystems.identity.application.IdentityMailHealthIndicator;
import de.zettsystems.starfare.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.actuate.endpoint.HttpCodeStatusMapper;
import org.springframework.boot.health.actuate.endpoint.StatusAggregator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Down and degraded are kept apart: a dead mail server (nothing listens on port 1) does not take
 * Starfare down. Covers the interplay of {@code zs.identity.mail-health} and the status order in
 * application.yaml, which only exists in a real context. The check itself is tested in zs-identity.
 */
@TestPropertySource(properties = {"spring.mail.host=localhost", "spring.mail.port=1"})
class HealthTrafficLightTest extends AbstractIntegrationTest {

    private static final String HEALTH = "/actuator/health";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private IdentityMailHealthIndicator indicator;

    @Autowired
    private HealthEndpoint endpoint;

    @Autowired
    private HttpCodeStatusMapper mapper;

    @Autowired
    private StatusAggregator aggregator;

    @Autowired
    private JavaMailSenderImpl mailSender;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void deadMailServerDegradesInsteadOfTakingTheGameDown() throws Exception {
        indicator.probe();

        Status overall = endpoint.health().getStatus();
        assertThat(overall).isEqualTo(IdentityMailHealthIndicator.DEGRADED);
        assertThat(mapper.getStatusCode(overall)).isEqualTo(200);
        mockMvc.perform(get(HEALTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"));
    }

    @Test
    void acceptedMailCountsAsReachable() {
        indicator.probe();

        indicator.delivered();

        assertThat(endpoint.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void realOutageOutranksDegraded() {
        assertThat(aggregator.getAggregateStatus(Status.UP, IdentityMailHealthIndicator.DEGRADED))
                .isEqualTo(IdentityMailHealthIndicator.DEGRADED);
        assertThat(aggregator.getAggregateStatus(Status.DOWN, IdentityMailHealthIndicator.DEGRADED))
                .isEqualTo(Status.DOWN);
        assertThat(mapper.getStatusCode(Status.DOWN)).isEqualTo(503);
    }

    @Test
    void notYetCheckedDoesNotColourTheTrafficLight() {
        assertThat(aggregator.getAggregateStatus(Status.UP, Status.UNKNOWN)).isEqualTo(Status.UP);
    }

    @Test
    void publicAnswerCarriesTheStatusOnly() throws Exception {
        indicator.probe();

        mockMvc.perform(get(HEALTH))
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "USER")
    void plainUserSeesNoDetails() throws Exception {
        indicator.probe();

        mockMvc.perform(get(HEALTH))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void administratorSeesWhySinceWhen() throws Exception {
        indicator.probe();

        mockMvc.perform(get(HEALTH))
                .andExpect(jsonPath("$.components.mail.status").value("DEGRADED"))
                .andExpect(jsonPath("$.components.mail.details.since").exists())
                .andExpect(jsonPath("$.components.mail.details.error").exists());
    }

    @Test
    void mailServerThatStopsAnsweringCannotBlockForever() {
        assertThat(mailSender.getJavaMailProperties())
                .containsEntry("mail.smtp.connectiontimeout", "10000")
                .containsEntry("mail.smtp.timeout", "10000")
                .containsEntry("mail.smtp.writetimeout", "10000");
    }
}
