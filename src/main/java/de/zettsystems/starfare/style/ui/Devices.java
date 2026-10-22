package de.zettsystems.starfare.style.ui;

import com.vaadin.flow.server.VaadinSession;
import de.zettsystems.starfare.style.values.DeviceLayout;

/** Session-wide layout choice; detected once from the browser, overridable per session. */
public final class Devices {
    static final String SESSION_KEY = "starfare.deviceLayout";

    private Devices() { }

    public static DeviceLayout current() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) { return DeviceLayout.DESKTOP; }
        if (session.getAttribute(SESSION_KEY) instanceof DeviceLayout stored) { return stored; }
        var browser = session.getBrowser();
        DeviceLayout detected = DeviceLayout.fromUserAgent(browser == null ? null : browser.getBrowserApplication());
        session.setAttribute(SESSION_KEY, detected);
        return detected;
    }

    public static boolean mobile() { return current() == DeviceLayout.MOBILE; }

    static void choose(DeviceLayout layout) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) { session.setAttribute(SESSION_KEY, layout); }
    }
}
