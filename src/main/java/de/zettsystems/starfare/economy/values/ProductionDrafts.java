package de.zettsystems.starfare.economy.values;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** One map-view's local edits. No game state, persistence, or forecasting lives here. */
public final class ProductionDrafts {
    private final Map<Integer, Draft> drafts = new HashMap<>();
    private String game = "";

    public void reconcile(String gameId, int turn, Map<Integer, IndustryView> allowed) {
        if (!game.equals(gameId)) { drafts.clear(); game = gameId; }
        drafts.keySet().retainAll(allowed.keySet());
        allowed.forEach((id, industry) -> {
            Draft existing = drafts.get(id);
            if (existing != null) { existing.observe(turn, industry); }
        });
    }

    public Draft forSystem(int system, int turn, IndustryView industry) {
        return drafts.computeIfAbsent(system, _ -> new Draft(turn, industry));
    }
    public Set<Integer> pending() {
        Set<Integer> result = new HashSet<>();
        drafts.forEach((id, draft) -> { if (draft.dirty()) { result.add(id); } });
        return Set.copyOf(result);
    }
    public void discardAll() { drafts.values().forEach(Draft::discard); }

    public static final class Draft {
        private AllocationSplit original;
        private AllocationSplit allocation;
        private IndustryView industry;
        private final Set<Integer> locks = new HashSet<>();
        private int turn;
        private int observedTurn;
        private boolean stale;
        private boolean expanded;

        private Draft(int turn, IndustryView industry) {
            this.turn = turn; this.observedTurn = turn; this.industry = industry;
            original = ProductionPreset.saved(industry); allocation = original;
        }
        public AllocationSplit allocation() { return allocation; }
        public AllocationSplit original() { return original; }
        public IndustryView industry() { return industry; }
        public Set<Integer> locks() { return Set.copyOf(locks); }
        public int turn() { return turn; }
        public boolean stale() { return stale; }
        public boolean dirty() { return !allocation.equals(original); }
        public boolean expanded() { return expanded || dirty(); }
        public void expand() { expanded = true; }
        public void collapse() { expanded = false; }
        public void pin(int index, boolean pinned) { if (pinned) { locks.add(index); } else { locks.remove(index); } }
        public void choose(AllocationSplit value) { allocation = value; expanded = true; }
        public void adjust(int index, int value) {
            boolean[] pinned = new boolean[3];
            locks.forEach(i -> pinned[i] = true);
            if (!valid()) {
                var adjusted = ProductionPreset.EVEN.apply(industry, allocation, locks);
                if (!adjusted.applicable()) { return; }
                allocation = adjusted.allocation();
            }
            choose(allocation.adjust(index, value, pinned, ProductionPreset.limits(industry)));
        }
        public boolean valid() {
            int[] limits = ProductionPreset.limits(industry);
            if (allocation.total() != industry.usableCapacity()) { return false; }
            for (int index = 0; index < 3; index++) { if (allocation.at(index) > limits[index]) { return false; } }
            return true;
        }
        public void observe(int currentTurn, IndustryView current) {
            observedTurn = currentTurn;
            AllocationSplit saved = ProductionPreset.saved(current);
            if (dirty()) { stale |= turn != currentTurn || !original.equals(saved); }
            else { original = saved; allocation = saved; turn = currentTurn; stale = false; }
            industry = current;
        }
        public boolean recheck(int currentTurn) {
            if (!valid()) { return false; }
            original = ProductionPreset.saved(industry); turn = currentTurn; stale = false;
            return true;
        }
        public void discard() { original = ProductionPreset.saved(industry); allocation = original; turn = observedTurn; stale = false; }
        public void accepted() { original = allocation; stale = false; }
    }
}
