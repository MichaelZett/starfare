package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.values.Visibility;

public interface UserPreferencesService {
    java.util.Map<de.zettsystems.starfare.social.values.DisplaySetting, String> displaySettings(String playerId);
    void chooseDisplaySetting(String playerId, de.zettsystems.starfare.social.values.DisplaySetting key, String value);

    Visibility getVisibility(String playerId);

    void setVisibility(String playerId, Visibility visibility);
}
