package de.zettsystems.starfare.report.application;

import de.zettsystems.starfare.game.domain.GameState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReportServiceTest {

    @Test
    void appendReportAddsLineForCurrentTurn() {
        GameState state = new GameState();
        ReportService reportService = new DefaultReportService();

        reportService.appendReport(state, 1, "Line A");
        reportService.appendReport(state, 1, "Line B");

        var report = state.reports().get(1);
        assertThat(report.turn()).isEqualTo(state.turn());
        assertThat(report.lines()).hasSize(2);
        assertThat(report.lines()).first().isEqualTo("Line A");
    }

    @Test
    void hasSignificantEventsChecksPreviousTurn() {
        GameState state = new GameState();
        ReportService reportService = new DefaultReportService();
        reportService.appendReport(state, 1, "Event");

        state.nextTurn();

        assertThat(reportService.hasSignificantEventsFor(state, 1)).isTrue();
    }
}
