package de.zettsystems.starfare.economy.values;

import java.util.Map;

/** Own-empire settings and expected production bases for an atomic preview/save. */
public record MiningAutomationView(int turn, boolean editable, MiningAutomation policy,
                                   Map<Integer, String> systems, Map<Integer, AllocationSplit> plans) {
    public MiningAutomationView { systems = Map.copyOf(systems); plans = Map.copyOf(plans); }
}
