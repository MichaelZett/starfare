package de.zettsystems.starfare.combat.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultCombatServiceTest {

    private DefaultCombatService service;
    private GameState state;

    @BeforeEach
    void setUp() {
        service = new DefaultCombatService(new DefaultReportService());
        state = new GameState();
        state.players().add(new Player(1, "P1", false, "#aaaaaa"));
        state.players().add(new Player(2, "P2", false, "#bbbbbb"));
        state.systems().add(new StarSystem(1, "S1", 0, 0, 1, 5, 2, false));         // owned by P1
        state.systems().add(new StarSystem(2, "S2", 100, 0, 2, 100, 2, false));     // owned by P2 (heavy)
        state.systems().add(new StarSystem(3, "S3", 200, 0, null, 1, 1, true));     // neutral, weak
        state.intel().put(1, new HashMap<>());
        state.intel().put(2, new HashMap<>());
    }

    private TurnReport reportOf(int playerId) {
        TurnReport report = state.reports().get(playerId);
        assertThat(report != null).as("no report for player " + playerId).isTrue();
        return report;
    }

    @Test
    void arrivalAtOwnSystemReinforces() {
        service.resolveAttack(state, 1, 1, 4, List.of(7));

        assertThat(state.getSystem(1).garrison()).isEqualTo(9);
        TurnEvent event = reportOf(1).events().getFirst();
        assertThat(event).isInstanceOf(TurnEvent.Reinforcement.class);
        TurnEvent.Reinforcement r = (TurnEvent.Reinforcement) event;
        assertThat(r.ships()).isEqualTo(4);
        assertThat(r.totalGarrison()).isEqualTo(9);
        assertThat(r.fleetLabel()).isEqualTo("F7");
    }

    @Test
    void overwhelmingAttackOnNeutralCapturesItAndFlipsNeutralFlag() {
        // attacker 50 vs neutral garrison 1 → deterministic win regardless of randomness
        service.resolveAttack(state, 1, 3, 50, List.of(1));

        StarSystem captured = state.getSystem(3);
        assertThat(captured.ownerId()).isOne();
        assertThat(captured.neutral()).as("captured systems are no longer neutral").isFalse();
        assertThat(captured.garrison() > 0).as("attacker survivors form the new garrison").isTrue();

        TurnEvent.BattleWon win = (TurnEvent.BattleWon) reportOf(1).events().getFirst();
        assertThat(win.wasNeutral()).as("BattleWon must record that the target was neutral").isTrue();
        assertThat(win.remaining()).isEqualTo(captured.garrison());
        // attacker now sees the system as their own in their intel map
        assertThat(state.intel().get(1).get(3).ownerId()).isOne();
    }

    @Test
    void overwhelmingAttackOnEnemyAlsoNotifiesOldOwnerWithSystemLost() {
        // P1 sends 200 ships against P2's heavy garrison of 100 → still a deterministic win
        service.resolveAttack(state, 1, 2, 200, List.of(3, 4));

        StarSystem captured = state.getSystem(2);
        assertThat(captured.ownerId()).isOne();

        // attacker gets BattleWon
        TurnEvent.BattleWon win = (TurnEvent.BattleWon) reportOf(1).events().getFirst();
        assertThat(win.systemId()).isEqualTo(2);
        assertThat(win.wasNeutral()).isFalse();

        // old owner gets SystemLost in their report
        TurnEvent.SystemLost lost = (TurnEvent.SystemLost) reportOf(2).events().getFirst();
        assertThat(lost.systemId()).isEqualTo(2);
        assertThat(lost.attackerId()).isOne();

        // both intel maps now point at the new owner
        assertThat(state.intel().get(1).get(2).ownerId()).isOne();
        assertThat(state.intel().get(2).get(2).ownerId()).isOne();
    }

    @Test
    void hopelessAttackLeavesOwnerIntactAndEmitsBothBattleLostAndDefenseHeld() {
        // P1 sends 1 ship against P2's 100 → defender wins reliably
        service.resolveAttack(state, 1, 2, 1, List.of(9));

        StarSystem target = state.getSystem(2);
        assertThat(target.ownerId()).as("owner must not flip").isEqualTo(2);
        assertThat(target.garrison() > 0).as("defenders survive").isTrue();

        TurnEvent.BattleLost lost = (TurnEvent.BattleLost) reportOf(1).events().getFirst();
        assertThat(lost.systemId()).isEqualTo(2);
        TurnEvent.DefenseHeld held = (TurnEvent.DefenseHeld) reportOf(2).events().getFirst();
        assertThat(held.systemId()).isEqualTo(2);
        assertThat(held.attacking()).isOne();
    }

    @Test
    void attackerScoutIntelIsRecordedEvenOnDefeat() {
        service.resolveAttack(state, 1, 2, 1, List.of(11));

        // attacker's intel for the target reflects what they "saw" before the fight
        GameState.Intel attackerIntel = state.intel().get(1).get(2);
        assertThat(attackerIntel.ownerId()).isEqualTo(2);
        assertThat(attackerIntel.turn()).isEqualTo(state.turn());
        // defender intel about themselves is not changed by a failed scout
        assertThat(state.intel().get(2).get(2)).isNull();
    }
}
