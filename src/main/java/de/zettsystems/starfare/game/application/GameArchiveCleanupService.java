package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.GameId;
import java.util.List;

public interface GameArchiveCleanupService { List<GameId> preview(); int removeExpired(); }
