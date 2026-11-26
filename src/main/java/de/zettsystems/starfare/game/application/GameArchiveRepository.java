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

    /**
     * Ids of the games the account held a seat in, latest end first, then by id in code point order; the snapshot
     * is only searched in the database, not read.
     */
    @Query(value = "select a.id from game_archives a where cast(a.state_json as jsonb) -> 'seatByUser' ->> :account is not null"
            + " order by a.finished_at desc, a.id collate \"C\"", nativeQuery = true)
    List<String> findIdsWithSeat(@Param("account") String account);
}
