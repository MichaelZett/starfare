package de.zettsystems.starfare.style.ui;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServiceInitListener;
import de.zettsystems.starfare.style.values.DeviceLayout;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Applies a {@code ?layout=} override and loads the phone stylesheet only for phones, so desktops
 * never download or evaluate mobile rules. The stylesheet is a static resource outside the
 * frontend bundle.
 */
@Component
public class DeviceLayoutInitListener implements VaadinServiceInitListener {
    public static final String MOBILE_STYLESHEET = "styles/starfare-mobile.css";

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent -> {
            uiEvent.getUI().getPage().addStyleSheet("styles/starfare-navigation.css");
            requested(VaadinRequest.getCurrent()).ifPresent(Devices::choose);
            if (Devices.mobile()) {
                var ui = uiEvent.getUI();
                ui.getPage().addStyleSheet(MOBILE_STYLESHEET);
                ui.getElement().executeJs("document.documentElement.classList.add('starfare-mobile')");
            }
        });
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
