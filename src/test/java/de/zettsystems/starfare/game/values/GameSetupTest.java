package de.zettsystems.starfare.game.values;

import de.zettsystems.starfare.ai.values.AiStrategy;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** The slim setup of the new-game dialog and the values it derives. */
class GameSetupTest {

    @Test
    void defaultsAreOnePersonAgainstTwoNormalComputersOnAMediumEvenGalaxyWithLittleChance() {
        GameSetup setup = GameSetup.defaults();

        assertThat(setup.systemCount()).isEqualTo(GalaxySize.MEDIUM.systems());
        assertThat(setup.humanPlayers()).isOne();
        assertThat(setup.aiPlayers()).isEqualTo(2);
        assertThat(setup.maxProduction()).isEqualTo(10);
        assertThat(setup.galaxyLayout()).isEqualTo(GalaxyLayout.EVEN);
        assertThat(setup.computerStrength()).isEqualTo(ComputerStrength.NORMAL);
        assertThat(setup.randomness()).isEqualTo(RandomnessLevel.LOW);
        assertThat(setup.enabledEvents()).containsExactlyInAnyOrder(CosmicEvent.values());
        assertThat(setup.roundLimit()).isZero();
        assertThat(setup.tutorial()).isFalse();
    }

    @Test
    void everyChoiceSurvivesEveryLaterChoice() {
        GameSetup setup = GameSetup.defaults().chooseTutorial(true).seatPlayers(2, 3)
                .chooseGalaxy(40, GalaxyLayout.RANDOM).chooseMaxProduction(15)
                .chooseComputerStrength(ComputerStrength.HARD).chooseRandomness(RandomnessLevel.HIGH)
                .chooseEvents(EnumSet.of(CosmicEvent.SUPERNOVA)).limitRounds(80)
                .chooseRoundRules(RoundRules.forTempo(GameTempo.BRISK, AttackOrder.RANDOM))
                .selectRuleset(RulesetRef.SECTOR_FORCES).normalized();

        assertThat(setup.tutorial()).isTrue();
        assertThat(setup.totalPlayers()).isEqualTo(5);
        assertThat(setup.systemCount()).isEqualTo(40);
        assertThat(setup.galaxyLayout()).isEqualTo(GalaxyLayout.RANDOM);
        assertThat(setup.maxProduction()).isEqualTo(15);
        assertThat(setup.computerStrength()).isEqualTo(ComputerStrength.HARD);
        assertThat(setup.enabledEvents()).containsExactly(CosmicEvent.SUPERNOVA);
        assertThat(setup.roundLimit()).isEqualTo(80);
        assertThat(setup.roundRules().tempo()).isEqualTo(GameTempo.BRISK);
        assertThat(setup.chooseTutorial(false).tutorial()).isFalse();
    }

    @Test
    void theRandomnessLevelSetsCombatDeviationEventsAndAttackOrder() {
        GameSetup none = GameSetup.defaults().chooseRandomness(RandomnessLevel.NONE);
        assertThat(none.combatRandomnessPercent()).isZero();
        assertThat(none.sectorOptions().eventFrequency()).isEqualTo(EventFrequency.OFF);
        assertThat(none.roundRules().attackOrder()).isEqualTo(AttackOrder.STRONGEST_FIRST);

        assertThat(GameSetup.defaults().combatRandomnessPercent()).isEqualTo(5);
        assertThat(GameSetup.defaults().sectorOptions().eventFrequency()).isEqualTo(EventFrequency.RARE);
        assertThat(GameSetup.defaults().roundRules().attackOrder()).isEqualTo(AttackOrder.RANDOM);

        GameSetup medium = GameSetup.defaults().chooseRandomness(RandomnessLevel.MEDIUM);
        assertThat(medium.combatRandomnessPercent()).isEqualTo(10);
        assertThat(medium.sectorOptions().eventFrequency()).isEqualTo(EventFrequency.NORMAL);

        GameSetup high = GameSetup.defaults().chooseRandomness(RandomnessLevel.HIGH);
        assertThat(high.combatRandomnessPercent()).isEqualTo(20);
        assertThat(high.sectorOptions().eventFrequency()).isEqualTo(EventFrequency.FREQUENT);
        assertThat(EventFrequency.FREQUENT.scale(0.05)).isEqualTo(0.1);
    }

    @Test
    void aClassicGameAlwaysBalancesItsStartAndKeepsItsEventsAndRoundLimit() {
        GameSetup classic = GameSetup.defaults().chooseEvents(Set.of(CosmicEvent.COSMIC_STORM)).limitRounds(5);

        assertThat(classic.sectorOptions().positionalBalance()).isTrue();
        assertThat(classic.sectorOptions().enabledEvents()).containsExactly(CosmicEvent.COSMIC_STORM);
        assertThat(classic.roundLimit()).isEqualTo(SectorOptions.MIN_ROUND_LIMIT);
        assertThat(classic.limitRounds(9999).roundLimit()).isEqualTo(SectorOptions.MAX_ROUND_LIMIT);
        assertThat(classic.limitRounds(-3).roundLimit()).isZero();
    }

    @Test
    void extendedGamesHaveNoClassicOptionsNoAttackOrderAndFixedAlliances() {
        GameSetup extended = GameSetup.defaults().chooseRandomness(RandomnessLevel.NONE)
                .selectRuleset(RulesetRef.EXTENDED);

        assertThat(extended.sectorOptions()).isEqualTo(SectorOptions.none());
        assertThat(extended.roundRules().attackOrder()).isEqualTo(RoundRules.DEFAULT_ATTACK_ORDER);
        assertThat(extended.victoryRules()).isEqualTo(new VictoryRules(70, true, true, 70, true));
        assertThat(GameSetup.defaults().victoryRules()).isEqualTo(VictoryRules.defaults(RulesetRef.SECTOR_FORCES, 70));
    }

