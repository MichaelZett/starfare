package de.zettsystems.starfare.social.ui;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.social.application.UserPreferencesService;
import de.zettsystems.starfare.social.values.Visibility;

/**
 * Lobby-header widget letting the current user change their visibility preference. Persisted via
 * {@link UserPreferencesService}; the reload is triggered by the parent page so the filtered online
 * list updates immediately.
 */
public class VisibilityMenu extends HorizontalLayout {

    public VisibilityMenu(UserPreferencesService preferences, Runnable afterChange) {
        addClassName("visibility-menu");
        setSpacing(true);
        setPadding(false);
        setDefaultVerticalComponentAlignment(Alignment.CENTER);

        Span label = new Span(I18n.t(UiTexts.VISIBILITY_LABEL));
        label.addClassName("visibility-label");

        ComboBox<Visibility> combo = new ComboBox<>();
        combo.setItems(Visibility.values());
        combo.setItemLabelGenerator(VisibilityMenu::translate);
        combo.setAllowCustomValue(false);

        String viewer = UserContext.currentUsername().orElse(null);
        Visibility current = viewer == null ? Visibility.ALL : preferences.getVisibility(viewer);
        combo.setValue(current);

        combo.addValueChangeListener(event -> {
            Visibility next = event.getValue();
            String name = UserContext.currentUsername().orElse(null);
            if (next == null || name == null) {
                return;
            }
            preferences.setVisibility(name, next);
            if (afterChange != null) {
                afterChange.run();
            }
        });

        add(label, combo);
    }

    private static String translate(Visibility v) {
        return switch (v) {
            case ALL -> I18n.t(UiTexts.VISIBILITY_ALL);
            case FRIENDS_ONLY -> I18n.t(UiTexts.VISIBILITY_FRIENDS_ONLY);
            case NONE -> I18n.t(UiTexts.VISIBILITY_NONE);
        };
    }
}
