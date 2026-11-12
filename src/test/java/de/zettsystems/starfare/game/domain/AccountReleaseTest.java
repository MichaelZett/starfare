package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.game.values.Player;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccountReleaseTest {
    @Test void releasesOnlyTheDeletedAccountsBindingsAndKeepsItsSeatPlayableByAi() {
        var state = new GameState();
        state.players().add(new Player(1, "Deleted", false, "#ffffff"));
        state.seatByUser().put("42", 1);
        state.seatByUser().put("43", 2);
        state.invitedSeats().put("42", 3);
        state.observers().add("42");
        state.joinedHumanPlayerIds().add(1);
        assertThat(state.releaseAccount("42")).isTrue();
        assertThat(state.seatByUser()).containsOnlyKeys("43");
        assertThat(state.invitedSeats()).doesNotContainKey("42");
        assertThat(state.observers()).doesNotContain("42");
        assertThat(state.joinedHumanPlayerIds()).doesNotContain(1);
        assertThat(state.players().getFirst().ai()).isTrue();
        assertThat(state.releaseAccount("42")).isFalse();
    }
}
