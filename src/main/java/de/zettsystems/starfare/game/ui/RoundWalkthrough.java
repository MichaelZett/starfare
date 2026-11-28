package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import de.zettsystems.starfare.game.values.Fleet;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.report.values.CosmicNews;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * The round start walks a seated player through the located news of the report before the map is free for orders:
 * battles, conquests and losses, and cosmic news that names a system. Hyperspace news (storm, spacetime quake) has no
 * system and is shown at the player's own fleets in flight. Production and arriving reinforcements are routine
 * bookkeeping and stay out, as does everything else without a place.
 */
final class RoundWalkthrough {
    private RoundWalkthrough() {
    }

    /**
     * The first event of the report the player has not confirmed yet.
     *
     * @param eventIndex original index in the report, the key of the stored confirmation
     * @param systemId   where it happened; the map centres there. {@link #NO_SYSTEM} for hyperspace news
     * @param position   1-based place among all walkthrough events of the round
     * @param total      walkthrough events of the round
     */
    record Step(int turn, int eventIndex, TurnEvent event, int systemId, int position, int total) {
        boolean battle() {
            return event.battleSite().isPresent();
        }

        boolean hasSystem() {
            return systemId != NO_SYSTEM;
        }
    }

    /** {@link Step#systemId()} of news that happened in hyperspace, not at a system. */
    static final int NO_SYSTEM = -1;

    /**
     * What the map marks for the open event (K8e): a short label at fleets (by global id) and systems, and the fleet
     * the map centres on instead of a system. An empty label only highlights the fleet.
     */
    record MapMarks(Map<Integer, String> fleets, Map<Integer, String> systems, @Nullable Integer focusFleet) {
        static final MapMarks NONE = new MapMarks(Map.of(), Map.of(), null);

        MapMarks {
            fleets = Map.copyOf(fleets);
            systems = Map.copyOf(systems);
        }
    }

    /** Whether the walkthrough shows the event: it has a site, or it is hyperspace news. */
    static boolean walked(TurnEvent event) {
        return siteOf(event).isPresent() || hyperspaceNews(event);
    }

    private static boolean hyperspaceNews(TurnEvent event) {
        return event instanceof TurnEvent.Cosmic cosmic
                && (cosmic.news() == CosmicNews.STORM || cosmic.news() == CosmicNews.SPACETIME_QUAKE);
    }

    /**
     * The marks of the open event on the map of its player, from the player's own fleets after the round: a vanished
     * fleet shows its loss where it flies, a quaked spaceport its loss at the system, a storm and a spacetime quake
     * their effect at every own fleet in flight, refugees and fleets that jumped on are highlighted, and fleeing
     * pirates are named at the exploded system.
     */
    static MapMarks marks(@Nullable Step step, List<Fleet> ownFleets) {
        if (step == null || !(step.event() instanceof TurnEvent.Cosmic cosmic)) {
            return MapMarks.NONE;
        }
        return switch (cosmic.news()) {
            case STORM -> inFlight(ownFleets, _ -> "+1");
            case SPACETIME_QUAKE -> inFlight(ownFleets, fleet -> "R" + fleet.arrivalTurn());
            case DISAPPEARANCE -> ownFleet(ownFleets, cosmic.fleetNo())
                    .map(fleet -> new MapMarks(Map.of(fleet.globalId(), loss(cosmic)), Map.of(), fleet.globalId()))
                    .orElseGet(() -> systemMark(step, loss(cosmic)));
            case SPACEPORT_QUAKE_LIGHT, SPACEPORT_QUAKE_HEAVY -> systemMark(step, loss(cosmic));
            case REFUGEES, PANIC_JUMP -> ownFleet(ownFleets, cosmic.fleetNo())
                    .map(fleet -> new MapMarks(Map.of(fleet.globalId(), ""), Map.of(), null))
                    .orElse(MapMarks.NONE);
            case PIRATES_FLEE -> systemMark(step, I18n.t(UiTexts.MAP_EVENT_PIRATES, cosmic.ships()));
            default -> MapMarks.NONE;
        };
    }

    private static String loss(TurnEvent.Cosmic cosmic) {
        return "−" + cosmic.ships();
    }

    private static MapMarks systemMark(Step step, String label) {
        return step.hasSystem() ? new MapMarks(Map.of(), Map.of(step.systemId(), label), null) : MapMarks.NONE;
    }

    private static Optional<Fleet> ownFleet(List<Fleet> ownFleets, int localNo) {
        return localNo <= 0 ? Optional.empty()
                : ownFleets.stream().filter(fleet -> fleet.localNo() == localNo).findFirst();
    }

    /** Every own fleet in flight with its label; the map centres on the one that arrives first. */
    private static MapMarks inFlight(List<Fleet> ownFleets, java.util.function.Function<Fleet, String> label) {
        List<Fleet> flying = ownFleets.stream().filter(Fleet::inFlight)
                .sorted(Comparator.comparingInt(Fleet::arrivalTurn).thenComparingInt(Fleet::globalId)).toList();
        Map<Integer, String> fleets = new LinkedHashMap<>();
        flying.forEach(fleet -> fleets.put(fleet.globalId(), label.apply(fleet)));
        return new MapMarks(fleets, Map.of(), flying.isEmpty() ? null : flying.getFirst().globalId());
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
            if (!walked(events.get(index))) {
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
        return Optional.of(new Step(report.turn(), first, event, siteOf(event).orElse(NO_SYSTEM), confirmed + 1, total));
    }

    /** Whether this very event still waits for the player, not merely another one at the same system. */
    static boolean isOpen(TurnReport report, int eventIndex, Set<Integer> acknowledged) {
        return eventIndex >= 0 && eventIndex < report.events().size()
                && walked(report.events().get(eventIndex))
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
