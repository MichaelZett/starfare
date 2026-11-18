package de.zettsystems.starfare.e2e;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import de.zettsystems.starfare.game.ui.LobbyView;
import de.zettsystems.starfare.testsupport.TestPlatformAccounts;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * Meldet ein Testkonto in dieser Browsersitzung an und leitet zur Lobby weiter. Gibt es nur in
 * den Browserabläufen: Starfares eigener Start hat keine Anmeldung, ein Host bringt seine eigene
 * mit. Das Anmeldezeichen liegt in der HTTP-Sitzung, wie es Spring Security nach einer Anmeldung
 * ablegt; jede weitere Anfrage und jeder Push dieser Sitzung sieht dasselbe Konto.
 */
@Route(E2eSignInView.PATH)
@Tag("div")
public class E2eSignInView extends Component implements BeforeEnterObserver {
    static final String PATH = "e2e/sign-in";
    private static final long serialVersionUID = 1L;

    private final transient TestPlatformAccounts accounts;

    public E2eSignInView(TestPlatformAccounts accounts) {
        this.accounts = accounts;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String email = event.getLocation().getQueryParameters().getSingleParameter("email").orElseThrow();
        SecurityContext context = accounts.signedIn(email);
        SecurityContextHolder.setContext(context);
        VaadinSession.getCurrent().getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        event.forwardTo(LobbyView.class);
    }
}
