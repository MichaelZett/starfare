package de.zettsystems.starfare.diplomacy.values;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Local notice fields survive unrelated events, but never move to another target. */
public final class DiplomacyDrafts {
    public record Key(DiplomacyOrder.Action action, int target) { }
    private final Map<Key, Integer> values = new HashMap<>();
    private final Set<Key> opened = new java.util.HashSet<>();
    public int value(Key key, int initial) { return values.getOrDefault(key, initial); }
    public void edit(Key key, int value) { values.put(key, value); }
    public boolean opened(Key key) { return opened.contains(key); }
    public void open(Key key, boolean open) { if (open) { opened.add(key); } else { opened.remove(key); } }
    public void completed(Key key) { values.remove(key); opened.remove(key); }
    public boolean retain(Set<Key> valid) {
        boolean removed = values.keySet().stream().anyMatch(key -> !valid.contains(key));
        values.keySet().retainAll(valid); opened.retainAll(valid); return removed;
    }
}
