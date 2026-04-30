package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.domain.UserPreferencesEntity;
import de.zettsystems.starfare.social.values.SocialEvent;
import de.zettsystems.starfare.social.values.Usernames;
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
    public Visibility getVisibility(String username) {
        String name = Usernames.normalize(username);
        if (name == null) {
            return Visibility.ALL;
        }
        return repository.findById(name).map(UserPreferencesEntity::getVisibility).orElse(Visibility.ALL);
    }

    @Override
    @Transactional
    public void setVisibility(String username, Visibility visibility) {
        String name = Usernames.normalize(username);
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
