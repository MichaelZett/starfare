package de.zettsystems.starfare.economy.ui;

/** Shared visible text keys for production planning. */
public final class ProductionKeys {
    private ProductionKeys() { }
    private static final String PREFIX = "production.";
    public static final String SHIPS = PREFIX + "preset.ships";
    public static final String METAL = PREFIX + "preset.metal";
    public static final String INDUSTRY = PREFIX + "preset.industry";
    public static final String EVEN = PREFIX + "preset.even";
    public static final String AUTARK = PREFIX + "preset.autark";
    public static final String LOCKED = PREFIX + "rejection.locked";
    public static final String NO_CAPACITY = PREFIX + "rejection.noCapacity";
    public static final String LIMIT = PREFIX + "rejection.limit";
    public static final String EMPTY = PREFIX + "rejection.empty";
    public static final String SMALL = PREFIX + "rejection.small";
    public static final String PINNED = PREFIX + "pinned";
    public static final String INVALID = PREFIX + "invalid";
    public static final String DELIVERY = PREFIX + "issue.delivery";
    public static final String SHORTAGE = PREFIX + "issue.shortage";
    public static final String DEPOSIT = PREFIX + "issue.deposit";
    public static final String LABOR = PREFIX + "issue.labor";
    public static final String CLEAR = PREFIX + "clear";
    public static final String CHANGE = PREFIX + "change";
    public static final String COLONY = PREFIX + "colony";
    public static final String OUTCOME_SHIPS = PREFIX + "outcome.ships";
    public static final String OUTCOME_SHIPS_SHORT = PREFIX + "outcome.shipsShort";
    public static final String OUTCOME_METAL = PREFIX + "outcome.metal";
    public static final String OUTCOME_METAL_LIMITED = PREFIX + "outcome.metalLimited";
    public static final String OUTCOME_EXPANSION = PREFIX + "outcome.expansion";
    public static final String MINING_YIELD = PREFIX + "miningYield";
    public static final String SAVED = PREFIX + "saved";
    public static final String NOT_SAVED = PREFIX + "notSaved";
    public static final String RELOCATION_SHORTENED = PREFIX + "relocation.shortened";
    public static final String RELOCATION_REMOVED = PREFIX + "relocation.removed";
    public static final String LOGISTICS_YIELD = PREFIX + "logistics.yield";
    public static final String LOGISTICS_SORT_YIELD = PREFIX + "logistics.sortYield";
    public static String preset(de.zettsystems.starfare.economy.values.ProductionPreset preset) {
        return switch (preset) { case SHIPS -> SHIPS; case METAL -> METAL; case INDUSTRY -> INDUSTRY;
            case EVEN -> EVEN; case AUTARK -> AUTARK; };
    }
    public static String rejection(de.zettsystems.starfare.economy.values.ProductionPreset.Rejection rejection) {
        return switch (rejection) { case LOCKED -> LOCKED; case NO_CAPACITY -> NO_CAPACITY;
            case LIMIT -> LIMIT; case EMPTY_DEPOSIT -> EMPTY; case TOO_SMALL -> SMALL; case NONE -> SHIPS; };
    }
}
