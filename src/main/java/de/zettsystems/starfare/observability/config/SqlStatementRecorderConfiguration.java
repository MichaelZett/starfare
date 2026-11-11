package de.zettsystems.starfare.observability.config;

import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link SqlStatementRecorder} from Starfare's own configuration, so a host does not need
 * a Starfare line in its YAML. An inspector set explicitly (for example by a test) wins.
 */
@Configuration(proxyBeanMethods = false)
public class SqlStatementRecorderConfiguration {
    static final String STATEMENT_INSPECTOR = "hibernate.session_factory.statement_inspector";

    @Bean
    HibernatePropertiesCustomizer sqlStatementRecorderCustomizer() {
        return properties -> properties.putIfAbsent(STATEMENT_INSPECTOR, SqlStatementRecorder.class.getName());
    }
}
