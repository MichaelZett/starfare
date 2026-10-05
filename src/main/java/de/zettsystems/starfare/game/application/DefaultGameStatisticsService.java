package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameResultEntity;
import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.values.CompletedGameOutcome;
import de.zettsystems.starfare.game.values.CompletedGameStatistics;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.OpponentStatistics;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.PlayerStatistics;
import de.zettsystems.starfare.game.values.RulesetRef;
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
                    .filter(entry -> state.outcome().wonBy(entry.getValue()))
                    .map(Map.Entry::getKey).findFirst().orElse(null);
            var winningAccounts = state.seatByUser().entrySet().stream().filter(entry -> state.outcome().wonBy(entry.getValue()))
                    .map(Map.Entry::getKey).collect(java.util.stream.Collectors.toSet());
            boolean aiVictory = state.players().stream()
                    .anyMatch(player -> player.ai() && state.outcome().wonBy(player.id()));
            return new FinishedGame(session.name(), state.outcome().allianceVictory() ? null : winner, aiVictory, state.finishedAt(), accounts, aiOpponentNames, state.ruleset(), winningAccounts);
        });
        if (game != null && !game.participants().isEmpty()) {
            var result = new GameResultEntity(gameId.value(), game.name(), game.winner(),
                    game.finishedAt() != null ? game.finishedAt() : Instant.now(), game.participants(), game.aiOpponentNames());
            if (game.aiVictory()) {
                result.recordAiVictory();
            }
            result.recordRuleset(game.ruleset());
            result.recordWinners(game.winners());
            results.save(result);
        }
    }

    @Override
    @Transactional
    public PlayerStatistics statisticsFor(String account) {
        return buildStatistics(account, null);
    }

    @Override
    @Transactional
    public PlayerStatistics statisticsFor(String account, @Nullable String variant) {
        return buildStatistics(account, variant);
    }

    private PlayerStatistics buildStatistics(String account, @Nullable String variant) {
        if (account.isBlank()) {
            return PlayerStatistics.empty();
        }
        Map<String, Totals> opponents = new HashMap<>();
        var completedGames = new ArrayList<CompletedGameStatistics>();
        int wins = 0;
        int losses = 0;
        var games = variant == null ? results.findForParticipant(account)
                : results.findForParticipantAndVariant(account, variant);
        if (!games.isEmpty()) {
            results.findWithAiOpponentsByGameIdIn(games.stream().map(GameResultEntity::getId).toList());
            results.findWithWinnersByGameIdIn(games.stream().map(GameResultEntity::getId).toList());
        }
        for (GameResultEntity game : games) {
            boolean won = game.wonBy(account);
            if (won) {
                wins++;
            } else if (game.hasWinner()) {
                losses++;
            }
            completedGames.add(new CompletedGameStatistics(
                    game.getGameName(),
                    game.getFinishedAt(),
                    outcomeFor(game, won),
                    game.getParticipants().stream().filter(opponent -> !opponent.equals(account)).sorted().toList(),
                    game.getAiOpponentNames().stream().sorted().toList(), game.getRuleset()));
            for (String opponent : game.getParticipants()) {
                if (!opponent.equals(account)) {
                    opponents.computeIfAbsent(opponent, _ -> new Totals()).add(won, !won && game.hasWinner());
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
        return game.hasWinner() ? CompletedGameOutcome.LOSS : CompletedGameOutcome.DRAW;
    }

    private record FinishedGame(String name, @Nullable String winner, boolean aiVictory, @Nullable Instant finishedAt,
                                LinkedHashSet<String> participants, LinkedHashSet<String> aiOpponentNames,
                                RulesetRef ruleset, Set<String> winners) {
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
