package de.zettsystems.starfare.economy.values;

import de.zettsystems.starfare.game.values.VisibleSystem;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EconomyIssuesTest {
    private static VisibleSystem system(int id, int owner, boolean visible, IndustryView industry) {
        return new VisibleSystem(id, "System " + id, 0, 0, owner, 10, 2, visible, "#abc", null, false,
                0, 0, 10, List.of(), industry);
    }
    @Test void sharedShortageIsOneGroupAndMultipleReasonsStayOnOneSystem() {
        var shortage = new IndustryView(10, 0, 0, 20, 2, true, 0, null, new MetalView(1, 3, 2, 0, 1, 8, 1));
        var systems = List.of(system(3, 1, true, shortage), system(1, 1, true, shortage), system(2, 1, true, shortage));
        var issues = EconomyIssues.forSystems(systems, 1, Set.of(), Set.of());
        assertThat(issues).extracting(issue -> issue.system().id()).containsExactly(1, 2, 3);
        assertThat(issues).hasSize(3);
        assertThat(issues.getFirst().reasons()).containsExactly(EconomyIssues.Kind.DELIVERY, EconomyIssues.Kind.METAL, EconomyIssues.Kind.DEPOSIT);
        assertThat(issues.stream().filter(issue -> issue.reasons().contains(EconomyIssues.Kind.METAL)).count()).isEqualTo(3);
    }
    @Test void researchReservationsAreNotMissingWorkers() {
        var research = new IndustryView(10, 0, 0, 20, 0, false, 0, null, null, 5, null);
        assertThat(EconomyIssues.forSystems(List.of(system(1, 1, true, research)), 1, Set.of(), Set.of())).isEmpty();
    }
    @Test void ownershipVisibilityAndPendingBattlesProtectAggregation() {
        var shortage = new IndustryView(10, 0, 0, 20, 0, false, 0, null, new MetalView(0, 3, 0, 0, 0, 10, 0));
        var systems = List.of(system(1, 1, true, shortage), system(2, 2, true, shortage), system(3, 1, false, shortage));
        assertThat(EconomyIssues.forSystems(systems, 1, Set.of(), Set.of())).hasSize(1);
        assertThat(EconomyIssues.forSystems(systems, 1, Set.of(2), Set.of())).isEmpty();
    }
    @Test void zeroMiningAndDeliberatelyZeroShipbuildingAreNotWarnings() {
        var quiet = new IndustryView(10, 10, 0, 20, 0, false, 0, null, new MetalView(0, 3, 0, 0, 0, 0, 0));
        var systems = List.of(system(1, 1, true, quiet));
        assertThat(EconomyIssues.forSystems(systems, 1, Set.of(), Set.of())).isEmpty();
        assertThat(EconomyIssues.forSystems(systems, 1, Set.of(), Set.of(1)).getFirst().reasons())
                .containsExactly(EconomyIssues.Kind.DELIVERY);
    }
}
