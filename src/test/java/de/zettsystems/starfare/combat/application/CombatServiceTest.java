package de.zettsystems.starfare.combat.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.report.application.ReportService;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CombatServiceTest {

    @Test
    void battleReportsUseEmpireNames() {
        GameState state = new GameState();
        state.players().addAll(List.of(new Player(1, "Account One", false, "#fff", "First Empire"),
                new Player(2, "Account Two", false, "#aaa", "Second Empire")));
        state.systems().add(new StarSystem(1, "S1", 0, 0, 2, 1, 2, false));
        state.intel().put(1, new HashMap<>());
        state.intel().put(2, new HashMap<>());
        new DefaultCombatService(new DefaultReportService()).resolveAttack(state, 1, 1, 100, List.of(1));
        assertThat(state.reports().get(1).events()).anySatisfy(event -> {
            assertThat(event).isInstanceOf(de.zettsystems.starfare.report.values.TurnEvent.BattleWon.class);
            if (event instanceof de.zettsystems.starfare.report.values.TurnEvent.BattleWon battle) {
                assertThat(battle.attackerName()).isEqualTo("First Empire");
                assertThat(battle.defenderName()).isEqualTo("Second Empire");
            }
        });
    }

    @Test
    void resolveAttackReinforcesOwnedSystem() {
        GameState state = new GameState();
        state.players().add(new Player(1, "P1", false, "#fff"));
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 5, 2, false));
        state.intel().put(1, new HashMap<>());

        ReportService reportService = new DefaultReportService();
        CombatService service = new DefaultCombatService(reportService);

        service.resolveAttack(state, 1, 1, 3, List.of(1));

        assertThat(state.getSystem(1).garrison()).isEqualTo(8);
        assertThat(state.reports().get(1).lines()).hasSize(1);
    }
}
