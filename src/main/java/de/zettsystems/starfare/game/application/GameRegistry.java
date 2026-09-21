package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameSession;
import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.time.Duration;
import java.util.function.Function;

public interface GameRegistry {

    GameId createGame(GameSetup requestedSetup, @Nullable String hostPlayerId, String name);

    GameId createGame(GameSetup setup);

    List<GameId> listIds();

    default List<GameId> loadedIds() {
        return listIds();
    }

    Optional<GameSession> find(GameId id);

    GameSession require(GameId id);

    void touch(GameId id);

    <T> T readState(GameId id, Function<GameState, T> fn);

    <T> T writeState(GameId id, Function<GameState, T> fn);

    Optional<Integer> claimSeat(GameId id, @Nullable String playerId);

    Optional<Integer> claimSeat(GameId id, @Nullable String playerId, String playerName, String empireName);

    Optional<Integer> seatOf(GameId id, @Nullable String playerId);

    boolean joinHumanPlayer(GameId id, int playerId);

    boolean canStartGame(GameId id);

    boolean startGame(GameId id);

    void abortGame(GameId id);

    int unloadInactiveSingleHumanGames(Duration inactivity);
}
