package de.zettsystems.starfare.ai.domain;

import de.zettsystems.starfare.ai.values.AiObservation;
import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.fleet.values.FleetOrder;
import de.zettsystems.starfare.game.values.EmpireStats;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.game.values.RoundStatus;
import de.zettsystems.starfare.game.values.VisibleSystem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CautiousPolicyTest {
    private static final int ME = 1;

    @Test
    void waitsUntilTheSpareShipsClearlyBeatTheDefence() {
        // Duel: the home keeps three rounds of production (6); a neutral garrison of 9 needs 9 * 1.5 + 1 = 15 ships.
        var short1 = plan(2, List.of(own(1, 20, 2), neutral(2, 9)), Map.of(2, 3), List.of());
        assertThat(short1).isEmpty();

        var enough = plan(2, List.of(own(1, 21, 2), neutral(2, 9)), Map.of(2, 3), List.of());
        assertThat(enough).containsExactly(new FleetOrder.Send(ME, 1, 2, 15));
    }

    @Test
    void sendsEverySpareShipAndKeepsTheHomeGuard() {
        assertThat(plan(2, List.of(own(1, 40, 2), neutral(2, 4)), Map.of(2, 3), List.of()))
                .containsExactly(new FleetOrder.Send(ME, 1, 2, 34));
    }

    @Test
    void theGuardShrinksWithTheSquareRootOfTheRivals() {
        // Production 4: twelve ships against one rival, 4 * 3 / sqrt(3) = 7 against three.
        assertThat(plan(2, List.of(own(1, 40, 4), neutral(2, 4)), Map.of(2, 3), List.of()))
                .containsExactly(new FleetOrder.Send(ME, 1, 2, 28));
        assertThat(plan(4, List.of(own(1, 40, 4), neutral(2, 4)), Map.of(2, 3), List.of()))
                .containsExactly(new FleetOrder.Send(ME, 1, 2, 33));
    }

    @Test
    void prefersNeutralTargetsOverEmpiresAtTheSameCost() {
        var systems = List.of(own(1, 60, 2), neutral(2, 5), enemy(3, 5, 3));
        assertThat(plan(2, systems, Map.of(2, 4, 3, 3), List.of())).extracting(FleetOrder.Send::toSystemId).containsExactly(2);
    }

    @Test
    void countsTheEnemyProductionUntilArrival() {
        // Garrison 5 and three rounds of production 3: (5 + 9) * 1.5 + 1 = 22 ships, 6 stay at home.
        assertThat(plan(2, List.of(own(1, 27, 2), enemy(3, 5, 3)), Map.of(3, 3), List.of())).isEmpty();
        assertThat(plan(2, List.of(own(1, 28, 2), enemy(3, 5, 3)), Map.of(3, 3), List.of()))
                .containsExactly(new FleetOrder.Send(ME, 1, 3, 22));
    }

    @Test
    void assumesSixDefendersWhereNothingIsKnown() {
        assertThat(plan(2, List.of(own(1, 15, 2), unknown(2)), Map.of(2, 3), List.of())).isEmpty();
        assertThat(plan(2, List.of(own(1, 16, 2), unknown(2)), Map.of(2, 3), List.of()))
                .containsExactly(new FleetOrder.Send(ME, 1, 2, 10));
    }

    @Test
    void startsOneOperationPerRoundAndNeverASecondFleetToTheSameTarget() {
        var systems = List.of(own(1, 50, 2), own(4, 50, 2), neutral(2, 3), neutral(3, 3));
        assertThat(plan(2, systems, Map.of(2, 2, 3, 5), List.of())).containsExactly(new FleetOrder.Send(ME, 1, 2, 44));

        var flying = new Fleet(7, ME, 1, 1, 2, 44, 1, 3);
        assertThat(plan(2, systems, Map.of(2, 2, 3, 5), List.of(flying))).extracting(FleetOrder.Send::toSystemId)
                .containsExactly(3);
    }

    @Test
    void leavesAlliesAlone() {
        var systems = List.of(own(1, 60, 2), enemy(3, 1, 1));
        var observation = observation(3, systems, Map.of(3, 2), List.of(), Set.of(2));
        assertThat(AiPolicy.plan(AiStrategy.CAUTIOUS, observation).sends()).isEmpty();
    }

    private static List<FleetOrder.Send> plan(int players, List<VisibleSystem> systems, Map<Integer, Integer> roundsFromHome,
                                              List<Fleet> fleets) {
        return AiPolicy.plan(AiStrategy.CAUTIOUS, observation(players, systems, roundsFromHome, fleets, Set.of())).sends();
    }

    /** Every own system reaches a target in the given rounds from the first own system, one more from the others. */
    private static AiObservation observation(int players, List<VisibleSystem> systems, Map<Integer, Integer> roundsFromHome,
                                             List<Fleet> fleets, Set<Integer> allies) {
        var playerList = IntStream.rangeClosed(1, players).mapToObj(id -> new Player(id, "P" + id, id != ME, "#fff")).toList();
        var view = new PlayerViewState(5, playerList, systems, fleets, null, false, null, List.of(), List.of(),
                EmpireStats.NONE, Set.of(), false, RoundStatus.NONE, null);
        var owned = systems.stream().filter(system -> system.fullyVisible()).map(VisibleSystem::id).toList();
        Map<AiObservation.Route, Integer> travel = new HashMap<>();
        for (int index = 0; index < owned.size(); index++) {
            int extra = index;
            roundsFromHome.forEach((target, rounds) -> travel.put(new AiObservation.Route(owned.get(extra), target), rounds + extra));
        }
        return new AiObservation(ME, view, travel, allies);
    }

    private static VisibleSystem own(int id, int garrison, int production) {
        return new VisibleSystem(id, "S" + id, 0, 0, ME, garrison, production, true, "#fff", null, false, null, garrison, List.of());
    }

    private static VisibleSystem neutral(int id, int garrison) {
        return foreign(id, null, garrison, 3);
    }

    private static VisibleSystem enemy(int id, int garrison, int production) {
        return foreign(id, 2, garrison, production);
    }

    private static VisibleSystem unknown(int id) {
        return new VisibleSystem(id, "S" + id, 0, 0, null, null, null, false, null, null, false, null, List.of());
    }

    private static VisibleSystem foreign(int id, @Nullable Integer owner, int garrison, int production) {
        return new VisibleSystem(id, "S" + id, 0, 0, owner, garrison, production, false, "#000", 4, false, null, List.of());
    }
}
