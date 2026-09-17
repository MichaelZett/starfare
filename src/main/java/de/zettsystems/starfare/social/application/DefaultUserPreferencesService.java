package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.domain.UserPreferencesEntity;
import de.zettsystems.starfare.social.values.PlayerIds;
import de.zettsystems.starfare.social.values.SocialEvent;
import de.zettsystems.starfare.social.values.Visibility;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class DefaultUserPreferencesService implements UserPreferencesService {

    private final UserPreferencesRepository repository;
    private final SocialBroadcaster broadcaster;

    public DefaultUserPreferencesService(UserPreferencesRepository repository, SocialBroadcaster broadcaster) {
        this.repository = repository;
        this.broadcaster = broadcaster;
    }

    @Override
    @Transactional(readOnly = true)
    public Visibility getVisibility(String playerId) {
        String name = PlayerIds.normalize(playerId);
        if (name == null) {
            return Visibility.ALL;
        }
        return repository.findById(name).map(UserPreferencesEntity::getVisibility).orElse(Visibility.ALL);
    }

    @Override
    @Transactional
    public void setVisibility(String playerId, Visibility visibility) {
        String name = PlayerIds.normalize(playerId);
        if (name == null || visibility == null) {
            return;
        }
        Instant now = Instant.now();
        UserPreferencesEntity entity = repository.findById(name)
                .orElseGet(() -> new UserPreferencesEntity(name, visibility, now));
        entity.changeVisibility(visibility, now);
        repository.save(entity);
        broadcaster.publish(new SocialEvent.VisibilityUpdated(name, visibility));
    }
}
