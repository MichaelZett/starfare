package de.zettsystems.starfare.game.ui;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SendDialogOutcomeTest {
    @Test
    void closingWithoutAcceptanceCancelsExactlyOnce() {
        var calls = new ArrayList<String>();
        var outcome = new SendDialogOutcome(() -> calls.add("accepted"), () -> calls.add("cancelled"));
        outcome.closed();
        outcome.closed();
        assertThat(calls).containsExactly("cancelled");
    }

    @Test
    void acceptedCommandClosesWithoutCancellation() {
        var calls = new ArrayList<String>();
        var outcome = new SendDialogOutcome(() -> calls.add("accepted"), () -> calls.add("cancelled"));
        outcome.accepted();
        assertThat(calls).isEmpty();
        outcome.closed();
        outcome.closed();
        assertThat(calls).containsExactly("accepted");
    }
}
