package de.zettsystems.starfare.style.ui;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServiceInitListener;
import de.zettsystems.starfare.style.values.DeviceLayout;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Applies a {@code ?layout=} override and loads Starfare's static stylesheets while a Starfare route
 * is open: a platform host shares one page between several games and its own pages, which must not
 * carry Starfare's rules. The phone stylesheet is loaded only for phones, so desktops never download
 * or evaluate mobile rules. The stylesheets are static resources outside the frontend bundle.
 */
@Component
public class DeviceLayoutInitListener implements VaadinServiceInitListener {
    public static final String MOBILE_STYLESHEET = "styles/starfare-mobile.css";
    static final List<String> STYLESHEETS = List.of("styles/starfare-navigation.css", "styles/starfare-wizard.css");
    private static final String STARFARE_PACKAGE = "de.zettsystems.starfare";

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent -> {
            UI ui = uiEvent.getUI();
            requested(VaadinRequest.getCurrent()).ifPresent(Devices::choose);
            ui.addBeforeEnterListener(enter -> follow(ui, ownsRoute(enter.getNavigationTarget())));
        });
    }

    /** Whether a route belongs to Starfare; in the own start every route does. */
    static boolean ownsRoute(@Nullable Class<?> target) {
        return target != null && target.getPackageName().startsWith(STARFARE_PACKAGE);
    }

    /** Loads the stylesheets on entering Starfare and removes them on leaving it, once per change. */
    static void follow(UI ui, boolean starfare) {
        Loaded loaded = ComponentUtil.getData(ui, Loaded.class);
        if (starfare && loaded == null) {
            List<Registration> sheets = new ArrayList<>();
            STYLESHEETS.forEach(sheet -> sheets.add(ui.getPage().addStyleSheet(sheet)));
            boolean mobile = Devices.mobile();
            if (mobile) {
                sheets.add(ui.getPage().addStyleSheet(MOBILE_STYLESHEET));
                ui.getElement().executeJs("document.documentElement.classList.add('starfare-mobile')");
            }
            ComponentUtil.setData(ui, Loaded.class, new Loaded(List.copyOf(sheets), mobile));
        } else if (!starfare && loaded != null) {
            loaded.sheets().forEach(Registration::remove);
            if (loaded.mobile()) {
                ui.getElement().executeJs("document.documentElement.classList.remove('starfare-mobile')");
            }
            ComponentUtil.setData(ui, Loaded.class, null);
        }
    }

    /** The stylesheets a UI currently carries for Starfare. */
    record Loaded(List<Registration> sheets, boolean mobile) implements java.io.Serializable {
    }

    /**
     * The parameter arrives directly, or inside the {@code query} or {@code location} that Vaadin's
     * init request carries for the page the browser opened.
     */
    static Optional<DeviceLayout> requested(@Nullable VaadinRequest request) {
        if (request == null) { return Optional.empty(); }
        return DeviceLayout.fromParameter(request.getParameter(DeviceLayout.PARAMETER))
                .or(() -> fromQuery(request.getParameter("query")))
                .or(() -> fromLocation(request.getParameter("location")));
    }

    static Optional<DeviceLayout> fromLocation(@Nullable String location) {
        if (location == null || !location.contains("?")) { return Optional.empty(); }
        return fromQuery(location.substring(location.indexOf('?') + 1));
    }

    static Optional<DeviceLayout> fromQuery(@Nullable String query) {
        if (query == null) { return Optional.empty(); }
        int start = 0;
        while (start <= query.length()) {
            int end = query.indexOf('&', start);
            String pair = query.substring(start, end < 0 ? query.length() : end);
            int separator = pair.indexOf('=');
            if (separator > 0 && DeviceLayout.PARAMETER.equals(pair.substring(0, separator))) {
                return DeviceLayout.fromParameter(URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8));
            }
            if (end < 0) { break; }
            start = end + 1;
        }
        return Optional.empty();
    }
}
