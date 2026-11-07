package de.zettsystems.starfare.observability.config;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinRequestInterceptor;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinSession;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Logs slow page builds and slow UI actions at WARN from {@code starfare.timing.slow-page-ms} on
 * ({@code 0} disables it), for {@code prod-langsam.sh}. Starfare runs with {@code @Push} over a
 * WebSocket, so a servlet filter would only see the first page load; a {@link VaadinRequestInterceptor}
 * also sees every push message.
 *
 * <p>A navigation listener marks the request with the target route ("Langsame Seite"); other slow
 * requests of a UI are reported with its current view ("Langsame Aktion"). The SQL statements of the
 * request thread come from {@link SqlStatementRecorder}. A fast request is still reported ("Viele
 * Abfragen") when it runs at least {@code starfare.timing.many-statements} statements or one statement
 * at least {@value #REPEATED} times: the fingerprint of N+1 and of parts that each load the same data.
 */
@Component
class PageTiming implements VaadinServiceInitListener, VaadinRequestInterceptor {

    static final int REPEATED = 3;
    private static final long serialVersionUID = 1L;
    private static final int REPEATED_SHOWN = 3;
    private static final String START_ATTRIBUTE = PageTiming.class.getName() + ".start";
    private static final String ROUTE_ATTRIBUTE = PageTiming.class.getName() + ".route";
    private static final Logger LOG = LoggerFactory.getLogger(PageTiming.class);

    private final long slowMillis;
    private final int manyStatements;

    PageTiming(@Value("${starfare.timing.slow-page-ms:500}") long slowMillis,
               @Value("${starfare.timing.many-statements:20}") int manyStatements) {
        this.slowMillis = slowMillis;
        this.manyStatements = manyStatements;
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        if (slowMillis <= 0) {
            return;
        }
        event.addVaadinRequestInterceptor(this);
        event.getSource().addUIInitListener(init -> init.getUI().addAfterNavigationListener(this::remember));
    }

    private void remember(AfterNavigationEvent navigation) {
        VaadinRequest request = VaadinRequest.getCurrent();
        if (request != null) {
            request.setAttribute(ROUTE_ATTRIBUTE, navigation.getLocation().getPathWithQueryParameters());
        }
    }

    @Override
    public void requestStart(VaadinRequest request, VaadinResponse response) {
        request.setAttribute(START_ATTRIBUTE, System.nanoTime());
        SqlStatementRecorder.begin();
    }

    @Override
    public void handleException(VaadinRequest request, VaadinResponse response, VaadinSession session, Exception t) {
        // Errors are logged by Vaadin; the duration is still reported in requestEnd.
    }

    @Override
    public void requestEnd(VaadinRequest request, VaadinResponse response, VaadinSession session) {
        SqlStatementRecorder.Recording statements = SqlStatementRecorder.end();
        if (!(request.getAttribute(START_ATTRIBUTE) instanceof Long start) || !LOG.isWarnEnabled()) {
            return;
        }
        long millis = (System.nanoTime() - start) / 1_000_000;
        Object route = request.getAttribute(ROUTE_ATTRIBUTE);
        report(route == null ? null : route.toString(), route == null ? currentView() : null, millis,
                statements.count(), statements.repeated(REPEATED, REPEATED_SHOWN))
                .ifPresent(LOG::warn);
    }

    /**
     * The log line for a request, if it is worth one. {@code route} is set for a navigation,
     * {@code view} for any other request of a UI; without either (resources, heartbeats of closed
     * UIs) nothing is reported.
     */
    Optional<String> report(@Nullable String route, @Nullable String view, long millis, int statements,
                            List<String> repeated) {
        String address = route != null ? route : view;
        if (address == null) {
            return Optional.empty();
        }
        String kind;
        if (millis >= slowMillis) {
            kind = route != null ? "Langsame Seite" : "Langsame Aktion";
        } else if (statements >= manyStatements || !repeated.isEmpty()) {
            kind = "Viele Abfragen";
        } else {
            return Optional.empty();
        }
        String details = repeated.isEmpty() ? "" : " — " + String.join("; ", repeated);
        return Optional.of(kind + " „" + address + "“: " + millis + " ms, " + statements + " SQL-Abfragen" + details);
    }

    /** The view of the UI that handled the request; {@code null} for requests without a UI (resources, heartbeats of closed UIs). */
    private static @Nullable String currentView() {
        UI ui = UI.getCurrent();
        return ui == null ? null : ui.getActiveViewLocation().getPathWithQueryParameters();
    }
}
