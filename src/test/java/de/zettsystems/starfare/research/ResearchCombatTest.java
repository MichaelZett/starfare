package de.zettsystems.starfare.research;

import de.zettsystems.starfare.combat.application.DefaultCoalitionCombatService;
import de.zettsystems.starfare.combat.domain.CoalitionResolver;
import de.zettsystems.starfare.economy.domain.RecyclingOrders;
import de.zettsystems.starfare.fleet.application.DefaultFleetService;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.game.values.StarSystem;
import de.zettsystems.starfare.navigation.application.DefaultNavigationService;
import de.zettsystems.starfare.report.application.DefaultReportService;
import de.zettsystems.starfare.report.values.CoalitionSide;
import de.zettsystems.starfare.research.values.ShipCohorts;
import de.zettsystems.starfare.testsupport.ColonyStates;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ResearchCombatTest {
    @Test void legacyBattleJsonKeepsHullCountAsBaseStrength() {
        var side = new tools.jackson.databind.ObjectMapper().readValue(
                "{\"id\":1,\"strength\":11,\"members\":[{\"playerId\":1,\"name\":\"A\",\"ships\":10,\"remaining\":3,\"destroyed\":4}]}",
                CoalitionSide.class);
        assertThat(side.baseStrength()).isEqualTo(10);
        assertThat(side.strength()).isEqualTo(11);
    }

    @Test void integerRoundingKeepsAlliedOwnersAndIndividualContingentsSeparateInEveryVolley() {
        var forces = new de.zettsystems.starfare.combat.domain.CombatCohorts(List.of(
                new de.zettsystems.starfare.combat.domain.CombatCohorts.Force(1, 1, 7, ShipCohorts.of(20, 13)),
                new de.zettsystems.starfare.combat.domain.CombatCohorts.Force(1, 2, 8, ShipCohorts.of(0, 13))));
        // The same lower-player-ID tie breaker as actual survivor allocation keeps one strong hull.
        assertThat(forces.strength(1, 1)).isEqualTo(3);
        assertThat(forces.strength(1, 26)).isEqualTo(52);
        assertThat(forces.strength(1, 0)).isZero();
    }

    @Test void weightedMixedGenerationsAffectCombatWithoutChangingTheNumberOfHulls() {
        var forces = List.of(new CoalitionSide(1, 0, List.of(new CoalitionSide.Member(1, "A", 20, 20, 0))),
                new CoalitionSide(2, 0, List.of(new CoalitionSide.Member(2, "B", 20, 20, 0))));
        var ordinary = CoalitionResolver.resolve(forces, 0, _ -> 0.5);
        assertThat(ordinary).allMatch(side -> side.remaining() == 0);
        var mixed = new ShipCohorts(Map.of(0, 10, 10, 10));
        var stronger = CoalitionResolver.resolve(forces, 0, _ -> 0.5, Map.of(1, mixed));
        assertThat(stronger.getFirst().strength()).isEqualTo(30);
        assertThat(stronger.getFirst().baseStrength()).isEqualTo(30);
        assertThat(de.zettsystems.starfare.report.values.CombatReadiness.from(20, 30, 30, 1).level()).isEqualTo(2);
        assertThat(stronger.getFirst().remaining()).isEqualTo(6);
        assertThat(stronger.get(1).remaining()).isZero();
        assertThat(CoalitionResolver.resolve(forces, 0, _ -> 0.5, Map.of(1, ShipCohorts.of(5, 20))))
                .isEqualTo(stronger);
    }

    @Test void conquestKeepsSurvivorGenerationsAndAccountsForPhysicalCombatLosses() {
        var state = ColonyStates.ready(RulesetRef.SPACEWARD_SHIP_RESEARCH);
        state.updateSystem(1, s -> new StarSystem(s.id(), s.name(), s.x(), s.y(), s.ownerId(), s.garrison(),
                s.productionPerTurn(), false, 0, ShipCohorts.of(20, 12)));
        state.start(); state.players().forEach(player -> state.intel().put(player.id(), new java.util.HashMap<>()));
        state.planShipResearch(1, 17); state.planShipResearch(2, 23);
        var fleets = new DefaultFleetService();
        long balance = RecyclingOrders.balanceTenths(state);
        assertThat(fleets.sendFleet(state, 1, 1, 3, 12)).isTrue();
        var fleet = state.fleets().getFirst();
        state.replaceFleet(new Fleet(fleet.globalId(), 1, fleet.localNo(), 1, 3, 12, state.turn(), state.turn() + 1)
                .commissionedAs(fleet.composition()));
        long before = state.totalShips();
        var battle = new DefaultCoalitionCombatService(new DefaultReportService(), new DefaultNavigationService(), _ -> 0.5);
        battle.resolveArrivals(state);
        state.recordCombatLoss(before - state.totalShips());
        assertThat(state.getSystem(3).ownerId()).isEqualTo(1);
        assertThat(state.getSystem(3).composition()).isEqualTo(ShipCohorts.of(20, 2));
        assertThat(state.fleets()).isEmpty();
        assertThat(RecyclingOrders.balanceTenths(state)).isEqualTo(balance);
        assertThatCode(() -> GameState.fromSnapshot(GameState.toSnapshot(state))).doesNotThrowAnyException();
    }

    @Test void dispatchDisbandAndStandingRoutesNeverReplaceOlderShipsByCurrentTechnology() {
        var state = ColonyStates.ready(RulesetRef.SPACEWARD_SHIP_RESEARCH);
        var mixed = new ShipCohorts(Map.of(0, 8, 2, 4));
        state.updateSystem(1, s -> new StarSystem(s.id(), s.name(), s.x(), s.y(), s.ownerId(), 12,
                s.productionPerTurn(), false, 0, mixed));
        state.start();
        var fleets = new DefaultFleetService();
        assertThat(fleets.sendFleet(state, 1, 1, 2, 5)).isTrue();
        var first = state.fleets().getFirst();
        assertThat(first.composition().levels()).containsEntry(2, 4).containsEntry(0, 1);
        assertThat(fleets.disbandFleet(state, 1, first.globalId())).isTrue();
        assertThat(state.getSystem(1).composition()).isEqualTo(mixed);
        assertThat(fleets.addStandingOrder(state, 1, 1, 2, 3)).isPositive();
        assertThat(fleets.addStandingOrder(state, 1, 1, 3, 3)).isPositive();
        var routed = fleets.applyStandingOrdersForProduction(state);
        routed.forEach((system, ships) -> state.updateSystem(system, s -> s.launchFleet(ships)));
        var travelling = state.fleets().stream().map(Fleet::composition).reduce(ShipCohorts.of(0, 0), ShipCohorts::merge);
        assertThat(travelling.merge(state.getSystem(1).composition())).isEqualTo(mixed);
        assertThat(state.fleets().getFirst().composition()).isEqualTo(ShipCohorts.of(2, 3));
    }
}
