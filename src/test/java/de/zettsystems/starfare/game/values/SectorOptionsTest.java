package de.zettsystems.starfare.game.values;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SectorOptionsTest {

    @Test
    void everythingIsOffByDefaultButEveryEventIsPreselected() {
        SectorOptions none = SectorOptions.none();
        assertThat(none.isDefault()).isTrue();
        assertThat(none.activeEvents()).isEmpty();
        assertThat(none.enabledEvents()).containsExactlyInAnyOrder(CosmicEvent.values());
        assertThat(none.chance(CosmicEvent.COSMIC_STORM)).isZero();
        assertThat(SectorOptions.orNone(null)).isEqualTo(none);
    }

    @Test
    void theMasterSwitchScalesEveryEnabledEvent() {
        var events = EnumSet.of(CosmicEvent.COSMIC_STORM, CosmicEvent.SUPERNOVA);
        var rare = new SectorOptions(EventFrequency.RARE, events, false, 0);
        var normal = new SectorOptions(EventFrequency.NORMAL, events, false, 0);
        assertThat(normal.chance(CosmicEvent.COSMIC_STORM)).isEqualTo(CosmicEvent.COSMIC_STORM.baseChance());
        assertThat(rare.chance(CosmicEvent.COSMIC_STORM)).isEqualTo(CosmicEvent.COSMIC_STORM.baseChance() / 2);
        assertThat(normal.chance(CosmicEvent.SPACEPORT_QUAKE)).as("switched off").isZero();
        assertThat(normal.activeEvents()).containsExactly(CosmicEvent.COSMIC_STORM, CosmicEvent.SUPERNOVA);
        assertThat(normal.isDefault()).isFalse();
    }

    @Test
    void piratesNeedTheSupernova() {
        var withoutSupernova = new SectorOptions(EventFrequency.NORMAL, Set.of(CosmicEvent.PIRATES), false, 0);
        var withSupernova = new SectorOptions(EventFrequency.NORMAL,
                Set.of(CosmicEvent.PIRATES, CosmicEvent.SUPERNOVA), false, 0);
        assertThat(withoutSupernova.enabled(CosmicEvent.PIRATES)).isFalse();
        assertThat(withSupernova.enabled(CosmicEvent.PIRATES)).isTrue();
    }

    @Test
    void theRoundLimitIsClampedAndZeroMeansNoLimit() {
        assertThat(new SectorOptions(EventFrequency.OFF, Set.of(), false, 3).roundLimit())
                .isEqualTo(SectorOptions.MIN_ROUND_LIMIT);
        assertThat(new SectorOptions(EventFrequency.OFF, Set.of(), false, 9999).roundLimit())
                .isEqualTo(SectorOptions.MAX_ROUND_LIMIT);
        var none = new SectorOptions(EventFrequency.OFF, Set.of(), false, -4);
        assertThat(none.hasRoundLimit()).isFalse();
        assertThat(new SectorOptions(null, null, true, 0).eventFrequency()).isEqualTo(EventFrequency.OFF);
    }

    @Test
    void onlySectorForcesKeepsTheOptions() {
        var chosen = new SectorOptions(EventFrequency.NORMAL, Set.of(CosmicEvent.SUPERNOVA), true, 40);
        GameSetup sector = GameSetup.defaults().withSectorOptions(chosen);
        assertThat(sector.sectorOptions()).isEqualTo(chosen);
        assertThat(sector.selectRuleset(RulesetRef.SPACEWARD).sectorOptions()).isEqualTo(SectorOptions.none());
        assertThat(sector.chooseAiStrategies(java.util.List.of()).sectorOptions()).isEqualTo(chosen);
    }
}
