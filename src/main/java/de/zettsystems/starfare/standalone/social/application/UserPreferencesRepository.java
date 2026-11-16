package de.zettsystems.starfare.standalone.social.application;

import de.zettsystems.starfare.standalone.social.domain.UserPreferencesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserPreferencesRepository extends JpaRepository<UserPreferencesEntity, String> {
}
