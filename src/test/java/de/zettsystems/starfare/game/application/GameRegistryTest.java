package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.Player;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class GameRegistryTest extends AbstractIntegrationTest {

    @Test
    void createGameReturnsUniqueIds() {
        GameId a = registry.createGame(GameSetup.defaults());
        GameId b = registry.createGame(GameSetup.defaults());

        assertThat(b).isNotEqualTo(a);
        assertThat(registry.listIds()).hasSize(2);
    }

    @Test
    void requireOnUnknownIdThrows() {
        GameId unknown = GameId.newId();
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> registry.require(unknown));
    }

    @Test
    void findOnUnknownIdIsEmpty() {
        assertThat(registry.find(GameId.newId())).isEmpty();
    }

    @Test
    void gameMustBeJoinedBeforeStart() {
        GameId id = registry.createGame(GameSetup.defaults());

        assertThat(registry.canStartGame(id)).isFalse();
        assertThat(registry.startGame(id)).isFalse();
        assertThat(registry.joinHumanPlayer(id, 1)).isTrue();
        assertThat(registry.canStartGame(id)).isTrue();
        assertThat(registry.startGame(id)).isTrue();
    }

    @Test
    void appliesStartProductionPerPlayerAndNeutralProductionRange() {
        GameSetup setup = new GameSetup(
                30, 1, 2,
                List.of(11, 7, 4),
                2, 5, 6, "#0072B2",
                true, true);
        GameId id = registry.createGame(setup);

        registry.readState(id, state -> {
            var byId = state.systems().stream().collect(java.util.stream.Collectors.toMap(s -> s.id(), s -> s));
            var p1Home = state.systems().stream().filter(s -> s.ownerId() != null && s.ownerId() == 1).findFirst().orElseThrow();
            var p2Home = state.systems().stream().filter(s -> s.ownerId() != null && s.ownerId() == 2).findFirst().orElseThrow();
            var p3Home = state.systems().stream().filter(s -> s.ownerId() != null && s.ownerId() == 3).findFirst().orElseThrow();

            assertThat(byId.get(p1Home.id()).productionPerTurn()).isEqualTo(11);
            assertThat(byId.get(p2Home.id()).productionPerTurn()).isEqualTo(7);
            assertThat(byId.get(p3Home.id()).productionPerTurn()).isEqualTo(4);

            state.systems().stream()
                    .filter(s -> s.ownerId() == null)
                    .forEach(s -> {
                assertThat(s.productionPerTurn()).isGreaterThanOrEqualTo(2);
                assertThat(s.productionPerTurn()).isLessThanOrEqualTo(5);
                    });
            return null;
        });
    }

    @Test
    void multipleHumansGetDistinctColors() {
        GameSetup setup = new GameSetup(
                24, 3, 1,
                List.of(4, 4, 4, 4),
                2, 6, 8, "#0072B2",
                true, true);
        GameId id = registry.createGame(setup);

        registry.readState(id, state -> {
            var humans = state.players().stream().filter(p -> !p.ai()).toList();
            assertThat(humans).hasSize(3);

            Set<String> colors = new HashSet<>();
            for (Player p : state.players()) {
                colors.add(p.colorHex());
            }
            assertThat(colors).hasSameSizeAs(state.players());

            Player seatZero = state.players().getFirst();
            assertThat(seatZero.ai()).isFalse();
            assertThat(seatZero.colorHex()).isEqualTo("#0072B2");
            return null;
        });
    }

    @Test
    void abortGameRemovesSession() {
        GameId id = registry.createGame(GameSetup.defaults());
        assertThat(registry.find(id)).isPresent();

        registry.abortGame(id);

        assertThat(registry.find(id)).isEmpty();
    }

    @Test
    void twoSessionsAreMutatedIndependentlyUnderConcurrency() throws Exception {
        GameId a = registry.createGame(GameSetup.defaults());
        GameId b = registry.createGame(GameSetup.defaults());

        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch done = new CountDownLatch(2);

        Runnable mutateA = () -> {
            try {
                barrier.await();
                for (int i = 0; i < 100; i++) {
                    registry.writeState(a, state -> {
                        state.waitThisTurn().add(i());
                        return null;
                    });
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                done.countDown();
            }
        };
        Runnable mutateB = () -> {
            try {
                barrier.await();
                for (int i = 0; i < 100; i++) {
                    registry.writeState(b, state -> {
                        state.waitThisTurn().add(j());
                        return null;
                    });
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                done.countDown();
            }
        };

        Thread ta = new Thread(mutateA);
        Thread tb = new Thread(mutateB);
        ta.start();
        tb.start();
        done.await();

        int sizeA = registry.readState(a, state -> state.waitThisTurn().size());
        int sizeB = registry.readState(b, state -> state.waitThisTurn().size());
        assertThat(sizeA).isGreaterThan(0);
        assertThat(sizeB).isGreaterThan(0);
    }

    private int counterA = 1;
    private int counterB = 10_000;

    private synchronized int i() {
        return counterA++;
    }

    private synchronized int j() {
        return counterB++;
    }

    @Test
    void snapshotDoesNotMutateSession() {
        GameId id = registry.createGame(GameSetup.defaults());
        registry.writeState(id, state -> {
            state.waitThisTurn().add(42);
            return null;
        });

        GameState snap = registry.readState(id, GameState::copyOf);
        snap.waitThisTurn().add(999);

        int real = registry.readState(id, state -> state.waitThisTurn().size());
        assertThat(real).isOne();
    }
}
