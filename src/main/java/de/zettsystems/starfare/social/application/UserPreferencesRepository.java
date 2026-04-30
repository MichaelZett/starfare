package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.domain.UserPreferencesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserPreferencesRepository extends JpaRepository<UserPreferencesEntity, String> {
}
