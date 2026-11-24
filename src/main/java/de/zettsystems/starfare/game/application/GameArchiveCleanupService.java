package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.GameId;

import java.util.List;

/** Deletes finished games after {@code starfare.game.archive-cleanup.retention}. */
public interface GameArchiveCleanupService {
    /** The games the next run removes. */
    List<GameId> preview();

    /** Removes the stored game and its archive entry of every expired game; returns how many. */
    int removeExpired();
}
