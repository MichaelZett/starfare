package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * The round start walks a seated player through the located news of the report before the map is free for orders:
 * battles, conquests and losses, and cosmic news that names a system. Production and arriving reinforcements are
 * routine bookkeeping and stay out, as does everything without a place.
 */
final class RoundWalkthrough {
    private RoundWalkthrough() {
    }

    /**
     * The first event of the report the player has not confirmed yet.
     *
     * @param eventIndex original index in the report, the key of the stored confirmation
     * @param systemId   where it happened; the map centres there
     * @param position   1-based place among all walkthrough events of the round
     * @param total      walkthrough events of the round
     */
    record Step(int turn, int eventIndex, TurnEvent event, int systemId, int position, int total) {
        boolean battle() {
            return event.battleSite().isPresent();
        }
    }

    /** Where an event of the walkthrough happened; empty for events the walkthrough leaves out. */
    static OptionalInt siteOf(TurnEvent event) {
        return switch (event) {
            case TurnEvent.Cosmic news -> news.system();
            // A planned joint start that was cancelled or reduced changes the player's attack.
            case TurnEvent.JointDeparture departure -> OptionalInt.of(departure.systemId());
            default -> event.battleSystemId();
        };
    }

    /** The first unconfirmed walkthrough event in report order, if any is left. */
    static Optional<Step> current(TurnReport report, Set<Integer> acknowledged) {
        List<TurnEvent> events = report.events();
        int total = 0;
        int confirmed = 0;
        int first = -1;
        for (int index = 0; index < events.size(); index++) {
            if (siteOf(events.get(index)).isEmpty()) {
                continue;
            }
            total++;
            if (acknowledged.contains(index)) {
                confirmed++;
            } else if (first < 0) {
                first = index;
            }
        }
        if (first < 0) {
            return Optional.empty();
        }
        TurnEvent event = events.get(first);
        return Optional.of(new Step(report.turn(), first, event, siteOf(event).getAsInt(), confirmed + 1, total));
    }

    /** Whether this very event still waits for the player, not merely another one at the same system. */
    static boolean isOpen(TurnReport report, int eventIndex, Set<Integer> acknowledged) {
        return eventIndex >= 0 && eventIndex < report.events().size()
                && siteOf(report.events().get(eventIndex)).isPresent()
                && !acknowledged.contains(eventIndex);
    }

    /**
     * The current event with the single action that confirms it: a battle with a replay offers the replay, every
     * other event (and every battle at instant speed) shows its result and continues. {@link RoundCurtain} holds it
     * in front of the board. Only the tutorial explains why the orders wait.
     */
    static Div card(Step step, boolean replay, boolean tutorial, Runnable action) {
        Span title = new Span(I18n.t(UiTexts.ROUND_WALKTHROUGH_TITLE, step.turn()));
        title.addClassName("round-walkthrough-title");
        Span progress = new Span(I18n.t(UiTexts.ROUND_WALKTHROUGH_PROGRESS, step.position(), step.total()));
        progress.addClassName("round-walkthrough-progress");
        Div heading = new Div();
        if (tutorial) {
            Span hint = new Span(I18n.t(UiTexts.ROUND_WALKTHROUGH_HINT));
            hint.addClassName("round-walkthrough-hint");
            heading.add(hint);
        }
        heading.add(progress);
        heading.addClassName("round-walkthrough-heading");
        Span text = new Span(RoundEventTexts.text(step.event(), replay));
        text.addClassName("round-walkthrough-text");
        Button next = new Button(I18n.t(replay ? UiTexts.ROUND_WALKTHROUGH_BATTLE : UiTexts.ROUND_WALKTHROUGH_NEXT),
                _ -> action.run());
        next.addThemeVariants(ButtonVariant.PRIMARY);
        next.addClassName("round-walkthrough-action");
        if (replay) {
            // The delegated browser click unlocks the battle sound before the server opens the replay.
            next.addClassName("event-battle-interactive");
        } else {
            next.setDisableOnClick(true);
        }
        // The board behind is inert, so the keyboard lands on the only thing to do.
        next.focus();
        Div card = new Div(title, heading, text, next);
        card.addClassName("round-walkthrough");
        return card;
    }
}
