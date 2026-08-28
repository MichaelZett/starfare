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
    void findByPlayerAAndPlayerBReturnsCanonicalPair() {
        repository.save(new FriendshipEntity("alice", "bob", FriendshipStatus.ACCEPTED, "alice", NOW));

        Optional<FriendshipEntity> found = repository.findByPlayerAAndPlayerB("alice", "bob");

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);
    }

    @Test
    void findByPlayerAAndPlayerBIsOrderSensitive() {
        repository.save(new FriendshipEntity("alice", "bob", FriendshipStatus.ACCEPTED, "alice", NOW));

        // CHECK constraint enforces user_a < user_b, so the swapped query must miss
        assertThat(repository.findByPlayerAAndPlayerB("bob", "alice")).isEmpty();
    }

    @Test
    void findByPlayerAndStatusMatchesEitherSide() {
        repository.save(new FriendshipEntity("alice", "bob", FriendshipStatus.ACCEPTED, "alice", NOW));
        repository.save(new FriendshipEntity("alice", "carol", FriendshipStatus.PENDING, "alice", NOW));
        repository.save(new FriendshipEntity("bob", "dave", FriendshipStatus.ACCEPTED, "bob", NOW));

        List<FriendshipEntity> bobsAccepted = repository.findByPlayerAndStatus("bob", FriendshipStatus.ACCEPTED);

        assertThat(bobsAccepted).as("bob is user_b in alice/bob and user_a in bob/dave").hasSize(2);
    }

    @Test
    void findByPlayerAndStatusFiltersOutMismatchingStatus() {
        repository.save(new FriendshipEntity("alice", "bob", FriendshipStatus.PENDING, "alice", NOW));

        assertThat(repository.findByPlayerAndStatus("bob", FriendshipStatus.ACCEPTED)).isEmpty();
        assertThat(repository.findByPlayerAndStatus("bob", FriendshipStatus.PENDING)).hasSize(1);
    }

    @Test
    void findByUserReturnsAllRelationshipsRegardlessOfStatus() {
        repository.save(new FriendshipEntity("alice", "bob", FriendshipStatus.ACCEPTED, "alice", NOW));
        repository.save(new FriendshipEntity("alice", "carol", FriendshipStatus.PENDING, "alice", NOW));
        repository.save(new FriendshipEntity("bob", "dave", FriendshipStatus.BLOCKED, "bob", NOW));

        List<FriendshipEntity> alicesRelations = repository.findByPlayer("alice");
        List<FriendshipEntity> bobsRelations = repository.findByPlayer("bob");

        assertThat(alicesRelations).hasSize(2);
        assertThat(bobsRelations).hasSize(2);
    }

    @Test
    void findByUserReturnsEmptyForUnknownUser() {
        repository.save(new FriendshipEntity("alice", "bob", FriendshipStatus.ACCEPTED, "alice", NOW));

        assertThat(repository.findByPlayer("zoe")).isEmpty();
    }

}
