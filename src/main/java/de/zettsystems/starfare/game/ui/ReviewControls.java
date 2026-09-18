package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.i18n.I18n;

import java.util.List;

/** View-local selection; never changes the game or the account's seat. */
final class ReviewControls extends HorizontalLayout {
    private final ComboBox<Player> perspective = new ComboBox<>(I18n.t(UiTexts.REVIEW_PERSPECTIVE));
    private final Checkbox fog = new Checkbox(I18n.t(UiTexts.REVIEW_FOG));
    private final Input timeline = new Input();
    private final Span timelineLabel = new Span();
    private boolean initialized;
    private int latestReplayTurn = -1;

    ReviewControls(Runnable onChange) {
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
        add(perspective, fog, timelineLabel, timeline);
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
            updateTimelineLabel();
            fog.setVisible(replayTurn() < 0);
            if (event.isFromClient()) { onChange.run(); }
        });
    }

    void reset() {
        initialized = false;
        perspective.clear();
        fog.setValue(false);
        timeline.setValue("");
        latestReplayTurn = -1;
        timeline.setVisible(false);
        timelineLabel.setVisible(false);
        setVisible(false);
    }

    void show(List<Player> players, int ownSeat, List<Integer> turns) {
        if (!initialized) {
            perspective.setItems(players);
            players.stream().filter(player -> player.id() == ownSeat).findFirst()
                    .or(() -> players.stream().findFirst()).ifPresent(perspective::setValue);
            initialized = true;
        }
        configureTimeline(turns);
        setVisible(true);
    }

    int perspective() {
        Player selected = perspective.getValue();
        return selected == null ? -1 : selected.id();
    }

    boolean fogOfWar() { return fog.getValue(); }

    int replayTurn() {
        try {
            int selected = Integer.parseInt(timeline.getValue());
            return selected <= latestReplayTurn ? selected : -1;
        } catch (NumberFormatException _) {
            return -1;
        }
    }

    private void configureTimeline(List<Integer> turns) {
        boolean available = !turns.isEmpty();
        timeline.setVisible(available);
        timelineLabel.setVisible(available);
        fog.setVisible(!available);
        if (!available) {
            return;
        }
        int first = turns.getFirst();
        int last = turns.getLast();
        latestReplayTurn = last;
        timeline.getElement().setAttribute("min", String.valueOf(first));
        timeline.getElement().setAttribute("max", String.valueOf(last + 1));
        if (timeline.getValue().isBlank()) {
            timeline.setValue(String.valueOf(last + 1));
        }
        fog.setVisible(replayTurn() < 0);
        updateTimelineLabel();
    }

    private void updateTimelineLabel() {
        int selectedTurn = replayTurn();
        timelineLabel.setText(selectedTurn < 0
                ? I18n.t(UiTexts.REPLAY_TIMELINE_FINAL)
                : I18n.t(UiTexts.REPLAY_TIMELINE_TURN, selectedTurn));
    }
}
