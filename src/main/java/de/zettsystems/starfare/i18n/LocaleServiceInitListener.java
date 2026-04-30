package de.zettsystems.starfare.i18n;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Applies the session-stored locale to every newly created {@code UI}, so the
 * language survives navigation and page reloads (after the switcher has written
 * it). Without this, Vaadin would always fall back to the browser locale.
 */
@Component
public class LocaleServiceInitListener implements VaadinServiceInitListener {

    static final String SESSION_KEY = "starfare.locale";

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent -> {
            VaadinSession session = VaadinSession.getCurrent();
            if (session == null) {
                return;
            }
            Object stored = session.getAttribute(SESSION_KEY);
            if (stored instanceof Locale locale) {
                uiEvent.getUI().setLocale(locale);
            }
        });
    }
}
