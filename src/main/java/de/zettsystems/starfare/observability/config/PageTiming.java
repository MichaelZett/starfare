package de.zettsystems.starfare.observability.config;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinRequestInterceptor;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinSession;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Logs slow page builds and slow UI actions at WARN from {@code starfare.timing.slow-page-ms} on
 * ({@code 0} disables it), for {@code prod-langsam.sh}. Starfare runs with {@code @Push} over a
 * WebSocket, so a servlet filter would only see the first page load; a {@link VaadinRequestInterceptor}
 * also sees every push message.
 *
 * <p>A navigation listener marks the request with the target route ("Langsame Seite"); other slow
 * requests of a UI are reported with its current view ("Langsame Aktion"). The SQL count (an N+1
 * fingerprint) comes from the {@code SessionFactory} statistics and is inflated by concurrent requests.
 */
@Component
@SuppressFBWarnings(value = "SE_TRANSIENT_FIELD_NOT_RESTORED",
        justification = "Serializable only through VaadinRequestInterceptor; a service-level Spring singleton, never serialized with a session.")
class PageTiming implements VaadinServiceInitListener, VaadinRequestInterceptor {

    private static final long serialVersionUID = 1L;
    private static final String START_ATTRIBUTE = PageTiming.class.getName() + ".start";
    private static final String STATEMENTS_ATTRIBUTE = PageTiming.class.getName() + ".statements";
    private static final String ROUTE_ATTRIBUTE = PageTiming.class.getName() + ".route";
    private static final Logger LOG = LoggerFactory.getLogger(PageTiming.class);

    private final transient SessionFactory sessions;
    private final long slowMillis;

    PageTiming(EntityManagerFactory entityManagerFactory,
               @Value("${starfare.timing.slow-page-ms:500}") long slowMillis) {
        this.sessions = entityManagerFactory.unwrap(SessionFactory.class);
        this.slowMillis = slowMillis;
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
        request.setAttribute(STATEMENTS_ATTRIBUTE, sessions.getStatistics().getPrepareStatementCount());
    }

    @Override
    public void handleException(VaadinRequest request, VaadinResponse response, VaadinSession session, Exception t) {
        // Errors are logged by Vaadin; the duration is still reported in requestEnd.
    }

    @Override
    public void requestEnd(VaadinRequest request, VaadinResponse response, VaadinSession session) {
        if (!(request.getAttribute(START_ATTRIBUTE) instanceof Long start)
                || !(request.getAttribute(STATEMENTS_ATTRIBUTE) instanceof Long statementsBefore)) {
            return;
        }
        long millis = (System.nanoTime() - start) / 1_000_000;
        if (millis < slowMillis || !LOG.isWarnEnabled()) {
            return;
        }
        long statements = sessions.getStatistics().getPrepareStatementCount() - statementsBefore;
        Object route = request.getAttribute(ROUTE_ATTRIBUTE);
        if (route != null) {
            LOG.warn("Langsame Seite „{}“: {} ms, {} SQL-Abfragen", route, millis, statements);
            return;
        }
        String view = currentView();
        if (view != null) {
            LOG.warn("Langsame Aktion „{}“: {} ms, {} SQL-Abfragen", view, millis, statements);
        }
    }

    /** The view of the UI that handled the request; {@code null} for requests without a UI (resources, heartbeats of closed UIs). */
    private static @Nullable String currentView() {
        UI ui = UI.getCurrent();
        return ui == null ? null : ui.getActiveViewLocation().getPathWithQueryParameters();
    }
}
