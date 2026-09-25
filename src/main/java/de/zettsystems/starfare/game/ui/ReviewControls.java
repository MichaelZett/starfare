package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.game.values.ReplayEventMarker;
import de.zettsystems.starfare.i18n.I18n;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/** View-local selection; never changes the game or the account's seat. */
final class ReviewControls extends HorizontalLayout {
    private final ComboBox<Player> perspective = new ComboBox<>(I18n.t(UiTexts.REVIEW_PERSPECTIVE));
    private final Checkbox fog = new Checkbox(I18n.t(UiTexts.REVIEW_FOG));
    private final Input timeline = new Input();
    private final Span timelineLabel = new Span();
    private final Span replayNotice = new Span();
    private final HorizontalLayout eventMarks = new HorizontalLayout();
    private final Button previous = new Button(I18n.t(UiTexts.REPLAY_TIMELINE_PREVIOUS));
    private final Button next = new Button(I18n.t(UiTexts.REPLAY_TIMELINE_NEXT));
    private final Button nextEvent = new Button(I18n.t(UiTexts.REPLAY_NEXT_EVENT));
    private final Runnable onChange;
    private final Consumer<ReplayEventMarker> onEventSelected;
    private List<Integer> turns = List.of();
    private List<ReplayEventMarker> markers = List.of();
    private @Nullable ReplayEventMarker currentEvent;
    private boolean initialized;
    private int latestReplayTurn = -1;

