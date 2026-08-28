package de.zettsystems.starfare.game.values;

/** Wie die Produktion neutraler Systeme über die Spanne min..max gestreut wird. */
public enum ProductionDistribution {
    /** Jeder Wert der Spanne ist gleich wahrscheinlich. */
    UNIFORM,
    /** Normalverteilt um die Mitte der Spanne; Extremwerte werden selten. */
    GAUSSIAN
}
