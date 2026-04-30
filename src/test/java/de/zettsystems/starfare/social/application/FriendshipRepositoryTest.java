package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.AbstractRepositoryTest;
import de.zettsystems.starfare.social.domain.FriendshipEntity;
import de.zettsystems.starfare.social.values.FriendshipStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FriendshipRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private FriendshipRepository repository;

    private static final Instant NOW = Instant.parse("2026-04-22T10:00:00Z");

    @BeforeEach
    void clearFriendships() {
        repository.deleteAll();
    }

    @Test
    void findByUserAAndUserBReturnsCanonicalPair() {
        repository.save(new FriendshipEntity("alice", "bob", FriendshipStatus.ACCEPTED, "alice", NOW));

        Optional<FriendshipEntity> found = repository.findByUserAAndUserB("alice", "bob");

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);
    }

    @Test
    void findByUserAAndUserBIsOrderSensitive() {
        repository.save(new FriendshipEntity("alice", "bob", FriendshipStatus.ACCEPTED, "alice", NOW));

        // CHECK constraint enforces user_a < user_b, so the swapped query must miss
        assertThat(repository.findByUserAAndUserB("bob", "alice")).isNotPresent();
    }

    @Test
    void findByUserAndStatusMatchesEitherSide() {
        repository.save(new FriendshipEntity("alice", "bob", FriendshipStatus.ACCEPTED, "alice", NOW));
        repository.save(new FriendshipEntity("alice", "carol", FriendshipStatus.PENDING, "alice", NOW));
        repository.save(new FriendshipEntity("bob", "dave", FriendshipStatus.ACCEPTED, "bob", NOW));

        List<FriendshipEntity> bobsAccepted = repository.findByUserAndStatus("bob", FriendshipStatus.ACCEPTED);

        assertThat(bobsAccepted).as("bob is user_b in alice/bob and user_a in bob/dave").hasSize(2);
    }

    @Test
    void findByUserAndStatusFiltersOutMismatchingStatus() {
        repository.save(new FriendshipEntity("alice", "bob", FriendshipStatus.PENDING, "alice", NOW));

        assertThat(repository.findByUserAndStatus("bob", FriendshipStatus.ACCEPTED)).isEmpty();
        assertThat(repository.findByUserAndStatus("bob", FriendshipStatus.PENDING)).hasSize(1);
    }

    @Test
    void findByUserReturnsAllRelationshipsRegardlessOfStatus() {
        repository.save(new FriendshipEntity("alice", "bob", FriendshipStatus.ACCEPTED, "alice", NOW));
        repository.save(new FriendshipEntity("alice", "carol", FriendshipStatus.PENDING, "alice", NOW));
        repository.save(new FriendshipEntity("bob", "dave", FriendshipStatus.BLOCKED, "bob", NOW));

        List<FriendshipEntity> alicesRelations = repository.findByUser("alice");
        List<FriendshipEntity> bobsRelations = repository.findByUser("bob");

        assertThat(alicesRelations).hasSize(2);
        assertThat(bobsRelations).hasSize(2);
    }

    @Test
    void findByUserReturnsEmptyForUnknownUser() {
        repository.save(new FriendshipEntity("alice", "bob", FriendshipStatus.ACCEPTED, "alice", NOW));

        assertThat(repository.findByUser("zoe")).isEmpty();
    }

}
