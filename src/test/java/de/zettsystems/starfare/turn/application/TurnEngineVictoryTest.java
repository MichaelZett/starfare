package de.zettsystems.starfare.turn.application;

import de.zettsystems.starfare.ai.application.DefaultAiService;
import de.zettsystems.starfare.combat.application.DefaultCombatService;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.report.application.ReportService;
import de.zettsystems.starfare.report.values.TurnEvent;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class TurnEngineVictoryTest {

    /** @param ownedByP1 wie viele der vier Systeme P1 gehoeren; der Rest geht an P2. */
    private static GameState fourSystemsWith(int ownedByP1) {
        GameState state = new GameState();
        state.players().add(new Player(1, "P1", false, "#fff"));
        state.players().add(new Player(2, "P2", false, "#000"));
        for (int i = 1; i <= 4; i++) {
            state.systems().add(new StarSystem(i, "S" + i, (i - 1) * 50, 0, i <= ownedByP1 ? 1 : 2, 5, 0, false));
        }
        state.intel().put(1, new HashMap<>());
        state.intel().put(2, new HashMap<>());
        return state;
    }

    private static TurnEngine newEngine() {
        ReportService reportService = new DefaultReportService();
        return new DefaultTurnEngine(new DefaultCombatService(reportService), new DefaultAiService(),
                reportService, new DefaultFleetService());
    }

    @Test
    void victoryReportWhenReachingTheThreshold() {
        GameState state = fourSystemsWith(3); // 75 % >= 70 %

        newEngine().advanceTurn(state);

        assertThat(state.gameOver()).isTrue();
        assertThat(state.winnerId()).isOne();
        assertThat(state.reports().get(1).lines().stream().anyMatch(l -> l.contains("Sieg"))).isTrue();
    }

    @Test
    void defeatReportReachesEveryOtherPlayer() {
        GameState state = fourSystemsWith(3);

        newEngine().advanceTurn(state);

        assertThat(state.reports().get(2).events())
                .anyMatch(event -> event instanceof TurnEvent.Defeat defeat
                        && defeat.winnerId() == 1 && "P1".equals(defeat.winnerName()));
    }

    @Test
    void plainMajorityIsNotEnough() {
        GameState state = fourSystemsWith(2); // 50 % — frueher ein Sieg, jetzt nicht mehr

        newEngine().advanceTurn(state);

        assertThat(state.gameOver()).isFalse();
        assertThat(state.winnerId()).isNull();
    }

    @Test
    void twoThirdsAreStillShortOfTheThreshold() {
        GameState state = new GameState();
        state.players().add(new Player(1, "P1", false, "#fff"));
        state.players().add(new Player(2, "P2", false, "#000"));
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 5, 0, false));
        state.systems().add(new StarSystem(2, "S2", 50, 0, 1, 5, 0, false));
        state.systems().add(new StarSystem(3, "S3", 100, 0, 2, 5, 0, false));
        state.intel().put(1, new HashMap<>());
        state.intel().put(2, new HashMap<>());

        newEngine().advanceTurn(state);

        assertThat(state.gameOver()).as("2 von 3 sind 66,7 %").isFalse();
    }

    @Test
    void neutralSystemsCountTowardsTheTotal() {
        GameState state = new GameState();
        state.players().add(new Player(1, "P1", false, "#fff"));
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 5, 0, false));
        state.systems().add(new StarSystem(2, "S2", 50, 0, 1, 5, 0, false));
        state.systems().add(new StarSystem(3, "S3", 100, 0, null, 5, 1, true));
        state.intel().put(1, new HashMap<>());

        newEngine().advanceTurn(state);

        assertThat(state.gameOver()).as("2 eigene von 3 gesamt reichen nicht").isFalse();
    }

    @Test
    void gameOverStopsFurtherTurns() {
        GameState state = new GameState();
        state.players().add(new Player(1, "P1", false, "#fff"));
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 5, 0, false));
        state.systems().add(new StarSystem(2, "S2", 50, 0, 1, 5, 0, false));
        state.intel().put(1, new HashMap<>());

        ReportService reportService = new DefaultReportService();
        TurnEngine engine = new DefaultTurnEngine(new DefaultCombatService(reportService), new DefaultAiService(), reportService, new DefaultFleetService());
        int before = state.turn();

        engine.advanceTurn(state);
        engine.advanceTurn(state);

        assertThat(state.gameOver()).isTrue();
        assertThat(state.turn()).isEqualTo(before + 1);
    }
}
