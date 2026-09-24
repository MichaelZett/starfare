package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameResultEntity;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.values.CompletedGameOutcome;
import de.zettsystems.starfare.game.values.CompletedGameStatistics;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.OpponentStatistics;
import de.zettsystems.starfare.game.values.PlayerStatistics;
import de.zettsystems.starfare.game.values.Player;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
class DefaultGameStatisticsService implements GameStatisticsService {
    private final GameRegistry registry;
    private final GameResultRepository results;

    DefaultGameStatisticsService(GameRegistry registry, GameResultRepository results) {
        this.registry = registry;
        this.results = results;
    }

    @Override
    @Transactional
    public void recordFinishedGame(GameId gameId) {
        if (results.existsById(gameId.value())) {
            return;
        }
        GameSession session = registry.find(gameId).orElse(null);
        if (session == null) {
            return;
        }
        FinishedGame game = session.readState(state -> {
            if (!state.gameOver()) {
                return null;
            }
            var accounts = new LinkedHashSet<>(state.seatByUser().keySet());
            var aiOpponentNames = state.players().stream().filter(Player::ai).map(Player::label)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            String winner = state.seatByUser().entrySet().stream()
                    .filter(entry -> Objects.equals(entry.getValue(), state.winnerId()))
                    .map(Map.Entry::getKey).findFirst().orElse(null);
            return new FinishedGame(session.name(), winner, state.finishedAt(), accounts, aiOpponentNames);
        });
        if (game != null && !game.participants().isEmpty()) {
            results.save(new GameResultEntity(gameId.value(), game.name(), game.winner(),
                    game.finishedAt() != null ? game.finishedAt() : Instant.now(), game.participants(), game.aiOpponentNames()));
        }
    }

    @Override
    @Transactional
    public PlayerStatistics statisticsFor(String account) {
        if (account.isBlank()) {
            return PlayerStatistics.empty();
        }
        Map<String, Totals> opponents = new HashMap<>();
        var completedGames = new ArrayList<CompletedGameStatistics>();
        int wins = 0;
        int losses = 0;
        var games = results.findForParticipant(account);
        if (!games.isEmpty()) {
            results.findWithAiOpponentsByGameIdIn(games.stream().map(GameResultEntity::getId).toList());
        }
        for (GameResultEntity game : games) {
            boolean won = account.equals(game.getWinnerPlayerId());
            if (won) {
                wins++;
            } else if (game.getWinnerPlayerId() != null) {
                losses++;
            }
            completedGames.add(new CompletedGameStatistics(
                    game.getGameName(),
                    game.getFinishedAt(),
                    outcomeFor(game, won),
                    game.getParticipants().stream().filter(opponent -> !opponent.equals(account)).sorted().toList(),
                    game.getAiOpponentNames().stream().sorted().toList()));
            for (String opponent : game.getParticipants()) {
                if (!opponent.equals(account)) {
                    opponents.computeIfAbsent(opponent, _ -> new Totals()).add(won, !won && game.getWinnerPlayerId() != null);
                }
            }
        }
        var rows = opponents.entrySet().stream()
                .map(entry -> new OpponentStatistics(entry.getKey(), entry.getValue().games, entry.getValue().wins, entry.getValue().losses))
                .sorted(Comparator.comparingInt(OpponentStatistics::games).reversed()
                        .thenComparing(OpponentStatistics::opponentId))
                .toList();
        completedGames.sort(Comparator.comparing(CompletedGameStatistics::finishedAt).reversed());
        return new PlayerStatistics(games.size(), wins, losses, rows, completedGames);
    }

    private static CompletedGameOutcome outcomeFor(GameResultEntity game, boolean won) {
        if (won) {
            return CompletedGameOutcome.WIN;
        }
        return game.getWinnerPlayerId() == null ? CompletedGameOutcome.DRAW : CompletedGameOutcome.LOSS;
    }

    private record FinishedGame(String name, @Nullable String winner, @Nullable Instant finishedAt,
                                LinkedHashSet<String> participants, LinkedHashSet<String> aiOpponentNames) {
    }

    private static final class Totals {
        private int games;
        private int wins;
        private int losses;

        private void add(boolean won, boolean lost) {
            games++;
            if (won) {
                wins++;
            }
            if (lost) {
                losses++;
            }
        }
    }
}
