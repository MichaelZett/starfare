package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.GameId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically lets the server take over abandoned real-time seats. */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring-injected GameService is kept by reference for the bean's lifetime by design.")
public class InactivityTimeoutRunner {
    private static final Logger LOG = LoggerFactory.getLogger(InactivityTimeoutRunner.class);

    private final GameService gameService;

    public InactivityTimeoutRunner(GameService gameService) {
        this.gameService = gameService;
    }

    @Scheduled(fixedDelayString = "${starfare.game.inactivity-check-interval:30s}")
    public void expireInactiveSeats() {
        for (GameId gameId : gameService.listGames()) {
            try {
                gameService.expireInactiveSeats(gameId);
            } catch (RuntimeException e) {
                // Eine zwischenzeitlich geloeschte oder defekte Partie darf die
                // uebrigen dieses Durchlaufs nicht ueberspringen.
                LOG.warn("Inactivity check failed for game {}", gameId, e);
            }
        }
    }
}
