package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.VaadinSession;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.i18n.StarfareI18NProvider;

import java.util.List;
import java.util.Locale;

/**
 * Small header control that lets the user switch between German and English.
 * The chosen locale is persisted in {@link VaadinSession} and applied to the
 * current {@link UI} via {@link UI#setLocale(Locale)}; the page reloads so all
 * rendered strings pick up the new language.
 */
final class LanguageSwitcher extends HorizontalLayout {

    static final String SESSION_KEY = "starfare.locale";

    LanguageSwitcher() {
        ComboBox<Locale> combo = new ComboBox<>();
        combo.setItems(List.of(StarfareI18NProvider.GERMAN, StarfareI18NProvider.ENGLISH));
        combo.setItemLabelGenerator(LanguageSwitcher::labelFor);
        combo.setAllowCustomValue(false);
        combo.setClearButtonVisible(false);
        combo.setWidth("140px");
        combo.setAriaLabel(I18n.t(UiTexts.LANG_LABEL));
        combo.setValue(currentLocale());
        combo.addValueChangeListener(event -> {
            Locale selected = event.getValue();
            if (selected == null) {
                return;
            }
            VaadinSession.getCurrent().setAttribute(SESSION_KEY, selected);
            UI ui = UI.getCurrent();
            if (ui != null) {
                ui.setLocale(selected);
                ui.getPage().reload();
            }
        });

        addClassName("lang-switcher");
        setPadding(false);
        setSpacing(false);
        add(combo);
    }

    static Locale currentLocale() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            Object stored = session.getAttribute(SESSION_KEY);
            if (stored instanceof Locale locale) {
                return locale;
            }
        }
        UI ui = UI.getCurrent();
        Locale uiLocale = ui != null ? ui.getLocale() : null;
        if (uiLocale != null && "en".equals(uiLocale.getLanguage())) {
            return StarfareI18NProvider.ENGLISH;
        }
        return StarfareI18NProvider.GERMAN;
    }

    private static String labelFor(Locale locale) {
        if ("en".equals(locale.getLanguage())) {
            return I18n.t(UiTexts.LANG_EN);
        }
        return I18n.t(UiTexts.LANG_DE);
    }
}