    ReviewControls(Runnable onChange, Consumer<ReplayEventMarker> onEventSelected) {
        this.onChange = onChange;
        this.onEventSelected = onEventSelected;
        setVisible(false);
        setWidthFull();
        addClassName("review-controls");
        setWrap(true);
        setAlignItems(Alignment.BASELINE);
        perspective.setId("review-perspective");
        perspective.setItemLabelGenerator(Player::label);
        perspective.setAllowCustomValue(false);
        perspective.setClearButtonVisible(false);
        fog.setId("review-fog");
        timeline.setType("range");
        timeline.setId("review-timeline");
        timeline.getElement().setAttribute("min", "1");
        timeline.getElement().setAttribute("step", "1");
        replayNotice.addClassName("review-notice");
        eventMarks.addClassName("review-event-marks");
        previous.addClickListener(_ -> moveTimeline(-1));
        next.addClickListener(_ -> moveTimeline(1));
        nextEvent.addClickListener(_ -> moveToNextEvent());
        add(perspective, fog, previous, timelineLabel, timeline, next, nextEvent, replayNotice, eventMarks);
        perspective.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                if (event.getValue() == null) {
                    perspective.setValue(event.getOldValue());
                } else {
                    onChange.run();
                }
            }
        });
        fog.addValueChangeListener(event -> {
            if (event.isFromClient()) { onChange.run(); }
        });
        timeline.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                snapToAvailableTurn();
                updateTimelineLabel();
                fog.setVisible(replayTurn() < 0);
                onChange.run();
            } else {
                updateTimelineLabel();
            }
        });
    }

    void reset() {
        initialized = false;
        perspective.clear();
        fog.setValue(false);
        timeline.setValue("");
        turns = List.of();
        markers = List.of();
        currentEvent = null;
        latestReplayTurn = -1;
        timeline.setVisible(false);
        timelineLabel.setVisible(false);
        previous.setVisible(false);
        next.setVisible(false);
        replayNotice.setVisible(false);
        eventMarks.setVisible(false);
        setVisible(false);
    }

    void show(List<Player> players, int ownSeat, List<Integer> availableTurns, List<ReplayEventMarker> markers) {
        if (!initialized) {
            perspective.setItems(players);
            players.stream().filter(player -> player.id() == ownSeat).findFirst()
                    .or(() -> players.stream().findFirst()).ifPresent(perspective::setValue);
            initialized = true;
        }
        List<Integer> sortedTurns = availableTurns.stream().sorted().toList();
        if (!turns.equals(sortedTurns) || !this.markers.equals(markers)) {
            currentEvent = null;
        }
        this.markers = List.copyOf(markers);
        configureTimeline(availableTurns);
        renderEventMarks(markers);
        setVisible(true);
    }

    void setReviewingVisible(boolean reviewing) { setVisible(reviewing); }

    int perspective() {
        Player selected = perspective.getValue();
        return selected == null ? -1 : selected.id();
    }

    boolean fogOfWar() { return fog.getValue(); }

    int replayTurn() {
        try {
            long selected = Long.parseLong(timeline.getValue());
            return selected <= latestReplayTurn ? (int) selected : -1;
        } catch (NumberFormatException _) {
            return -1;
        }
    }

    private void configureTimeline(List<Integer> availableTurns) {
        turns = availableTurns.stream().sorted().toList();
        boolean available = !turns.isEmpty();
        timeline.setVisible(available);
        timelineLabel.setVisible(available);
        previous.setVisible(available);
        next.setVisible(available);
        fog.setVisible(!available);
        replayNotice.setVisible(true);
        if (!available) {
            nextEvent.setVisible(false);
            replayNotice.setText(I18n.t(UiTexts.REPLAY_UNAVAILABLE));
            eventMarks.setVisible(false);
            return;
        }
        int first = turns.getFirst();
        int last = turns.getLast();
        latestReplayTurn = last;
        timeline.getElement().setAttribute("min", String.valueOf(first));
        timeline.getElement().setAttribute("max", String.valueOf(last + 1L));
        if (timeline.getValue().isBlank()) {
            timeline.setValue(String.valueOf(last + 1L));
        } else {
            snapToAvailableTurn();
        }
        fog.setVisible(replayTurn() < 0);
        updateTimelineLabel();
    }

    private void renderEventMarks(List<ReplayEventMarker> markers) {
        eventMarks.removeAll();
        for (ReplayEventMarker marker : markers) {
            String icon = marker.conquest() ? "⚑" : "⚔";
            Button mark = new Button(I18n.t(UiTexts.REPLAY_EVENT_MARK, icon, marker.turn(), marker.systemName()));
            mark.addClassName("review-event-mark");
            mark.addClickListener(_ -> selectEvent(marker));
            eventMarks.add(mark);
        }
        nextEvent.setVisible(!markers.isEmpty());
        eventMarks.setVisible(!markers.isEmpty());
    }

    private void moveToNextEvent() {
        if (markers.isEmpty()) { return; }
        int nextIndex = currentEvent == null
                ? firstEventIndexAtOrAfter(replayTurn()) : markers.indexOf(currentEvent) + 1;
        if (nextIndex >= 0 && nextIndex < markers.size()) { selectEvent(markers.get(nextIndex)); }
    }

    private int firstEventIndexAtOrAfter(int turn) {
        for (int index = 0; index < markers.size(); index++) {
            if (turn < 0 || markers.get(index).turn() >= turn) { return index; }
        }
        return markers.size();
    }

    private void selectEvent(ReplayEventMarker marker) {
        currentEvent = marker;
        timeline.setValue(String.valueOf(marker.turn()));
        updateTimelineLabel();
        fog.setVisible(false);
        onEventSelected.accept(marker);
    }

    private void snapToAvailableTurn() {
        if (turns.isEmpty()) { return; }
        try {
            int value = Integer.parseInt(timeline.getValue());
            int last = turns.getLast();
            if (turns.contains(value) || value == last + 1) { return; }
            if (value > last) {
                currentEvent = null;
                timeline.setValue(String.valueOf(last + 1L));
                return;
            }
            int nearest = turns.stream().min((left, right) -> {
                long leftDistance = Math.abs((long) left - value);
                long rightDistance = Math.abs((long) right - value);
                return Long.compare(leftDistance, rightDistance);
            }).orElse(last);
            currentEvent = null;
            timeline.setValue(String.valueOf(nearest));
        } catch (NumberFormatException _) {
            timeline.setValue(String.valueOf(turns.getLast() + 1L));
        }
    }

    private void moveTimeline(int delta) {
        if (turns.isEmpty()) { return; }
        int index = replayTurn() < 0 ? turns.size() : turns.indexOf(replayTurn());
        long candidate = (long) index + delta;
        int nextIndex = (int) Math.clamp(candidate, 0L, turns.size());
        String value = nextIndex == turns.size() ? String.valueOf(turns.getLast() + 1L)
                : String.valueOf(turns.get(nextIndex));
        timeline.setValue(value);
        currentEvent = null;
        updateTimelineLabel();
        fog.setVisible(replayTurn() < 0);
        onChange.run();
    }

    private void updateTimelineLabel() {
        int selectedTurn = replayTurn();
        timelineLabel.setText(selectedTurn < 0
                ? I18n.t(UiTexts.REPLAY_TIMELINE_FINAL)
                : I18n.t(UiTexts.REPLAY_TIMELINE_TURN, selectedTurn));
        replayNotice.setText(I18n.t(selectedTurn < 0
                ? UiTexts.REPLAY_FINAL_NOTICE : UiTexts.REPLAY_HISTORICAL_NOTICE));
    }
}
