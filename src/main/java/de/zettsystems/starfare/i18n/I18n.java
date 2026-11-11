package de.zettsystems.starfare.i18n;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;

import java.util.Locale;

/**
 * Translation lookups for Starfare. Reads Starfare's own bundle in the language of the current UI,
 * so Starfare's texts do not depend on the I18NProvider of the application that starts it.
 */
public final class I18n {

    private static final StarfareI18NProvider BUNDLE = new StarfareI18NProvider();

    private I18n() {
    }

    public static String t(String key, Object... params) {
        return BUNDLE.getTranslation(key, currentLocale(), params);
    }

    private static Locale currentLocale() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            return ui.getLocale();
        }
        VaadinService service = VaadinService.getCurrent();
        Locale locale = service != null ? service.getContext().getAttribute(Locale.class) : null;
        return locale == null ? StarfareI18NProvider.GERMAN : locale;
    }
}
