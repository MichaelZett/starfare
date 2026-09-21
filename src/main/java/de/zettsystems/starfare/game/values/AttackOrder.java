package de.zettsystems.starfare.game.values;

/** In welcher Reihenfolge mehrere Angreifer in derselben Runde auf ein System treffen. */
public enum AttackOrder {
    /** Jede Runde neu ausgelost; kein Angreifer kann sich auf seinen Platz verlassen. */
    RANDOM,
    /** Der stärkste Verband zuerst, bei Gleichstand die kleinere Spieler-ID. */
    STRONGEST_FIRST
}
