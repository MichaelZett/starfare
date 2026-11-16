package de.zettsystems.starfare.standalone.social.application;

import de.zettsystems.starfare.standalone.social.values.Visibility;

public interface UserPreferencesService {

    Visibility getVisibility(String playerId);

    void setVisibility(String playerId, Visibility visibility);
}
