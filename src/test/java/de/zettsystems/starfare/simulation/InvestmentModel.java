package de.zettsystems.starfare.simulation;

import de.zettsystems.starfare.economy.domain.Industry;
import de.zettsystems.starfare.economy.values.SystemQuality;

/** Counterfactual prices only; no production rule or saved game is changed. */
enum InvestmentModel {
    BASELINE, HALF_COST, EARLY_COST, QUALITY_COST, HALF_CAPTURE;

    int cost(int capacity, SystemQuality quality) {
        int early = Math.min(2 * capacity, Math.max(2, Math.ceilDiv(capacity * capacity, 10)));
        return switch (this) {
            case BASELINE, HALF_CAPTURE -> 2 * capacity;
            case HALF_COST -> capacity;
            case EARLY_COST -> early;
            case QUALITY_COST -> Math.min(2 * capacity, Math.ceilDiv(early * switch (quality.suitability()) {
                case FERTILE -> 3;
                case HABITABLE -> 4;
                case HARSH -> 5;
            }, 4));
        };
    }

    Industry grow(Industry current, SystemQuality quality) {
        if (current.capacity() == 50 || current.expansionAllocation() == 0) { return current; }
        int capacity = current.capacity();
        int progress = current.expansionProgress() + current.expansionAllocation();
        while (capacity < 50 && progress >= cost(capacity, quality)) {
            progress -= cost(capacity, quality);
            capacity++;
        }
        return new Industry(capacity, capacity == 50 ? 0 : current.expansionAllocation(), progress);
    }
}
