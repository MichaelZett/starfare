package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.domain.FriendshipEntity;
import de.zettsystems.starfare.social.values.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

interface FriendshipRepository extends JpaRepository<FriendshipEntity, Long> {

    Optional<FriendshipEntity> findByUserAAndUserB(String userA, String userB);

    @Query("select f from FriendshipEntity f where (f.userA = :user or f.userB = :user) and f.status = :status")
    List<FriendshipEntity> findByUserAndStatus(String user, FriendshipStatus status);

    @Query("select f from FriendshipEntity f where f.userA = :user or f.userB = :user")
    List<FriendshipEntity> findByUser(String user);
}
