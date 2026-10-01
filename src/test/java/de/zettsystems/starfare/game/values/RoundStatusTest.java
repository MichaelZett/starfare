package de.zettsystems.starfare.game.values;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoundStatusTest {
    @Test
    void identifiesTheSubmittedPlayerAndAllPendingNamesInSeatOrder() {
        var status = new RoundStatus(List.of(
                new RoundStatus.Seat(1, "Alpha", "#111111", true),
                new RoundStatus.Seat(2, "Beta", "#222222", false),
                new RoundStatus.Seat(3, "Gamma", "#333333", false)), null, null);

        assertThat(status.hasSubmitted(1)).isTrue();
        assertThat(status.hasSubmitted(2)).isFalse();
        assertThat(status.hasSubmitted(-1)).isFalse();
        assertThat(status.pendingPlayerLabels()).containsExactly("Beta", "Gamma");
    }

    @Test
    void emptyAndFullySubmittedRoundsHaveNoPendingNames() {
        assertThat(RoundStatus.NONE.hasSubmitted(1)).isFalse();
        assertThat(RoundStatus.NONE.pendingPlayerLabels()).isEmpty();
        var submitted = new RoundStatus(List.of(new RoundStatus.Seat(1, "Alpha", "#111111", true)), null, null);
        assertThat(submitted.pendingPlayerLabels()).isEmpty();
    }
}
