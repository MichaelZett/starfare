package de.zettsystems.starfare.auth.config;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import de.zettsystems.identity.ui.LoginView;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/register", "/register/**", "/password/**")
                        .permitAll()
                        .requestMatchers("/favicon.ico", "/icons/**", "/images/**", "/styles.css",
                                "/manifest.webmanifest", "/sw.js", "/offline.html", "/robots.txt")
                        .permitAll()
                        .requestMatchers(EndpointRequest.to("health", "info"))
                        .permitAll()
                        .requestMatchers(EndpointRequest.toAnyEndpoint())
                        .hasRole("SYSTEM_ADMIN"))
                .with(VaadinSecurityConfigurer.vaadin(), configurer -> configurer.loginView(LoginView.class))
                .build();
    }
}
