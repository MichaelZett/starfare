package de.zettsystems.starfare.testsupport;

import org.testcontainers.utility.DockerImageName;

/**
 * The PostgreSQL image for Testcontainers. Gradle passes {@code postgres.version} from
 * {@code gradle.properties}; the fallback only serves runs started directly from the IDE and is
 * kept in sync by the task {@code verifyPostgresVersion}.
 */
public final class PostgresTestImage {

    private static final String DEFAULT_VERSION = "18.7";

    private PostgresTestImage() {
    }

    public static DockerImageName dockerImage() {
        return DockerImageName.parse("postgres:" + version() + "-alpine")
                .asCompatibleSubstituteFor("postgres");
    }

    private static String version() {
        String version = System.getProperty("postgres.version");
        return version == null || version.isBlank() ? DEFAULT_VERSION : version;
    }
}
