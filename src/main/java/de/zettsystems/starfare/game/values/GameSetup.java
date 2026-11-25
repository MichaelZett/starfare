package de.zettsystems.starfare.game.values;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * The choices of the new-game dialog. Everything else is fixed: start values, colours from the palette,
 * observers, re-entry and battle presentation always on, victory at 70 % of the systems, the start balance in
 * classic games. Setups stored by earlier versions keep only what this record still knows.
 *
 * @param systemCount    systems of the galaxy; the dialog offers the {@link GalaxySize} steps
 * @param maxProduction  highest production of a neutral system; neutral production follows a bell curve from 1
 * @param computerStrength planning profile and start values of the computer empires
 * @param randomness     combat deviation, event frequency and the order of several attackers
 * @param enabledEvents  the cosmic events the host left on; they strike only with randomness and classic rules
 * @param roundLimit     last round of a classic game, scored when nobody has won before; 0 means no limit
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GameSetup(
        int systemCount,
        int humanPlayers,
        int aiPlayers,
        @JsonSetter(nulls = Nulls.AS_EMPTY) int maxProduction,
        @Nullable GalaxyLayout galaxyLayout,
        @Nullable RoundRules roundRules,
        @Nullable RulesetRef ruleset,
        @Nullable ComputerStrength computerStrength,
        @Nullable RandomnessLevel randomness,
        @Nullable Set<CosmicEvent> enabledEvents,
        @JsonSetter(nulls = Nulls.AS_EMPTY) int roundLimit,
        // Setups stored before the choice existed have no value and are no tutorial.
        @JsonSetter(nulls = Nulls.AS_EMPTY) boolean tutorial) {

    public GameSetup {
        maxProduction = maxProduction <= 0 ? GameConfig.DEFAULT_MAX_PRODUCTION : maxProduction;
        galaxyLayout = galaxyLayout == null ? GameConfig.DEFAULT_GALAXY_LAYOUT : galaxyLayout;
        ruleset = ruleset == null ? RulesetRef.SECTOR_FORCES : ruleset;
        computerStrength = computerStrength == null ? ComputerStrength.DEFAULT : computerStrength;
        randomness = randomness == null ? RandomnessLevel.DEFAULT : randomness;
        enabledEvents = enabledEvents == null ? Set.copyOf(EnumSet.allOf(CosmicEvent.class))
                : enabledEvents.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(enabledEvents));
        RoundRules deadlines = roundRules == null ? RoundRules.defaults() : roundRules;
        // The randomness level decides the order of several attackers; coalition battles have none to store.
        AttackOrder order = ruleset.resolvesCoalitionBattles() ? RoundRules.DEFAULT_ATTACK_ORDER : randomness.attackOrder();
        roundRules = new RoundRules(deadlines.roundLimit(), deadlines.stragglerLimit(), order, deadlines.tempo());
        roundLimit = roundLimit <= 0 ? 0
                : Math.clamp(roundLimit, SectorOptions.MIN_ROUND_LIMIT, SectorOptions.MAX_ROUND_LIMIT);
    }

    /** What the dialog proposes: one person against two normal computers on a medium even galaxy. */
    public static GameSetup defaults() {
        return new GameSetup(GameConfig.DEFAULT_SYSTEM_COUNT, GameConfig.DEFAULT_HUMAN_PLAYERS,
                GameConfig.DEFAULT_AI_PLAYERS, GameConfig.DEFAULT_MAX_PRODUCTION, GameConfig.DEFAULT_GALAXY_LAYOUT,
                RoundRules.defaults(), RulesetRef.SECTOR_FORCES, ComputerStrength.DEFAULT, RandomnessLevel.DEFAULT,
                EnumSet.allOf(CosmicEvent.class), 0, false);
    }

    @Override public GalaxyLayout galaxyLayout() { return Objects.requireNonNull(galaxyLayout); }
    @Override public RoundRules roundRules() { return Objects.requireNonNull(roundRules); }
    @Override public RulesetRef ruleset() { return Objects.requireNonNull(ruleset); }
    @Override public ComputerStrength computerStrength() { return Objects.requireNonNull(computerStrength); }
    @Override public RandomnessLevel randomness() { return Objects.requireNonNull(randomness); }
    @Override public Set<CosmicEvent> enabledEvents() { return Objects.requireNonNull(enabledEvents); }

    public GameSetup seatPlayers(int humans, int computers) {
        return new GameSetup(systemCount, humans, computers, maxProduction, galaxyLayout, roundRules, ruleset,
                computerStrength, randomness, enabledEvents, roundLimit, tutorial);
    }

    public GameSetup chooseGalaxy(int systems, GalaxyLayout layout) {
        return new GameSetup(systems, humanPlayers, aiPlayers, maxProduction, layout, roundRules, ruleset,
                computerStrength, randomness, enabledEvents, roundLimit, tutorial);
    }

    public GameSetup chooseMaxProduction(int highest) {
        return new GameSetup(systemCount, humanPlayers, aiPlayers, highest, galaxyLayout, roundRules, ruleset,
                computerStrength, randomness, enabledEvents, roundLimit, tutorial);
    }

    public GameSetup chooseRoundRules(RoundRules rules) {
        return new GameSetup(systemCount, humanPlayers, aiPlayers, maxProduction, galaxyLayout, rules, ruleset,
                computerStrength, randomness, enabledEvents, roundLimit, tutorial);
    }

    public GameSetup selectRuleset(RulesetRef selected) {
        return new GameSetup(systemCount, humanPlayers, aiPlayers, maxProduction, galaxyLayout, roundRules, selected,
                computerStrength, randomness, enabledEvents, roundLimit, tutorial);
    }

    public GameSetup chooseComputerStrength(ComputerStrength strength) {
        return new GameSetup(systemCount, humanPlayers, aiPlayers, maxProduction, galaxyLayout, roundRules, ruleset,
                strength, randomness, enabledEvents, roundLimit, tutorial);
    }

    public GameSetup chooseRandomness(RandomnessLevel level) {
        return new GameSetup(systemCount, humanPlayers, aiPlayers, maxProduction, galaxyLayout, roundRules, ruleset,
                computerStrength, level, enabledEvents, roundLimit, tutorial);
    }

    /** The cosmic events that may strike; the randomness level decides how often. */
    public GameSetup chooseEvents(Set<CosmicEvent> events) {
        return new GameSetup(systemCount, humanPlayers, aiPlayers, maxProduction, galaxyLayout, roundRules, ruleset,
                computerStrength, randomness, events, roundLimit, tutorial);
    }

    /** Ends a classic game after this round with a score; 0 plays until someone wins. */
    public GameSetup limitRounds(int lastRound) {
        return new GameSetup(systemCount, humanPlayers, aiPlayers, maxProduction, galaxyLayout, roundRules, ruleset,
                computerStrength, randomness, enabledEvents, lastRound, tutorial);
    }

    /** A tutorial shows aids that are left out of a normal game; chosen at creation, visible to every seat. */
    public GameSetup chooseTutorial(boolean chosen) {
        return new GameSetup(systemCount, humanPlayers, aiPlayers, maxProduction, galaxyLayout, roundRules, ruleset,
                computerStrength, randomness, enabledEvents, roundLimit, chosen);
    }

    /** Keeps every count within the supported bounds; a galaxy has at least one system per empire. */
    public GameSetup normalized() {
        int humans = Math.clamp(humanPlayers, 0, GameConfig.MAX_TOTAL_PLAYERS);
        int computers = Math.clamp(aiPlayers, 0, GameConfig.MAX_TOTAL_PLAYERS - humans);
        int systems = Math.clamp(systemCount, Math.max(GameConfig.MIN_SYSTEM_COUNT, humans + computers),
                GameConfig.MAX_SYSTEM_COUNT);
        int highest = Math.clamp(maxProduction, GameConfig.MIN_PRODUCTION, GameConfig.MAX_PRODUCTION);
        return new GameSetup(systems, humans, computers, highest, galaxyLayout, roundRules, ruleset,
                computerStrength, randomness, enabledEvents, roundLimit, tutorial);
    }

    public int totalPlayers() {
        return humanPlayers + aiPlayers;
    }

    /** Seat colours come from the palette in seat order. */
    public String colorForSeat(int seatIndex) {
        return GameConfig.PLAYER_PALETTE.get(Math.floorMod(seatIndex, GameConfig.PLAYER_PALETTE.size()));
    }

    /** Largest deviation of a side's strength before a battle, from the randomness level. */
    public int combatRandomnessPercent() {
        return randomness().combatRandomnessPercent();
    }

    /** Fixed victory and diplomacy rules of the chosen rules. */
    public VictoryRules victoryRules() {
        return VictoryRules.forNewGame(ruleset());
    }

    /**
     * Classic options after Galactic Empire: the events at the frequency of the randomness level, the start
     * balance and the round limit. Other rules have none of them.
     */
    public SectorOptions sectorOptions() {
        if (!ruleset().usesSectorOptions()) {
            return SectorOptions.none();
        }
        return new SectorOptions(randomness().eventFrequency(), enabledEvents(), true, roundLimit);
    }
}
