package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.domain.FriendshipEntity;
import de.zettsystems.starfare.social.values.Friendship;
import de.zettsystems.starfare.social.values.FriendshipStatus;
import de.zettsystems.starfare.social.values.SocialEvent;
import de.zettsystems.starfare.social.values.Usernames;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class DefaultFriendshipService implements FriendshipService {

    private final FriendshipRepository repository;
    private final SocialBroadcaster broadcaster;

    public DefaultFriendshipService(FriendshipRepository repository, SocialBroadcaster broadcaster) {
        this.repository = repository;
        this.broadcaster = broadcaster;
    }

    @Override
    @Transactional
    public boolean request(String from, String to) {
        String a = Usernames.normalize(from);
        String b = Usernames.normalize(to);
        if (a == null || b == null || a.equals(b)) {
            return false;
        }
        String[] pair = canonical(a, b);
        Optional<FriendshipEntity> existing = repository.findByUserAAndUserB(pair[0], pair[1]);
        if (existing.isPresent()) {
            return false;
        }
        FriendshipEntity entity = new FriendshipEntity(pair[0], pair[1], FriendshipStatus.PENDING, a, Instant.now());
        repository.save(entity);
        broadcaster.publish(new SocialEvent.FriendRequestReceived(a, b));
        return true;
    }

    @Override
    @Transactional
    public boolean accept(String accepter, String other) {
        String self = Usernames.normalize(accepter);
        String o = Usernames.normalize(other);
        if (self == null || o == null) {
            return false;
        }
        String[] pair = canonical(self, o);
        Optional<FriendshipEntity> existing = repository.findByUserAAndUserB(pair[0], pair[1]);
        if (existing.isEmpty()) {
            return false;
        }
        FriendshipEntity entity = existing.get();
        if (entity.getStatus() != FriendshipStatus.PENDING) {
            return false;
        }
        if (self.equals(entity.getRequestedBy())) {
            return false;
        }
        entity.accept(Instant.now());
        repository.save(entity);
        broadcaster.publish(new SocialEvent.FriendshipUpdated(pair[0], pair[1], FriendshipStatus.ACCEPTED));
        return true;
    }

    @Override
    @Transactional
    public boolean decline(String decliner, String other) {
        String self = Usernames.normalize(decliner);
        String o = Usernames.normalize(other);
        if (self == null || o == null) {
            return false;
        }
        String[] pair = canonical(self, o);
        Optional<FriendshipEntity> existing = repository.findByUserAAndUserB(pair[0], pair[1]);
        if (existing.isEmpty()) {
            return false;
        }
        FriendshipEntity entity = existing.get();
        if (entity.getStatus() != FriendshipStatus.PENDING) {
            return false;
        }
        if (self.equals(entity.getRequestedBy())) {
            return false;
        }
        repository.delete(entity);
        broadcaster.publish(new SocialEvent.FriendshipUpdated(pair[0], pair[1], null));
        return true;
    }

    @Override
    @Transactional
    public boolean remove(String actor, String other) {
        String self = Usernames.normalize(actor);
        String o = Usernames.normalize(other);
        if (self == null || o == null) {
            return false;
        }
        String[] pair = canonical(self, o);
        Optional<FriendshipEntity> existing = repository.findByUserAAndUserB(pair[0], pair[1]);
        if (existing.isEmpty()) {
            return false;
        }
        FriendshipEntity entity = existing.get();
        if (entity.getStatus() != FriendshipStatus.ACCEPTED
                && !(entity.getStatus() == FriendshipStatus.PENDING && self.equals(entity.getRequestedBy()))) {
            return false;
        }
        repository.delete(entity);
        broadcaster.publish(new SocialEvent.FriendshipUpdated(pair[0], pair[1], null));
        return true;
    }

    @Override
    @Transactional
    public boolean block(String blocker, String other) {
        String self = Usernames.normalize(blocker);
        String o = Usernames.normalize(other);
        if (self == null || o == null || self.equals(o)) {
            return false;
        }
        String[] pair = canonical(self, o);
        Optional<FriendshipEntity> existing = repository.findByUserAAndUserB(pair[0], pair[1]);
        FriendshipEntity entity = existing.orElseGet(() ->
                new FriendshipEntity(pair[0], pair[1], FriendshipStatus.BLOCKED, self, Instant.now()));
        entity.block(self, Instant.now());
        repository.save(entity);
        broadcaster.publish(new SocialEvent.FriendshipUpdated(pair[0], pair[1], FriendshipStatus.BLOCKED));
        return true;
    }

    @Override
    @Transactional
    public boolean unblock(String actor, String other) {
        String self = Usernames.normalize(actor);
        String o = Usernames.normalize(other);
        if (self == null || o == null) {
            return false;
        }
        String[] pair = canonical(self, o);
        Optional<FriendshipEntity> existing = repository.findByUserAAndUserB(pair[0], pair[1]);
        if (existing.isEmpty()) {
            return false;
        }
        FriendshipEntity entity = existing.get();
        if (entity.getStatus() != FriendshipStatus.BLOCKED || !self.equals(entity.getRequestedBy())) {
            return false;
        }
        repository.delete(entity);
        broadcaster.publish(new SocialEvent.FriendshipUpdated(pair[0], pair[1], null));
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Friendship> statusBetween(String user1, String user2) {
        return lookupStatus(user1, user2);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean areFriends(String user1, String user2) {
        return lookupStatus(user1, user2)
                .map(f -> f.status() == FriendshipStatus.ACCEPTED)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlockedBetween(String user1, String user2) {
        return lookupStatus(user1, user2)
                .map(f -> f.status() == FriendshipStatus.BLOCKED)
                .orElse(false);
    }

    private Optional<Friendship> lookupStatus(String user1, String user2) {
        String a = Usernames.normalize(user1);
        String b = Usernames.normalize(user2);
        if (a == null || b == null) {
            return Optional.empty();
        }
        String[] pair = canonical(a, b);
        return repository.findByUserAAndUserB(pair[0], pair[1]).map(this::toValue);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Friendship> incomingRequests(String username) {
        String self = Usernames.normalize(username);
        if (self == null) {
            return List.of();
        }
        return repository.findByUserAndStatus(self, FriendshipStatus.PENDING).stream()
                .filter(e -> !self.equals(e.getRequestedBy()))
                .map(this::toValue)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Friendship> friendsOf(String username) {
        String self = Usernames.normalize(username);
        if (self == null) {
            return List.of();
        }
        return repository.findByUserAndStatus(self, FriendshipStatus.ACCEPTED).stream()
                .map(this::toValue)
                .toList();
    }

    private String[] canonical(String a, String b) {
        return a.compareTo(b) < 0 ? new String[]{a, b} : new String[]{b, a};
    }

    private Friendship toValue(FriendshipEntity e) {
        return new Friendship(e.getUserA(), e.getUserB(), e.getStatus(), e.getRequestedBy());
    }
}
