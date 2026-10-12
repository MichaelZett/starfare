package de.zettsystems.starfare.economy.values;

/** Fixed trial values for spaceward/1.2.0-w3. */
public final class RecyclingRules {
    public static final int METAL_SCALE = 10;
    public static final int RETURN_TENTHS = 7;
    public static final int LOSS_TENTHS = METAL_SCALE - RETURN_TENTHS;
    public static final int AI_REAR_GARRISON = 8;
    private RecyclingRules() { }
}
