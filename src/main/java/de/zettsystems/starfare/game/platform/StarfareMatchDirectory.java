package de.zettsystems.starfare.game.platform;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.MatchDirectory;
import de.zettsystems.gamesplatformapi.MatchSummary;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.GameListScope;
import de.zettsystems.starfare.game.values.GameSummary;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Starfare's matches for the platform. Both queries start from Starfare's own lobby list, so
 * the platform sees exactly what Starfare's lobby shows the account: games of a variant in
 * testing only for its testers, finished games not at all.
 */
@Component
class StarfareMatchDirectory implements MatchDirectory {

    private static final Comparator<MatchSummary> MOST_RECENT_FIRST =
            Comparator.comparing(MatchSummary::lastChange).reversed();

    private final GameService games;

    StarfareMatchDirectory(GameService games) {
        this.games = games;
    }

    @Override
    public String gameKey() {
        return StarfareMatches.GAME_KEY;
    }

    @Override
    public List<MatchSummary> matchesOf(AccountId account) {
        String name = StarfareMatches.accountName(account);
        return lobbyOf(name, game -> takesPart(game, name));
    }

    @Override
    public List<MatchSummary> openMatches(AccountId viewer) {
        String name = StarfareMatches.accountName(viewer);
        return lobbyOf(name, game -> !game.started() && !takesPart(game, name) && game.canJoin(name))
                .stream().filter(MatchSummary::isJoinable).toList();
    }

    /** Seated, or the host who organises a game without a seat of their own (M12.1). */
    private static boolean takesPart(GameSummary game, String account) {
        return game.seatByPlayer().containsKey(account) || account.equals(game.hostPlayerId());
    }

    private List<MatchSummary> lobbyOf(String account, Predicate<GameSummary> filter) {
        return games.visibleGamesFor(account, GameListScope.LOBBY).stream().filter(filter)
                .map(StarfareMatches::summary).flatMap(Optional::stream)
                .sorted(MOST_RECENT_FIRST).toList();
    }
}
