package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.domain.GameArchiveEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

interface GameArchiveRepository extends JpaRepository<GameArchiveEntity, String> {
    /** Ids of the games that ended before the cutoff, oldest first, without reading their snapshots. */
    @Query("select a.id from GameArchiveEntity a where a.finishedAt < :cutoff order by a.finishedAt, a.id")
    List<String> findIdsFinishedBefore(@Param("cutoff") Instant cutoff);

    /** Ids of the archives stored before they had a listing (game_archive_seats and the listed columns). */
    @Query("select a.id from GameArchiveEntity a where a.listed = false order by a.id")
    List<String> findIdsNotListed();
}
