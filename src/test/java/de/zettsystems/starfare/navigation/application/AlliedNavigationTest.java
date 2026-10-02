package de.zettsystems.starfare.navigation.application;

import de.zettsystems.starfare.combat.application.DefaultCombatService;
import de.zettsystems.starfare.diplomacy.application.DefaultDiplomacyService;
import de.zettsystems.starfare.diplomacy.values.DiplomacyOrder;
import de.zettsystems.starfare.economy.application.DefaultEconomyService;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.navigation.domain.Routes;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.testsupport.NavigationStates;
import de.zettsystems.starfare.turn.application.RoundPipeline;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AlliedNavigationTest {
    private final DefaultDiplomacyService diplomacy = new DefaultDiplomacyService();
    private final DefaultFleetService fleets = new DefaultFleetService();
    private final DefaultReportService reports = new DefaultReportService();
    private final RoundPipeline pipeline = new RoundPipeline(new DefaultCombatService(reports), reports, fleets);
    private GameState allied() {
        var state = NavigationStates.running(); state.updateSystem(2, system -> system.captureBy(2, 5));
        assertThat(diplomacy.act(state, 1, new DiplomacyOrder(DiplomacyOrder.Action.FOUND, 2, 0))).isTrue();
        assertThat(diplomacy.act(state, 2, new DiplomacyOrder(DiplomacyOrder.Action.APPROVE, state.diplomacy().proposals().getFirst().id(), 0))).isTrue();
        return state;
    }
    private void advance(GameState state) { pipeline.advanceTurn(state, () -> { }, routed -> new DefaultEconomyService().produce(state, routed)); }
    private void through(GameState state, int turn) { while (state.turn() < turn) { advance(state); } }
    @Test void alliedStationsAllowTransitButKeepShipsAndSystemsOwnedSeparately() {
        var state = allied(); assertThat(Routes.plan(state, 1, 1, 4).orElseThrow().systems()).containsExactly(1, 2, 3, 4);
        assertThat(fleets.sendFleet(state, 1, 1, 4, 10)).isTrue(); through(state, 3);
        assertThat(state.getSystem(2).ownerId()).isEqualTo(2); assertThat(state.getSystem(2).garrison()).isEqualTo(13);
        assertThat(state.fleets().getFirst().ownerId()).isEqualTo(1); assertThat(state.fleets().getFirst().ships()).isEqualTo(10);
        assertThat(fleets.queueDisband(state, 1, state.fleets().getFirst().globalId())).isFalse();
        through(state, 9); assertThat(state.getSystem(4).ownerId()).isEqualTo(2);
        assertThat(state.getSystem(4).garrison()).isEqualTo(37); assertThat(state.fleets().getFirst().journey().stationed()).isTrue();
        advance(state); assertThat(state.fleets().getFirst().ships()).isEqualTo(10);
        assertThat(state.reports().values().stream().flatMap(report -> report.events().stream())).noneMatch(event -> event instanceof de.zettsystems.starfare.report.values.TurnEvent.BattleWon);
    }
    @Test void departureAutomaticallyReturnsDockedShipsWithoutAnAttackOrForeignDisband() {
        var state = allied(); fleets.sendFleet(state, 1, 1, 4, 10); through(state, 3);
        assertThat(diplomacy.act(state, 1, new DiplomacyOrder(DiplomacyOrder.Action.LEAVE, 0, 0))).isTrue();
        advance(state); assertThat(state.allied(1, 2)).isFalse();
        var fleet = state.fleets().getFirst(); assertThat(fleet.evacuating()).isTrue(); assertThat(fleet.inFlight()).isFalse();
        assertThat(fleets.queueWait(state, 1, fleet.globalId())).isFalse();
        assertThat(fleets.setFleetWait(state, 1, fleet.globalId())).isFalse();
        assertThat(fleets.queueDisband(state, 1, fleet.globalId())).isFalse();
        through(state, 6); assertThat(state.fleets()).isEmpty(); assertThat(state.getSystem(2).ownerId()).isEqualTo(2);
        assertThat(state.getSystem(1).garrison()).isEqualTo(70);
    }
    @Test void aReturnWithoutOwnSystemsStaysBlockedAndResumesWhenAHomeBecomesReachable() {
        var state = allied(); fleets.sendFleet(state, 1, 1, 4, 10); through(state, 3);
        state.updateSystem(1, system -> system.captureBy(2, 50)); state.updateSystem(3, system -> system.captureBy(2, 5));
        diplomacy.act(state, 1, new DiplomacyOrder(DiplomacyOrder.Action.LEAVE, 0, 0)); through(state, 6);
        assertThat(state.fleets().getFirst().journey().blocked()).isTrue(); assertThat(state.fleets().getFirst().ships()).isEqualTo(10);
        state.updateSystem(1, system -> system.captureBy(1, 1)); advance(state);
        assertThat(state.fleets().getFirst().inFlight()).isTrue(); through(state, 8);
        assertThat(state.fleets()).isEmpty(); assertThat(state.getSystem(1).garrison()).isEqualTo(19);
    }
    @Test void aFlightAlreadyOnItsWayToAFormerPartnerCannotBecomeASurpriseAttack() {
        var state = allied(); fleets.sendFleet(state, 1, 1, 2, 10);
        diplomacy.act(state, 1, new DiplomacyOrder(DiplomacyOrder.Action.LEAVE, 0, 0)); through(state, 3);
        assertThat(state.getSystem(2).ownerId()).isEqualTo(2); assertThat(state.getSystem(2).garrison()).isEqualTo(13);
        assertThat(state.fleets().getFirst().evacuating()).isTrue(); through(state, 6);
        assertThat(state.fleets()).isEmpty(); assertThat(state.getSystem(1).garrison()).isEqualTo(70);
    }
    @Test void automaticReturnUsesRemainingAlliesAndOneFullRoundAtEveryStop() {
        var state = allied(); state.players().add(new de.zettsystems.starfare.game.values.Player(3, "Third", false, "#009e73"));
        int group = state.diplomacy().groups().getFirst().id();
        assertThat(diplomacy.act(state, 3, new DiplomacyOrder(DiplomacyOrder.Action.JOIN, group, 0))).isTrue();
        int proposal = state.diplomacy().proposals().getFirst().id();
        diplomacy.act(state, 1, new DiplomacyOrder(DiplomacyOrder.Action.APPROVE, proposal, 0));
        diplomacy.act(state, 2, new DiplomacyOrder(DiplomacyOrder.Action.APPROVE, proposal, 0));
        state.updateSystem(3, system -> system.captureBy(3, 5)); state.updateSystem(4, system -> system.captureBy(1, 5));
        fleets.sendFleet(state, 1, 1, 2, 10); through(state, 3);
        state.updateSystem(1, system -> system.captureBy(2, 50));
        assertThat(diplomacy.act(state, 2, new DiplomacyOrder(DiplomacyOrder.Action.LEAVE, 0, 0))).isTrue();
        through(state, 4); assertThat(state.allied(1, 3)).isTrue(); assertThat(state.allied(1, 2)).isFalse();
        through(state, 6); var stopped = state.fleets().getFirst();
        assertThat(stopped.toSystemId()).isEqualTo(3); assertThat(stopped.inFlight()).isFalse();
        assertThat(stopped.journey().readyTurn()).isEqualTo(7); assertThat(state.getSystem(3).garrison()).isEqualTo(25);
        through(state, 7); assertThat(state.fleets().getFirst().inFlight()).isFalse();
        through(state, 9); assertThat(state.fleets()).isEmpty();
        assertThat(state.getSystem(4).garrison()).isEqualTo(47); assertThat(state.getSystem(3).ownerId()).isEqualTo(3);
    }
    @Test void aLostPartnerStationReturnsItsIndependentContingentWithoutCapture() {
        var state = allied(); fleets.sendFleet(state, 1, 1, 2, 10); through(state, 3);
        state.players().add(new de.zettsystems.starfare.game.values.Player(3, "Third", false, "#009e73"));
        state.updateSystem(2, system -> system.captureBy(3, 1)); advance(state);
        assertThat(state.fleets().getFirst().evacuating()).isTrue(); assertThat(state.getSystem(2).ownerId()).isEqualTo(3);
        through(state, 6); assertThat(state.fleets()).isEmpty(); assertThat(state.getSystem(2).ownerId()).isEqualTo(3);
    }
    @Test void aBlockedContingentRecoversWhenItsStationBecomesItsOnlyOwnSystem() {
        var state = allied(); fleets.sendFleet(state, 1, 1, 2, 10); through(state, 3);
        state.updateSystem(1, system -> system.captureBy(2, 50)); state.updateSystem(3, system -> system.captureBy(2, 5));
        diplomacy.act(state, 1, new DiplomacyOrder(DiplomacyOrder.Action.LEAVE, 0, 0)); through(state, 5);
        assertThat(state.fleets().getFirst().journey().blocked()).isTrue();
        state.updateSystem(2, system -> system.captureBy(1, 1)); advance(state);
        assertThat(state.fleets()).isEmpty(); assertThat(state.getSystem(2).garrison()).isEqualTo(15);
    }
}
