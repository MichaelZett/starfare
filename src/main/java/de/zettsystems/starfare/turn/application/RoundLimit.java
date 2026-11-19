package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.SectorScore;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.SectorOptions;
import de.zettsystems.starfare.report.application.ReportService;
import de.zettsystems.starfare.report.values.CosmicNews;
import de.zettsystems.starfare.report.values.TurnEvent;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;

/**
 * Round limit of SectorForces after Galactic Empire (1983): warns during the last rounds and, if nobody
 * reached the victory threshold, ends the game after the last round with the best {@link SectorScore}.
 */
@org.springframework.stereotype.Component
final class RoundLimit {
    private final ReportService reports;

    RoundLimit(ReportService reports) {
        this.reports = reports;
    }

    /** The winner by score if the game ended after this round, empty otherwise. */
    OptionalInt resolve(GameState state) {
        SectorOptions options = state.sectorOptions();
        if (state.gameOver() || !state.ruleset().usesSectorOptions() || !options.hasRoundLimit()) {
            return OptionalInt.empty();
        }
        int limit = options.roundLimit();
        int turn = state.turn();
        if (turn < limit) {
            if (turn >= limit - SectorOptions.ROUND_LIMIT_WARNING) {
                TurnEvent warning = new TurnEvent.Cosmic(CosmicNews.ROUND_LIMIT_NEAR, null, "", 0, 0, 0, limit, "");
                state.players().forEach(player -> reports.appendEvent(state, player.id(), warning));
            }
            return OptionalInt.empty();
        }
        List<TurnEvent.ScoreLine> lines = score(state);
        TurnEvent.FinalScore finalScore = new TurnEvent.FinalScore(turn, lines);
        state.players().forEach(player -> reports.appendEvent(state, player.id(), finalScore));
        return OptionalInt.of(lines.getFirst().playerId());
    }

    /** Best first; ties go to more systems, then to the lower seat. */
    static List<TurnEvent.ScoreLine> score(GameState state) {
        return state.players().stream().map(player -> line(state, player))
                .sorted(Comparator.comparingInt(TurnEvent.ScoreLine::points).reversed()
                        .thenComparing(Comparator.comparingInt(TurnEvent.ScoreLine::systems).reversed())
                        .thenComparingInt(TurnEvent.ScoreLine::playerId))
                .toList();
    }

    private static TurnEvent.ScoreLine line(GameState state, Player player) {
        SectorScore.Line score = SectorScore.of(state, player.id());
        return new TurnEvent.ScoreLine(player.id(), player.empireNameOrName(), score.systems(), score.stationed(),
                score.inFlight(), score.points());
    }
}
