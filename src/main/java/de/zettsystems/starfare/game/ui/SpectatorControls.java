package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.i18n.I18n;

import java.util.List;

/** View-local spectator selection. It neither changes a seat nor stores game state. */
final class SpectatorControls extends HorizontalLayout {
    private final ComboBox<Player> perspective = new ComboBox<>(I18n.t(UiTexts.SPECTATOR_PERSPECTIVE));
    private final Checkbox fog = new Checkbox(I18n.t(UiTexts.REVIEW_FOG), true);
    private boolean initialized;

    SpectatorControls(Runnable onChange) {
        setVisible(false);
        setWidthFull();
        setWrap(true);
        setAlignItems(Alignment.BASELINE);
        addClassName("review-controls");
        perspective.setId("spectator-perspective");
        perspective.setItemLabelGenerator(Player::name);
        perspective.setAllowCustomValue(false);
        perspective.setClearButtonVisible(false);
        fog.setId("spectator-fog");
        add(perspective, fog);
        perspective.addValueChangeListener(event -> {
            if (event.isFromClient() && event.getValue() != null) {
                onChange.run();
            }
        });
        fog.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                onChange.run();
            }
        });
    }

    void show(List<Player> players) {
        if (!initialized) {
            perspective.setItems(players);
            players.stream().findFirst().ifPresent(perspective::setValue);
            initialized = true;
        }
        setVisible(true);
    }

    void reset() {
        initialized = false;
        perspective.clear();
        fog.setValue(true);
        setVisible(false);
    }

    int perspective() {
        Player selected = perspective.getValue();
        return selected == null ? -1 : selected.id();
    }

    boolean fogOfWar() {
        return fog.getValue();
    }
}
