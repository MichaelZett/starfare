package de.zettsystems.starfare.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A new migration takes the next number. Production holds every earlier one; Flyway refuses a migration
 * numbered below the highest applied one ("resolved migration not applied to database"), which a fresh test
 * database never shows (2026-11-17: V2_10 added below V2_11, the platform's release 0.1.8 did not start).
 */
class MigrationVersionsTest {

    /** Never used before V2_11 went to production; it stays empty. */
    private static final int HISTORIC_GAP = 10;

    @Test
    void migrationsAreNumberedWithoutGaps() throws IOException {
        List<Integer> minors;
        try (var files = Files.list(Path.of("src/main/resources/db/migration"))) {
            minors = files.map(file -> file.getFileName().toString())
                    .filter(name -> name.endsWith(".sql"))
                    .map(name -> {
                        assertThat(name).as("migration file name").matches("V2_\\d+__\\w+\\.sql");
                        return Integer.valueOf(name.substring("V2_".length(), name.indexOf("__")));
                    })
                    .sorted().toList();
        }
        assertThat(minors).isNotEmpty();
        int highest = minors.getLast();
        assertThat(minors)
                .as("V2_1 to V2_%d without V2_%d: number a new migration V2_%d", highest, HISTORIC_GAP, highest + 1)
                .containsExactlyElementsOf(IntStream.rangeClosed(1, highest).filter(minor -> minor != HISTORIC_GAP).boxed().toList());
    }
}
