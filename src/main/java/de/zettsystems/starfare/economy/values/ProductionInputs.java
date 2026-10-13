package de.zettsystems.starfare.economy.values;

import de.zettsystems.starfare.game.values.RelocationAdjustment;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * One map view's short-lived production input: the value being dragged, local pins, the
 * opened editor, the focused control and the latest save feedback. Every completed input is
 * saved at once, so nothing here outlives a server update except pins and view state.
 */
public final class ProductionInputs {
    private final Map<Integer, Input> inputs = new HashMap<>();
    private String game = "";

    public void reconcile(String gameId, int turn, Map<Integer, IndustryView> allowed) {
        if (!game.equals(gameId)) { inputs.clear(); game = gameId; }
        inputs.keySet().retainAll(allowed.keySet());
        allowed.forEach((id, industry) -> {
            Input existing = inputs.get(id);
            if (existing != null) { existing.observe(turn, industry); }
        });
    }

    public Input forSystem(int system, int turn, IndustryView industry) {
        return inputs.computeIfAbsent(system, _ -> new Input(turn, industry));
    }

    /** Save feedback: confirmation including shortened relocations, or a rejected change. */
    public enum Status { NONE, SAVED, NOT_SAVED }

    public static final class Input {
        private AllocationSplit saved;
        private AllocationSplit allocation;
        private IndustryView industry;
        private final Set<Integer> locks = new HashSet<>();
        private int turn;
        private boolean expanded;
        private @Nullable Integer focus;
        private Status status = Status.NONE;
        private List<RelocationAdjustment> relocations = List.of();

        private Input(int turn, IndustryView industry) {
            this.turn = turn; this.industry = industry;
            saved = ProductionPreset.saved(industry); allocation = saved;
        }
        public AllocationSplit allocation() { return allocation; }
        public AllocationSplit saved() { return saved; }
        public IndustryView industry() { return industry; }
        public Set<Integer> locks() { return Set.copyOf(locks); }
        public int turn() { return turn; }
        public boolean changed() { return !allocation.equals(saved); }
        public boolean expanded() { return expanded; }
        public void expand() { expanded = true; }
        public @Nullable Integer focus() { return focus; }
        public void focus(@Nullable Integer index) { focus = index; }
        public Status status() { return status; }
        public List<RelocationAdjustment> relocations() { return relocations; }
        public void pin(int index, boolean pinned) { if (pinned) { locks.add(index); } else { locks.remove(index); } }
        public void choose(AllocationSplit value) { allocation = value; expanded = true; status = Status.NONE; }
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
        /** The server accepted the current allocation; it is the expected base of the next save. */
        public void accepted(List<RelocationAdjustment> shortened) {
            saved = allocation; status = Status.SAVED; relocations = List.copyOf(shortened);
        }
        /** The server refused: show the stored plan again, never the refused wish. */
        public void rejected() { allocation = saved; status = Status.NOT_SAVED; relocations = List.of(); }
        /** A server update always wins; a new round also clears the previous feedback. */
        public void observe(int currentTurn, IndustryView current) {
            if (currentTurn != turn) { status = Status.NONE; relocations = List.of(); focus = null; }
            turn = currentTurn; industry = current;
            saved = ProductionPreset.saved(current); allocation = saved;
        }
    }
}
