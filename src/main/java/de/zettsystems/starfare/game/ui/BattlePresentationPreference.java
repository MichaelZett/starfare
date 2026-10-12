package de.zettsystems.starfare.game.ui;


/** Persistent personal choice; the game setup supplies the default until the first explicit choice. */
final class BattlePresentationPreference {

    private BattlePresentationPreference() {
    }

    static boolean enabled(boolean defaultValue) {
        return speed(defaultValue) > 0;
    }

    static double speed(boolean defaultValue) {
        return DisplayPreferences.number(de.zettsystems.starfare.social.values.DisplaySetting.SPEED, defaultValue ? 1.0 : 0.0);
    }

    static void set(boolean enabled) {
        setSpeed(enabled ? 1.0 : 0.0);
    }

    static void setSpeed(double speed) {
        BattleReplayDialog.setSpeed(speed);
    }

}
