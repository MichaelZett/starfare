package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.game.values.GameId;

/** Persistent personal choice; the game setup supplies the default until the first explicit choice. */
final class BattlePresentationPreference {

    private BattlePresentationPreference() {
    }

    static boolean enabled(GameId gameId, int reportTurn, boolean defaultValue) {
        return speed(gameId, reportTurn, defaultValue) > 0;
    }

    static double speed(GameId gameId, int reportTurn, boolean defaultValue) {
        return DisplayPreferences.number(de.zettsystems.starfare.social.values.DisplaySetting.SPEED, defaultValue ? 1.0 : 0.0);
    }

    static void set(GameId gameId, int reportTurn, boolean enabled) {
        setSpeed(gameId, reportTurn, enabled ? 1.0 : 0.0);
    }

    static void setSpeed(GameId gameId, int reportTurn, double speed) {
        BattleReplayDialog.setSpeed(speed);
    }

}
