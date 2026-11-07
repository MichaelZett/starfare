package de.zettsystems.starfare.observability.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageTimingTest {

    private final PageTiming timing = new PageTiming(500, 20);

    @Test
    void slowNavigationIsASlowPageWithItsRepeatedStatements() {
        assertThat(timing.report("map", null, 800, 12, List.of("9× from game_sessions g1_0 where g1_0.id=?")))
                .contains("Langsame Seite „map“: 800 ms, 12 SQL-Abfragen — 9× from game_sessions g1_0 where g1_0.id=?");
    }

    @Test
    void slowRequestOfAUiIsASlowAction() {
        assertThat(timing.report(null, "lobby", 500, 0, List.of()))
                .contains("Langsame Aktion „lobby“: 500 ms, 0 SQL-Abfragen");
    }

    @Test
    void fastRequestIsReportedOnlyForManyOrRepeatedStatements() {
        assertThat(timing.report("", null, 40, 19, List.of())).isEmpty();
        assertThat(timing.report("", null, 40, 20, List.of())).contains("Viele Abfragen „“: 40 ms, 20 SQL-Abfragen");
        assertThat(timing.report(null, "archive", 40, 3, List.of("3× from game_archives")))
                .contains("Viele Abfragen „archive“: 40 ms, 3 SQL-Abfragen — 3× from game_archives");
    }

    @Test
    void requestWithoutRouteOrViewIsNeverReported() {
        assertThat(timing.report(null, null, 5_000, 100, List.of("5× from x"))).isEmpty();
    }
}