    @Test
    void normalizationKeepsEveryCountWithinItsBounds() {
        GameSetup crowded = GameSetup.defaults().seatPlayers(12, 9).chooseGalaxy(4, GalaxyLayout.EVEN)
                .chooseMaxProduction(99).normalized();

        assertThat(crowded.humanPlayers()).isEqualTo(GameConfig.MAX_TOTAL_PLAYERS);
        assertThat(crowded.aiPlayers()).isZero();
        assertThat(crowded.systemCount()).isEqualTo(GameConfig.MAX_TOTAL_PLAYERS);
        assertThat(crowded.maxProduction()).isEqualTo(GameConfig.MAX_PRODUCTION);

        GameSetup tiny = GameSetup.defaults().seatPlayers(1, 1).chooseGalaxy(500, GalaxyLayout.EVEN).normalized();
        assertThat(tiny.systemCount()).isEqualTo(GameConfig.MAX_SYSTEM_COUNT);
        assertThat(GameSetup.defaults().chooseGalaxy(1, GalaxyLayout.EVEN).normalized().systemCount())
                .isEqualTo(GameConfig.MIN_SYSTEM_COUNT);
    }

    @Test
    void seatColoursFollowThePalette() {
        GameSetup setup = GameSetup.defaults();

        assertThat(setup.colorForSeat(0)).isEqualTo(GameConfig.PLAYER_PALETTE.getFirst());
        assertThat(setup.colorForSeat(GameConfig.PLAYER_PALETTE.size())).isEqualTo(GameConfig.PLAYER_PALETTE.getFirst());
    }

    @Test
    void galaxySizesNeedTwoSystemsPerEmpire() {
        assertThat(GalaxySize.SMALL.fits(8)).isTrue();
        assertThat(GalaxySize.SMALL.fits(9)).isFalse();
        assertThat(GalaxySize.fitting(GalaxySize.SMALL, 10)).isEqualTo(GalaxySize.MEDIUM);
        assertThat(GalaxySize.fitting(GalaxySize.LARGE, 10)).isEqualTo(GalaxySize.LARGE);
        assertThat(GalaxySize.of(64)).contains(GalaxySize.HUGE);
        assertThat(GalaxySize.of(25)).isEmpty();
    }

    @Test
    void computerStrengthScalesTheStartAndPicksProfiles() {
        RulesetRef classic = RulesetRef.SECTOR_FORCES;
        RulesetRef extended = RulesetRef.EXTENDED;
        assertThat(ComputerStrength.EASY.computerStart(classic, 4)).isEqualTo(2);
        assertThat(ComputerStrength.NORMAL.computerStart(classic, 8)).isEqualTo(8);
        assertThat(ComputerStrength.HARD.computerStart(classic, 8)).isEqualTo(12);
        assertThat(ComputerStrength.EASY.computerStart(classic, 1)).isOne();
        assertThat(ComputerStrength.EASY.computerStart(extended, 4)).as("extended keeps its values until phase E").isEqualTo(3);
        assertThat(ComputerStrength.HARD.computerStart(extended, 8)).isEqualTo(10);

        Random random = new Random(3);
        assertThat(ComputerStrength.EASY.strategy(classic, 2, random)).isEqualTo(AiStrategy.BASELINE);
        for (int draw = 0; draw < 20; draw++) {
            assertThat(ComputerStrength.NORMAL.strategy(classic, 2, random)).isEqualTo(AiStrategy.BASELINE);
            assertThat(ComputerStrength.HARD.strategy(classic, 2, random)).isIn(AiStrategy.BASELINE, AiStrategy.RUSH);
            assertThat(ComputerStrength.NORMAL.strategy(extended, 4, random))
                    .isIn(ComputerStrength.NORMAL_EXTENDED_PROFILES);
            assertThat(ComputerStrength.HARD.strategy(extended, 5, random))
                    .isIn(AiStrategy.INDUSTRY_ADAPTIVE, AiStrategy.BASELINE);
        }
        assertThat(ComputerStrength.NORMAL.strategy(classic, 4, random)).isEqualTo(AiStrategy.BASELINE);
        assertThat(ComputerStrength.HARD.strategy(classic, 3, random)).isEqualTo(AiStrategy.BASELINE);
        assertThat(ComputerStrength.HARD.strategy(extended, 2, random)).isEqualTo(AiStrategy.INDUSTRY_ADAPTIVE);
    }

    @Test
    void storedSetupsOfEarlierVersionsKeepWhatTheSetupStillKnows() {
        String earlier = """
                {"systemCount":30,"humanPlayers":2,"aiPlayers":1,"startProductionPerPlayer":[4,4,4],
                 "neutralMinProduction":2,"neutralMaxProduction":9,"startGarrison":8,"observersAllowed":true,
                 "seatColorHexes":["#0072B2"],"productionDistribution":"UNIFORM","galaxyLayout":"RANDOM",
                 "combatRandomnessPercent":12,"victorySystemPercent":60,"ruleset":{"variant":"classic","version":"1.0.0"}}
                """;

        GameSetup restored = JsonMapper.builder().build().readValue(earlier, GameSetup.class);

        assertThat(restored.systemCount()).isEqualTo(30);
        assertThat(restored.totalPlayers()).isEqualTo(3);
        assertThat(restored.galaxyLayout()).isEqualTo(GalaxyLayout.RANDOM);
        assertThat(restored.maxProduction()).isEqualTo(GameConfig.DEFAULT_MAX_PRODUCTION);
        assertThat(restored.randomness()).isEqualTo(RandomnessLevel.DEFAULT);
        assertThat(restored.computerStrength()).isEqualTo(ComputerStrength.DEFAULT);
    }
}
