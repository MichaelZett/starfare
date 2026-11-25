package de.zettsystems.starfare.game.domain;

import de.zettsystems.starfare.ai.values.AiStrategy;
import de.zettsystems.starfare.game.values.ComputerStrength;
import de.zettsystems.starfare.game.values.GameConfig;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.RulesetRef;
import de.zettsystems.starfare.game.values.StarSystem;
import java.util.Objects;
import java.util.Random;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GalaxyGenesisTest {
    private static final long SEED = 42;

    @Test
    void theSameSeedGivesEveryStrengthTheSameGalaxy() {
        var easy = galaxy(ComputerStrength.EASY);
        for (ComputerStrength strength : ComputerStrength.values()) {
            var other = galaxy(strength);
            assertThat(other.systems()).extracting(StarSystem::name, StarSystem::x, StarSystem::y, StarSystem::ownerId)
                    .containsExactlyElementsOf(easy.systems().stream()
                            .map(system -> Tuple.tuple(system.name(), system.x(), system.y(), system.ownerId()))
                            .toList());
        }
    }

    @Test
    void peopleTakeTheFirstSeatsAndComputersStartWithTheirShare() {
        for (ComputerStrength strength : ComputerStrength.values()) {
            var state = galaxy(strength);
            assertThat(state.players()).extracting(Player::ai).containsExactly(false, true, true, true);
            assertThat(home(state, 1).productionPerTurn()).isEqualTo(GameConfig.START_PRODUCTION);
            assertThat(state.players().subList(1, 4)).allSatisfy(computer -> assertThat(home(state, computer.id()).productionPerTurn())
                    .isEqualTo(strength.computerStart(RulesetRef.SECTOR_FORCES, GameConfig.START_PRODUCTION)));
        }
    }

    @Test
    void anotherStartShareOnlyChangesTheComputerHomes() {
        var regular = galaxy(ComputerStrength.NORMAL);
        var stronger = new GameState();
        GalaxyGenesis.populate(stronger, setup(ComputerStrength.NORMAL), new Random(SEED), 2.0);
        assertThat(home(stronger, 1)).isEqualTo(home(regular, 1));
        assertThat(home(stronger, 2).productionPerTurn()).isEqualTo(2 * GameConfig.START_PRODUCTION);
        assertThat(stronger.systems().stream().filter(system -> system.ownerId() == null).toList())
                .isEqualTo(regular.systems().stream().filter(system -> system.ownerId() == null).toList());
    }

    @Test
    void theSeatsDrawLotsForTheHomes() {
        var cramped = new java.util.HashSet<Integer>();
        for (long seed = 1; seed <= 40; seed++) {
            var state = new GameState();
            GalaxyGenesis.populate(state, GameSetup.defaults().seatPlayers(1, 2), new Random(seed));
            cramped.add(state.players().stream().min(java.util.Comparator.comparingInt(player -> state.players().stream()
                    .filter(other -> other.id() != player.id())
                    .mapToInt(other -> state.travelRounds(home(state, player.id()).id(), home(state, other.id()).id())).sum()))
                    .orElseThrow().id());
        }
        assertThat(cramped).as("every seat sometimes gets the home closest to its opponents").containsExactlyInAnyOrder(1, 2, 3);
    }

    @Test
    void easyComputersPlanWithTheBaseline() {
        assertThat(galaxy(ComputerStrength.EASY).players().subList(1, 4)).extracting(Player::aiStrategy)
                .containsOnly(AiStrategy.BASELINE);
    }

    private static GameState galaxy(ComputerStrength strength) {
        var state = new GameState();
        GalaxyGenesis.populate(state, setup(strength), new Random(SEED));
        return state;
    }

    private static GameSetup setup(ComputerStrength strength) {
        return GameSetup.defaults().seatPlayers(1, 3).chooseComputerStrength(strength);
    }

    private static StarSystem home(GameState state, int player) {
        return state.systems().stream().filter(system -> Objects.equals(system.ownerId(), player)).findFirst().orElseThrow();
    }
}
