package de.zettsystems.starfare.economy.values;

import de.zettsystems.starfare.game.values.RelocationAdjustment;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * One map view's short-lived production input: the value being dragged, the focused control and the
 * latest save feedback. Every completed input is saved at once, so nothing here outlives a server
 * update except the view state.
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
        private int turn;
        private @Nullable Integer focus;
        private Status status = Status.NONE;
        private List<RelocationAdjustment> relocations = List.of();

        private Input(int turn, IndustryView industry) {
            this.turn = turn; this.industry = industry;
            saved = AllocationSplit.saved(industry); allocation = saved;
        }
        public AllocationSplit allocation() { return allocation; }
        public AllocationSplit saved() { return saved; }
        public IndustryView industry() { return industry; }
        public int turn() { return turn; }
        public boolean changed() { return !allocation.equals(saved); }
        public @Nullable Integer focus() { return focus; }
        public void focus(@Nullable Integer control) { focus = control; }
        public Status status() { return status; }
        public List<RelocationAdjustment> relocations() { return relocations; }
        /** Sets the expansion share; shipbuilding receives the rest of the capacity. */
        public void expansion(int value) {
            allocation = allocation.adjust(AllocationSplit.EXPANSION, value, AllocationSplit.limits(industry));
            status = Status.NONE;
        }
        public boolean valid() {
            int[] limits = AllocationSplit.limits(industry);
            return allocation.total() == industry.capacity()
                    && allocation.ships() <= limits[AllocationSplit.SHIPS]
                    && allocation.expansion() <= limits[AllocationSplit.EXPANSION];
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
            saved = AllocationSplit.saved(current); allocation = saved;
        }
    }
}
