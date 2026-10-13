package de.zettsystems.starfare.economy.values;

/** Current stock and the empire-wide next-round forecast, in whole metal units. */
public record MetalBalance(long current, long extraction, long recycling, long consumption, long next) {
    /** Net stock change next round; equals extraction plus recycling minus consumption after rounding. */
    public long change() { return next - current; }

    /** Signed change for compact display, e.g. "(+3)", "(−3)" or "(±0)" with a true minus sign. */
    public String formattedChange() {
        long change = change();
        if (change == 0) { return "(±0)"; }
        return change > 0 ? "(+" + change + ")" : "(\u2212" + Math.abs(change) + ")";
    }
}
