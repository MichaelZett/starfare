package de.zettsystems.starfare.report.values;

/** Kind of a SectorForces news item after Galactic Empire's "Hyperraum-Ticker". */
public enum CosmicNews {
    /** Every fleet in hyperspace arrives one round later. */
    STORM,
    /** Every fleet in hyperspace has its remaining travel time halved. */
    SPACETIME_QUAKE,
    /** An own fleet lost ships without a trace; {@code ships} lost, {@code remaining} still flying. */
    DISAPPEARANCE,
    /** A quake destroyed less than half of the garrison. */
    SPACEPORT_QUAKE_LIGHT,
    /** A quake destroyed at least half of the garrison. */
    SPACEPORT_QUAKE_HEAVY,
    /** The sun turned unstable and explodes when {@code round} is resolved. */
    SUN_UNSTABLE,
    /** The system is destroyed for good. */
    SUPERNOVA,
    /** The own garrison fled the supernova towards {@code targetName}. */
    REFUGEES,
    /** The neutral garrison fled the supernova as pirates towards {@code targetName}. */
    PIRATES_FLEE,
    /** An own fleet found its target destroyed and jumped to {@code targetName}. */
    PANIC_JUMP,
    /** Pirates took the own system; it is neutral now. */
    PIRATE_RAID_WON,
    /** The own system fought off pirates. */
    PIRATE_RAID_REPELLED,
    /** The game ends after {@code round}. */
    ROUND_LIMIT_NEAR
}
