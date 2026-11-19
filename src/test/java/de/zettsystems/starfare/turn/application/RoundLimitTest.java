package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.ai.application.DefaultAiService;
import de.zettsystems.starfare.combat.application.DefaultCombatService;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.domain.SectorScore;
import de.zettsystems.starfare.game.values.EventFrequency;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.SectorOptions;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.report.values.CosmicNews;
import de.zettsystems.starfare.report.values.TurnEvent;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoundLimitTest {
    private final DefaultReportService reports = new DefaultReportService();
    private final TurnEngine engine = new DefaultTurnEngine(new DefaultCombatService(reports), new DefaultAiService(),
            reports, new DefaultFleetService());

    private static GameState galaxy(int roundLimit) {
        GameState state = new GameState();
        state.rememberSetup(GameSetup.defaults().withSectorOptions(
                new SectorOptions(EventFrequency.OFF, null, false, roundLimit)));
        for (int id = 1; id <= 2; id++) {
            state.players().add(new Player(id, "P" + id, false, "#ffffff"));
            state.intel().put(id, new HashMap<>());
        }
        state.systems().addAll(List.of(
                new StarSystem(1, "A", 0, 0, 1, 10, 2, false),
                new StarSystem(2, "B", 100, 0, 1, 5, 1, false),
                new StarSystem(3, "C", 900, 0, 2, 30, 3, false),
                new StarSystem(4, "N1", 400, 300, null, 5, 2, true),
                new StarSystem(5, "N2", 500, 300, null, 5, 2, true)));
        state.start();
        return state;
    }

    private static List<TurnEvent> events(GameState state, int player) {
        var report = state.reports().get(player);
        return report == null ? List.of() : report.events();
    }

    @Test
    void warnsDuringTheLastFiveRoundsAndEndsWithTheBestScore() {
        GameState state = galaxy(SectorOptions.MIN_ROUND_LIMIT);
        int resolved = 0;
        while (!state.gameOver()) {
            resolved = state.turn();
            engine.advanceTurn(state);
            boolean warned = events(state, 2).stream().anyMatch(event -> event instanceof TurnEvent.Cosmic news
                    && news.news() == CosmicNews.ROUND_LIMIT_NEAR && news.round() == SectorOptions.MIN_ROUND_LIMIT);
            assertThat(warned).as("warning after round %d", resolved)
                    .isEqualTo(resolved >= SectorOptions.MIN_ROUND_LIMIT - SectorOptions.ROUND_LIMIT_WARNING
                            && resolved < SectorOptions.MIN_ROUND_LIMIT);
        }

        assertThat(resolved).isEqualTo(SectorOptions.MIN_ROUND_LIMIT);
        assertThat(state.roundLimitReached()).isTrue();
        TurnEvent.FinalScore score = events(state, 2).stream().filter(TurnEvent.FinalScore.class::isInstance)
                .map(TurnEvent.FinalScore.class::cast).findFirst().orElseThrow();
        assertThat(score.round()).isEqualTo(SectorOptions.MIN_ROUND_LIMIT);
        assertThat(score.lines()).extracting(TurnEvent.ScoreLine::playerId).containsExactly(1, 2);
        TurnEvent.ScoreLine best = score.lines().getFirst();
        assertThat(best.systems()).isEqualTo(2);
        assertThat(best.points()).isEqualTo(2 * SectorScore.POINTS_PER_SYSTEM
                + best.stationed() * SectorScore.POINTS_PER_STATIONED_SHIP + best.inFlight() * SectorScore.POINTS_PER_SHIP_IN_FLIGHT);
        assertThat(state.winnerId()).isOne();
        assertThat(events(state, 1)).contains(new TurnEvent.Victory(1, TurnEvent.Victory.BY_SCORE));
        assertThat(events(state, 2)).anyMatch(TurnEvent.Defeat.class::isInstance);
    }

    @Test
    void shipsDecideWhenSystemsAreEven() {
        GameState state = galaxy(SectorOptions.MIN_ROUND_LIMIT);
        state.updateSystem(2, StarSystem::explode);
        state.updateSystem(1, system -> system.loseShips(10));

        List<TurnEvent.ScoreLine> lines = RoundLimit.score(state);

        assertThat(lines).extracting(TurnEvent.ScoreLine::playerId).containsExactly(2, 1);
        assertThat(lines.getFirst().points()).isEqualTo(400 + 30 * 25);
    }

    @Test
    void gamesWithoutLimitNeverEndByScore() {
        GameState state = galaxy(0);
        for (int round = 0; round < 30; round++) {
            engine.advanceTurn(state);
        }

        assertThat(state.gameOver()).isFalse();
        assertThat(state.roundLimitReached()).isFalse();
    }
}
