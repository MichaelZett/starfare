package de.zettsystems.starfare.combat.application;

import de.zettsystems.starfare.diplomacy.values.AllianceGroup;
import de.zettsystems.starfare.diplomacy.values.DiplomacyState;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.navigation.application.DefaultNavigationService;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.report.values.*;
import de.zettsystems.starfare.testsupport.NavigationStates;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CoalitionCombatServiceTest {
    private final DefaultCoalitionCombatService combat = new DefaultCoalitionCombatService(new DefaultReportService(), new DefaultNavigationService(), _ -> .5);
    private static GameState state() {
        var state = NavigationStates.running(); state.configureLobby(true, true, true, 0);
        state.players().add(new Player(3, "Third", false, "#fff")); state.players().add(new Player(4, "Fourth", false, "#aaa"));
        state.intel().put(3, new HashMap<>()); state.intel().put(4, new HashMap<>());
        state.updateSystem(4, system -> system.captureBy(2, 60)); return state;
    }
    private static void allies(GameState state, int first, int second) {
        state.agreeTreaties(new DiplomacyState(2, List.of(new AllianceGroup(1, Set.of(first, second), 3, Map.of())), List.of()));
    }
    private static Fleet incoming(GameState state, int id, int owner, int ships) {
        var fleet = new Fleet(id, owner, id, 3, 4, ships, state.turn(), state.turn() + 1);
        state.fleets().add(fleet); return fleet;
    }
    private static TurnEvent.CoalitionBattle battle(GameState state, int player) {
        return (TurnEvent.CoalitionBattle) state.reports().get(player).events().getLast();
    }
    @Test void alliedDefenseIncludesParkedSupportAndPreservesOwners() {
        var state = state(); allies(state, 1, 2);
        var support = incoming(state, 1, 1, 40); state.replaceFleet(support.dockAtPartner());
        incoming(state, 2, 3, 60); combat.resolveArrivals(state);
        assertThat(state.getSystem(4).ownerId()).isEqualTo(2);
        assertThat(state.getSystem(4).garrison()).isEqualTo(24);
        assertThat(state.fleets()).singleElement().satisfies(fleet -> {
            assertThat(fleet.ownerId()).isEqualTo(1); assertThat(fleet.ships()).isEqualTo(16); assertThat(fleet.inFlight()).isFalse();
        });
        assertThat(battle(state, 1)).isEqualTo(battle(state, 2)).isEqualTo(battle(state, 3));
        assertThat(battle(state, 1).lostBy(1)).isEqualTo(24); assertThat(battle(state, 2).lostBy(2)).isEqualTo(36);
    }
    @Test void arrivingAlliesFightTogetherAndConquestKeepsSupportSeparate() {
        var state = state(); allies(state, 1, 3); incoming(state, 1, 1, 60); incoming(state, 2, 3, 40);
        combat.resolveArrivals(state);
        assertThat(state.getSystem(4).ownerId()).isEqualTo(1); assertThat(state.getSystem(4).garrison()).isEqualTo(24);
        assertThat(state.fleets()).singleElement().satisfies(fleet -> {
            assertThat(fleet.ownerId()).isEqualTo(3); assertThat(fleet.ships()).isEqualTo(16); assertThat(fleet.journey().stationed()).isTrue();
        });
        assertThat(state.reports().get(1).events()).hasSize(1); // No post-battle reinforcement can disclose the result before playback.
    }
    @Test void explicitAbsentBeneficiaryReceivesSystemButNoTransferredShips() {
        var state = state(); allies(state, 1, 3);
        var fleet = incoming(state, 1, 1, 100); state.replaceFleet(fleet.designateConquest(3)); combat.resolveArrivals(state);
        assertThat(state.getSystem(4).ownerId()).isEqualTo(3); assertThat(state.getSystem(4).garrison()).isZero();
        assertThat(state.fleets()).singleElement().satisfies(survivor -> { assertThat(survivor.ownerId()).isEqualTo(1); assertThat(survivor.ships()).isEqualTo(40); });
        assertThat(battle(state, 1).ownerName()).isEqualTo("Third");
    }
    @Test void conflictingClaimsUseSurvivingImperialStrengthThenStablePlayerId() {
        var state = state(); allies(state, 1, 3); incoming(state, 9, 3, 50); incoming(state, 2, 1, 50);
        combat.resolveArrivals(state); assertThat(state.getSystem(4).ownerId()).isEqualTo(1);
        var stronger = state(); allies(stronger, 1, 3); incoming(stronger, 1, 1, 40); incoming(stronger, 2, 3, 60);
        combat.resolveArrivals(stronger); assertThat(stronger.getSystem(4).ownerId()).isEqualTo(3);
    }
    @Test void annihilationRetainsOwnerAndNeutralDefenseRemainsNeutral() {
        var state = state(); incoming(state, 1, 1, 60); combat.resolveArrivals(state);
        assertThat(state.getSystem(4).ownerId()).isEqualTo(2); assertThat(state.getSystem(4).garrison()).isZero(); assertThat(state.fleets()).isEmpty();
        var neutral = state(); neutral.updateSystem(4, system -> new StarSystem(4, "Delta", 400, 100, null, 60, 4, true));
        incoming(neutral, 1, 1, 20); combat.resolveArrivals(neutral);
        assertThat(neutral.getSystem(4).ownerId()).isNull(); assertThat(neutral.getSystem(4).garrison()).isEqualTo(40);
    }
    @Test void threeHostileEmpiresResolveOneBattleAndTwoCoalitionsResolveAsTwoSides() {
        var state = state(); incoming(state, 1, 1, 20); incoming(state, 2, 3, 20); combat.resolveArrivals(state);
        assertThat(battle(state, 1).sides()).hasSize(3);
        assertThat(state.getSystem(4).ownerId()).isEqualTo(2);
        var paired = state(); paired.agreeTreaties(new DiplomacyState(3, List.of(
                new AllianceGroup(1, Set.of(1, 3), 3, Map.of()), new AllianceGroup(2, Set.of(2, 4), 3, Map.of())), List.of()));
        incoming(paired, 1, 1, 60); incoming(paired, 2, 3, 40); incoming(paired, 3, 4, 40); combat.resolveArrivals(paired);
        assertThat(battle(paired, 1).sides()).hasSize(2); assertThat(paired.getSystem(4).ownerId()).isEqualTo(2);
        assertThat(paired.getSystem(4).garrison()).isZero(); assertThat(paired.fleets()).isEmpty();
    }
    @Test void evacuatedFormerPartnerNeitherAttacksNorDefends() {
        var state = state(); var leaving = incoming(state, 1, 1, 100); state.replaceFleet(leaving.dockAtPartner().requireReturn());
        incoming(state, 2, 3, 100); combat.resolveArrivals(state);
        assertThat(state.getSystem(4).ownerId()).isEqualTo(3); assertThat(state.getSystem(4).garrison()).isEqualTo(40);
        assertThat(state.fleets()).singleElement().satisfies(fleet -> { assertThat(fleet.evacuating()).isTrue(); assertThat(fleet.ships()).isEqualTo(100); });
        assertThat(state.reports()).doesNotContainKey(1);
    }
    @Test void ownReinforcementsJoinDefenseAndExpiredClaimFallsBackToOwner() {
        var state = state(); incoming(state, 1, 2, 40); incoming(state, 2, 1, 60); combat.resolveArrivals(state);
        assertThat(state.getSystem(4).garrison()).isEqualTo(40); assertThat(state.getSystem(4).ownerId()).isEqualTo(2); assertThat(state.fleets()).isEmpty();
        var expired = state(); var fleet = incoming(expired, 1, 1, 100); expired.replaceFleet(fleet.designateConquest(3)); combat.resolveArrivals(expired);
        assertThat(expired.getSystem(4).ownerId()).isEqualTo(1);
    }
    @Test void planningValidatesClaimAndUndoPreservesItsValue() {
        var state = state(); allies(state, 1, 3); var fleets = new DefaultFleetService();
        assertThat(fleets.queueSend(state, 1, 3, 4, 3, 2)).isFalse();
        assertThat(fleets.queueSend(state, 1, 3, 4, 3, 3)).isTrue();
        assertThat(state.pendingOrders().get(1).getFirst()).isEqualTo(new de.zettsystems.starfare.fleet.values.FleetOrder.Send(1, 3, 4, 3, 3));
        assertThat(state.undoOrders(1)).isTrue(); assertThat(state.pendingOrders().get(1)).isEmpty();
    }
}
