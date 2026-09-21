package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.GameId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Ends rounds whose round or straggler limit has passed. */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring-injected GameService is kept by reference for the bean's lifetime by design.")
public class RoundDeadlineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(RoundDeadlineRunner.class);

    private final GameService gameService;

    public RoundDeadlineRunner(GameService gameService) {
        this.gameService = gameService;
    }

    // Kurzes Intervall: das Nachzuegler-Limit kann bei 30 s liegen.
    @Scheduled(fixedDelayString = "${starfare.game.round-check-interval:5s}")
    public void enforceRoundDeadlines() {
        Instant now = Instant.now();
        for (GameId gameId : gameService.loadedGames()) {
            try {
                gameService.enforceRoundDeadline(gameId, now);
            } catch (RuntimeException e) {
                // Eine zwischenzeitlich geloeschte oder defekte Partie darf die
                // uebrigen dieses Durchlaufs nicht ueberspringen.
                LOG.warn("Round deadline check failed for game {}", gameId, e);
            }
        }
        gameService.unloadInactiveSingleHumanGames();
    }
}
