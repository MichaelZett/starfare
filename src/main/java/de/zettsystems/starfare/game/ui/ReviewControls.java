package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.i18n.I18n;

import java.util.List;

/** View-local selection; never changes the game or the account's seat. */
final class ReviewControls extends HorizontalLayout {
    private final ComboBox<Player> perspective = new ComboBox<>(I18n.t(UiTexts.REVIEW_PERSPECTIVE));
    private final Checkbox fog = new Checkbox(I18n.t(UiTexts.REVIEW_FOG));
    private boolean initialized;

    ReviewControls(Runnable onChange) {
        setVisible(false);
        setWidthFull();
        addClassName("review-controls");
        setWrap(true);
        setAlignItems(Alignment.BASELINE);
        perspective.setId("review-perspective");
        perspective.setItemLabelGenerator(Player::name);
        perspective.setAllowCustomValue(false);
        perspective.setClearButtonVisible(false);
        fog.setId("review-fog");
        add(perspective, fog);
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
    }

    void reset() {
        initialized = false;
        perspective.clear();
        fog.setValue(false);
        setVisible(false);
    }

    void show(List<Player> players, int ownSeat) {
        if (!initialized) {
            perspective.setItems(players);
            players.stream().filter(player -> player.id() == ownSeat).findFirst()
                    .or(() -> players.stream().findFirst()).ifPresent(perspective::setValue);
            initialized = true;
        }
        setVisible(true);
    }

    int perspective() {
        Player selected = perspective.getValue();
        return selected == null ? -1 : selected.id();
    }

    boolean fogOfWar() { return fog.getValue(); }
}
