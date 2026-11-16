package de.zettsystems.starfare.game.application;

import de.zettsystems.starfare.game.values.DisplaySetting;

import java.util.Map;

/** A player's own display choices (speed, sound, filters, sidebar), kept across sessions. */
public interface DisplaySettingsService {

    Map<DisplaySetting, String> displaySettings(String playerId);

    /** Values longer than 512 characters and blank player ids are ignored. */
    void chooseDisplaySetting(String playerId, DisplaySetting key, String value);
}
