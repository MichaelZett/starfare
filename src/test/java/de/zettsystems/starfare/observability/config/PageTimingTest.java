package de.zettsystems.starfare.observability.config;

import com.vaadin.flow.server.VaadinRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
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

    @Test
    void requestEndLogsTheStatementsRecordedSinceRequestStart(CapturedOutput output) {
        VaadinRequest request = requestWithAttributes();
        timing.requestStart(request, null);
        new SqlStatementRecorder().inspect("select g1_0.id from game_sessions g1_0 where g1_0.id=?");
        request.setAttribute(PageTiming.START_ATTRIBUTE, System.nanoTime() - 2_000_000_000L);
        request.setAttribute(PageTiming.ROUTE_ATTRIBUTE, "map/7");

        timing.requestEnd(request, null, null);

        assertThat(output).contains("Langsame Seite „map/7“:").contains(" ms, 1 SQL-Abfragen");
        assertThat(SqlStatementRecorder.end().count()).isZero();
    }

    @Test
    void requestEndWithoutStartOnlyEndsTheRecording(CapturedOutput output) {
        SqlStatementRecorder.begin();
        new SqlStatementRecorder().inspect("select 1");

        timing.requestEnd(requestWithAttributes(), null, null);

        assertThat(output).doesNotContain("SQL-Abfragen");
        assertThat(SqlStatementRecorder.end().count()).isZero();
    }

    private static VaadinRequest requestWithAttributes() {
        Map<String, Object> attributes = new HashMap<>();
        VaadinRequest request = mock(VaadinRequest.class);
        doAnswer(call -> attributes.put(call.getArgument(0), call.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        when(request.getAttribute(anyString())).thenAnswer(call -> attributes.get(call.<String>getArgument(0)));
        return request;
    }
}
