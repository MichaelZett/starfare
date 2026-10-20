package de.zettsystems.starfare.research;

import de.zettsystems.starfare.research.values.ShipCohorts;
import de.zettsystems.starfare.research.values.ShipResearch;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.game.values.StarSystem;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ShipCohortsTest {
    @Test void launchesUseNewShipsRecyclingUsesOldShipsAndCasualtiesCannotUpgradeSurvivors() {
        var mixed = new ShipCohorts(Map.of(0, 8, 2, 4));
        assertThat(mixed.take(5, true).levels()).containsEntry(2, 4).containsEntry(0, 1);
        assertThat(mixed.take(5, false).levels()).containsExactlyEntriesOf(Map.of(0, 5));
        assertThat(mixed.strength()).isEqualTo(12.8);
        for (int count = 0; count <= mixed.count(); count++) {
            var survived = mixed.survive(count);
            assertThat(survived.count()).isEqualTo(count);
            survived.levels().forEach((level, ships) -> assertThat(ships).isLessThanOrEqualTo(mixed.levels().get(level)));
        }
        assertThat(mixed.minus(mixed.take(5, true)).count()).isEqualTo(7);
        assertThatThrownBy(() -> mixed.take(13, true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mixed.survive(13)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mixed.minus(ShipCohorts.of(3, 1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ShipCohorts(Map.of(0, Integer.MAX_VALUE, 1, 1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ShipCohorts.of(0, -1)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void immutableSystemAndFleetOperationsPreserveBuildTimeGenerations() {
        var mixed = new ShipCohorts(Map.of(0, 8, 2, 4));
        var system = new StarSystem(1, "A", 0, 0, 1, 12, 4, false, 0, mixed);
        assertThat(system.launchFleet(4).composition()).isEqualTo(ShipCohorts.of(0, 8));
        assertThat(system.scrapShips(8).composition()).isEqualTo(ShipCohorts.of(2, 4));
        assertThat(system.reserveGarrison(3).relocateTo(1, 1).planShipbuilding(2).composition()).isEqualTo(mixed);
        assertThat(system.completeShipbuilding(2, 0, 4, 3).composition().levels()).containsEntry(3, 2);
        var fleet = new Fleet(1, 1, 1, 1, 2, 12, 1, 2).commissionedAs(mixed);
        assertThat(fleet.designateConquest(2).delayedByOneTurn().endJourneyAfterBattle().composition()).isEqualTo(mixed);
        assertThat(fleet.surviveBattle(6).composition()).isEqualTo(mixed.survive(6));
        assertThatThrownBy(() -> fleet.commissionedAs(ShipCohorts.of(2, 1))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void researchCostsRiseKeepRemaindersAndStopChargingAtTheMaximum() {
        var first = ShipResearch.INITIAL.allocate(25).advance(550);
        assertThat(first).isEqualTo(new ShipResearch(25, 2, 50));
        assertThat(ShipResearch.cost(2)).isEqualTo(900);
        assertThat(first.advance(0)).isEqualTo(first);
        assertThat(first.advance(1_000_000)).isEqualTo(new ShipResearch(25, ShipResearch.MAX_LEVEL, 0));
        assertThatThrownBy(() -> first.allocate(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> first.allocate(101)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> first.advance(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ShipResearch(0, 0, 100)).isInstanceOf(IllegalArgumentException.class);
    }
}
