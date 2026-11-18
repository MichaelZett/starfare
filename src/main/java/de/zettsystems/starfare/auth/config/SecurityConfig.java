package de.zettsystems.starfare.auth.config;

import com.vaadin.flow.spring.security.NavigationAccessControlConfigurer;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Starfare's own start has no sign-in: everybody plays as its standard account
 * ({@code standalone.platform.SoloPlatformAccounts}), games against the computer (plan Z3 b). So
 * every page is open and the views' access annotations are not checked; inside a platform host the
 * host's security decides. Actuator endpoints other than health and info stay closed.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(EndpointRequest.to("health", "info"))
                        .permitAll()
                        .requestMatchers(EndpointRequest.toAnyEndpoint())
                        .hasRole("SYSTEM_ADMIN"))
                .with(VaadinSecurityConfigurer.vaadin(), configurer -> configurer
                        .enableNavigationAccessControl(false)
                        .anyRequest(AuthorizeHttpRequestsConfigurer.AuthorizedUrl::permitAll))
                .build();
    }

    /** Without a sign-in there is nobody to check views against. */
    @Bean
    NavigationAccessControlConfigurer navigationAccessControlConfigurer() {
        return new NavigationAccessControlConfigurer().disabled();
    }
}
