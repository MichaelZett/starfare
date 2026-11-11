package de.zettsystems.starfare.i18n;

import com.vaadin.flow.i18n.I18NProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

/**
 * Reads Starfare's own bundle {@code games/starfare/i18n/translations}. Starfare resolves its
 * texts through {@link I18n}, not through Vaadin's global provider, which belongs to the starting
 * application. Starfare's own start registers this class as that provider (see
 * {@code StarfareApplication}); a host brings its own.
 */
@SuppressFBWarnings(value = "SE_NO_SERIALVERSIONID",
        justification = "Vaadin I18NProvider singleton; never crosses JVM boundaries, so serialVersionUID is irrelevant.")
public class StarfareI18NProvider implements I18NProvider {

    public static final Locale GERMAN = Locale.GERMAN;
    public static final Locale ENGLISH = Locale.ENGLISH;
    private static final List<Locale> LOCALES = List.of(GERMAN, ENGLISH);
    private static final String BUNDLE = "games.starfare.i18n.translations";
    private static final Control NO_DEFAULT_FALLBACK =
            Control.getNoFallbackControl(Control.FORMAT_PROPERTIES);

    @Override
    public List<Locale> getProvidedLocales() {
        return LOCALES;
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        Locale effective = locale == null ? GERMAN : locale;
        ResourceBundle bundle;
        try {
            bundle = ResourceBundle.getBundle(BUNDLE, effective, NO_DEFAULT_FALLBACK);
        } catch (MissingResourceException _) {
            return key;
        }
        String value;
        try {
            value = bundle.getString(key);
        } catch (MissingResourceException _) {
            return key;
        }
        if (params == null || params.length == 0) {
            return value;
        }
        return new MessageFormat(value, effective).format(params);
    }
}
