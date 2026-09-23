package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Schlacht-Bestätigungen überdauern die Sitzung: Wer eine beendete Partie später
 * aus dem Archiv öffnet, soll nicht erneut durch die Auswertung geführt werden.
 */
class BattleAcknowledgementServiceTest extends AbstractIntegrationTest {

    private static final GameId GAME = GameId.of("ack-game");
    private static final GameId OTHER_GAME = GameId.of("ack-other-game");

    @Autowired
    private BattleAcknowledgementService acknowledgements;

    @Autowired
    private BattleAcknowledgementRepository repository;

    @Autowired
    private GameService games;

    @AfterEach
    void clear() {
        repository.deleteAll();
    }

    @Test
    void acknowledgementsArePersistedPerPlayerGameAndTurn() {
        acknowledgements.acknowledge("alice", GAME, 5, 0);
        acknowledgements.acknowledge("alice", GAME, 5, 2);
        acknowledgements.acknowledge("alice", GAME, 5, 2);

        assertThat(acknowledgements.acknowledgedEventIndices("alice", GAME, 5)).containsExactlyInAnyOrder(0, 2);
        assertThat(acknowledgements.acknowledgedEventIndices("bob", GAME, 5)).isEmpty();
        assertThat(acknowledgements.acknowledgedEventIndices("alice", OTHER_GAME, 5)).isEmpty();
        assertThat(acknowledgements.acknowledgedEventIndices("alice", GAME, 6)).isEmpty();
    }

    @Test
    void acknowledgingALaterTurnDropsEarlierTurnsOfThatGameOnly() {
        acknowledgements.acknowledge("alice", GAME, 5, 0);
        acknowledgements.acknowledge("alice", OTHER_GAME, 5, 0);
        acknowledgements.acknowledge("bob", GAME, 5, 0);

        acknowledgements.acknowledge("alice", GAME, 6, 1);

        assertThat(acknowledgements.acknowledgedEventIndices("alice", GAME, 5)).isEmpty();
        assertThat(acknowledgements.acknowledgedEventIndices("alice", GAME, 6)).containsExactly(1);
        assertThat(acknowledgements.acknowledgedEventIndices("alice", OTHER_GAME, 5)).containsExactly(0);
        assertThat(acknowledgements.acknowledgedEventIndices("bob", GAME, 5)).containsExactly(0);
    }

    @Test
    void abortingAGameForgetsItsAcknowledgements() {
        GameId id = games.newGame(GameSetup.defaults(), "host", "Aborted");
        acknowledgements.acknowledge("host", id, 1, 0);
        acknowledgements.acknowledge("host", GAME, 1, 0);

        games.abortGame(id);

        assertThat(acknowledgements.acknowledgedEventIndices("host", id, 1)).isEmpty();
        assertThat(acknowledgements.acknowledgedEventIndices("host", GAME, 1)).containsExactly(0);
    }
}
