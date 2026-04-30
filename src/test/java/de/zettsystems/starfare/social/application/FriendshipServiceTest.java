package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.AbstractIntegrationTest;
import de.zettsystems.starfare.social.values.Friendship;
import de.zettsystems.starfare.social.values.FriendshipStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FriendshipServiceTest extends AbstractIntegrationTest {

    @Autowired
    private FriendshipService service;

    @Autowired
    private FriendshipRepository repository;

    @BeforeEach
    void cleanFriendships() {
        repository.deleteAll();
    }

    @Test
    void requestCreatesPendingEntry() {
        assertThat(service.request("alice", "bob")).isTrue();

        Optional<Friendship> status = service.statusBetween("alice", "bob");
        assertThat(status).isPresent();
        assertThat(status.get().status()).isEqualTo(FriendshipStatus.PENDING);
        assertThat(status.get().requestedBy()).isEqualTo("alice");
    }

    @Test
    void duplicateRequestIsRejected() {
        assertThat(service.request("alice", "bob")).isTrue();
        assertThat(service.request("alice", "bob")).isFalse();
        assertThat(service.request("bob", "alice")).isFalse();
    }

    @Test
    void selfRequestIsRejected() {
        assertThat(service.request("alice", "alice")).isFalse();
        assertThat(service.request("ALICE", "alice")).isFalse();
    }

    @Test
    void acceptPromotesPendingToAccepted() {
        service.request("alice", "bob");

        assertThat(service.accept("bob", "alice")).isTrue();
        assertThat(service.areFriends("alice", "bob")).isTrue();
        assertThat(service.statusBetween("alice", "bob").orElseThrow().status()).isEqualTo(FriendshipStatus.ACCEPTED);
    }

    @Test
    void initiatorCannotAcceptOwnRequest() {
        service.request("alice", "bob");

        assertThat(service.accept("alice", "bob")).isFalse();
    }

    @Test
    void declineRemovesPendingRequest() {
        service.request("alice", "bob");

        assertThat(service.decline("bob", "alice")).isTrue();
        assertThat(service.statusBetween("alice", "bob")).isEmpty();
    }

    @Test
    void removeDropsFriendship() {
        service.request("alice", "bob");
        service.accept("bob", "alice");

        assertThat(service.remove("alice", "bob")).isTrue();
        assertThat(service.areFriends("alice", "bob")).isFalse();
    }

    @Test
    void initiatorCanCancelOwnPendingRequest() {
        service.request("alice", "bob");

        assertThat(service.remove("alice", "bob")).isTrue();
        assertThat(service.statusBetween("alice", "bob")).isEmpty();
    }

    @Test
    void blockOverwritesPending() {
        service.request("alice", "bob");

        assertThat(service.block("alice", "bob")).isTrue();

        Friendship status = service.statusBetween("alice", "bob").orElseThrow();
        assertThat(status.status()).isEqualTo(FriendshipStatus.BLOCKED);
        assertThat(status.requestedBy()).isEqualTo("alice");
        assertThat(service.isBlockedBetween("alice", "bob")).isTrue();
    }

    @Test
    void onlyBlockerCanUnblock() {
        service.block("alice", "bob");

        assertThat(service.unblock("bob", "alice")).isFalse();
        assertThat(service.isBlockedBetween("alice", "bob")).isTrue();
        assertThat(service.unblock("alice", "bob")).isTrue();
        assertThat(service.isBlockedBetween("alice", "bob")).isFalse();
    }

    @Test
    void incomingRequestsListsOnlyForeignInitiators() {
        service.request("alice", "bob");
        service.request("carol", "bob");

        List<Friendship> incoming = service.incomingRequests("bob");

        assertThat(incoming).hasSize(2);
        assertThat(incoming.stream().allMatch(f -> f.status() == FriendshipStatus.PENDING)).isTrue();
        assertThat(incoming.stream().noneMatch(f -> "bob".equals(f.requestedBy()))).isTrue();
    }

    @Test
    void incomingRequestsForInitiatorIsEmpty() {
        service.request("alice", "bob");

        assertThat(service.incomingRequests("alice")).isEmpty();
    }

    @Test
    void friendsOfReturnsAcceptedOnly() {
        service.request("alice", "bob");
        service.accept("bob", "alice");
        service.request("alice", "carol");

        List<Friendship> friends = service.friendsOf("alice");

        assertThat(friends).hasSize(1);
        assertThat(friends.getFirst().status()).isEqualTo(FriendshipStatus.ACCEPTED);
    }

    @Test
    void canonicalisationIsCaseInsensitive() {
        service.request("Alice", "BOB");

        assertThat(service.statusBetween("ALICE", "bob")).isPresent();
        assertThat(service.statusBetween("bob", "alice")).isPresent();
    }
}
