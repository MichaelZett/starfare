package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.values.Visibility;

public interface UserPreferencesService {

    Visibility getVisibility(String username);

    void setVisibility(String username, Visibility visibility);
}
