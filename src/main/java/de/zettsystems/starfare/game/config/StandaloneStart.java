package de.zettsystems.starfare.game.config;

import org.springframework.util.ClassUtils;

/**
 * Unterscheidet Starfares eigenen Start vom Betrieb in einem Plattform-Host (games-platform).
 * Das Paket {@code standalone} gibt es nur im eigenen Start (gradle/game-module.gradle lässt es
 * im Spielartefakt weg); seine Einstiegsansicht verrät deshalb ohne Einstellung, wie die
 * Anwendung läuft. Oberfläche ({@code OwnFrame}) und Anwendungsschicht lesen beide hier.
 */
public final class StandaloneStart {
    static final String MARKER = "de.zettsystems.starfare.standalone.ui.StandaloneEntryView";
    private static final boolean ACTIVE = ClassUtils.isPresent(MARKER, StandaloneStart.class.getClassLoader());

    private StandaloneStart() {
    }

    /** {@code true} im eigenen Start, {@code false} eingebettet in einen Plattform-Host. */
    public static boolean active() {
        return ACTIVE;
    }
}
