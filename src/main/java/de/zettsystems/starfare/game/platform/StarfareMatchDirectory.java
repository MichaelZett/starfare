package de.zettsystems.starfare.game.platform;

import de.zettsystems.gamesplatformapi.AccountId;
import de.zettsystems.gamesplatformapi.FinishedMatch;
import de.zettsystems.gamesplatformapi.MatchDirectory;
import de.zettsystems.gamesplatformapi.MatchInvitation;
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
 * Starfare's matches for the platform. The queries for open and running matches start from Starfare's
 * own lobby list, so the platform sees exactly what Starfare's lobby shows the account: games of a
 * variant in testing only for its testers. Finished games come from the archive, only the account's own.
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

    /** Games with a seat reserved for the account that has not started, from the host who invited (plan M13). */
    @Override
    public List<MatchInvitation> invitationsOf(AccountId account) {
        String name = StarfareMatches.accountName(account);
        return games.visibleGamesFor(name, GameListScope.LOBBY).stream()
                .filter(game -> !game.started() && game.invitedSeats().containsKey(name))
                .flatMap(game -> Optional.ofNullable(game.hostPlayerId()).flatMap(StarfareMatches::account)
                        .flatMap(host -> StarfareMatches.summary(game).map(match -> new MatchInvitation(match, host)))
                        .stream())
                .sorted(Comparator.comparing((MatchInvitation invitation) -> invitation.match().lastChange()).reversed())
                .toList();
    }

    /** One page of {@link GameService#finishedMatchesOf(String)}: the account's finished games, latest first. */
    @Override
    public List<FinishedMatch> finishedMatchesOf(AccountId account, int offset, int limit) {
        if (offset < 0 || limit < 1) {
            throw new IllegalArgumentException("offset must be at least 0 and limit at least 1, got " + offset + " and " + limit);
        }
        return games.finishedMatchesOf(StarfareMatches.accountName(account)).stream().skip(offset).limit(limit).toList();
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
