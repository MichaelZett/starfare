package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.domain.FriendshipEntity;
import de.zettsystems.starfare.social.values.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

interface FriendshipRepository extends JpaRepository<FriendshipEntity, Long> {

    Optional<FriendshipEntity> findByPlayerAAndPlayerB(String playerA, String playerB);

    @Query("select f from FriendshipEntity f where (f.playerA = :user or f.playerB = :user) and f.status = :status")
    List<FriendshipEntity> findByPlayerAndStatus(String user, FriendshipStatus status);

    @Query("select f from FriendshipEntity f where f.playerA = :user or f.playerB = :user")
    List<FriendshipEntity> findByPlayer(String user);
}
