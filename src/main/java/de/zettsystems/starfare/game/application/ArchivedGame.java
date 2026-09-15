package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameState;
import de.zettsystems.starfare.game.values.GameId;
import org.jspecify.annotations.Nullable;
import java.time.Instant;

record ArchivedGame(GameId id, String name, @Nullable String hostPlayerId, Instant finishedAt, GameState state) {}
