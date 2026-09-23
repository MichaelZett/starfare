package de.zettsystems.starfare.game.values;

import java.time.Instant;
import java.util.List;

/** One completed game shown in a player's statistics history. */
public record CompletedGameStatistics(String gameName, Instant finishedAt, CompletedGameOutcome outcome,
                                      List<String> opponentIds, List<String> aiOpponentNames) {
    public CompletedGameStatistics {
        opponentIds = List.copyOf(opponentIds);
        aiOpponentNames = List.copyOf(aiOpponentNames);
    }

    public CompletedGameStatistics(String gameName, Instant finishedAt, CompletedGameOutcome outcome,
                                   List<String> opponentIds) {
        this(gameName, finishedAt, outcome, opponentIds, List.of());
    }
}
