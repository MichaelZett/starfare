package de.zettsystems.starfare.i18n;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;

import java.util.Locale;

/**
 * Static facade for translation lookups. Prefer Component#getTranslation inside Vaadin components;
 * use this helper from plain utility classes or when no component is at hand.
 */
public final class I18n {

    private I18n() {
    }

    public static String t(String key, Object... params) {
        UI ui = UI.getCurrent();
        if (ui != null) {
            return ui.getTranslation(key, params);
        }
        VaadinService service = VaadinService.getCurrent();
        Locale locale = service != null ? service.getContext().getAttribute(Locale.class) : null;
        StarfareI18NProvider provider = new StarfareI18NProvider();
        return provider.getTranslation(key, locale == null ? StarfareI18NProvider.GERMAN : locale, params);
    }
}
