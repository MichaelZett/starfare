package de.zettsystems.starfare;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.server.VaadinSession;
import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.identity.values.AccountName;
import de.zettsystems.starfare.game.application.GameRegistry;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.application.RoundDeadlineRunner;
import de.zettsystems.starfare.testsupport.ReviewGameFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Locale;
import java.util.UUID;

/** Real views, identity principals and game services, without a browser or JavaScript. */
@SpringBootTest
@Import({SyncAsyncTestConfig.class, TestPostgresConfiguration.class})
@ActiveProfiles("test")
public abstract class AbstractBrowserlessTest extends SpringBrowserlessTest {

    @MockitoBean
    private RoundDeadlineRunner roundDeadlineRunner;

    @Autowired
    protected GameRegistry registry;
    @Autowired
    protected GameService games;
    @Autowired
    protected ReviewGameFixture reviewGames;
    @Autowired
    protected UserAccountService accounts;
    @Autowired
    private UserDetailsService userDetailsService;

    @BeforeEach
    void prepareSession() {
        registry.listIds().forEach(registry::abortGame);
        VaadinSession.getCurrent().setAttribute("starfare.locale", Locale.GERMAN);
        UI.getCurrent().setLocale(Locale.GERMAN);
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    /** Creates a unique verified account, then signs in with the application's actual principal. */
    protected String loginAsNewPlayer(String name) {
        String email = "browserless-" + UUID.randomUUID() + "@example.test";
        accounts.createAccount(email, "Test-password-42!", AccountName.display(name), true);
        loginAs(email);
        return email;
    }

    protected void loginAs(String email) {
        var details = userDetailsService.loadUserByUsername(email);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(details, null, details.getAuthorities()));
    }

    /** ui.access tasks are drained when the test releases the session lock. */
    protected static void runPendingUiAccess() {
        VaadinSession session = VaadinSession.getCurrent();
        session.unlock();
        session.lock();
        roundTrip();
    }

    protected Div mapSystem(String name) {
        var systems = find(Div.class).all().stream().filter(div ->
                (div.hasClassName("sys-own") || div.hasClassName("sys-fog"))
                        && div.getText().split("\\n", 2)[0].equals(name)).toList();
        org.assertj.core.api.Assertions.assertThat(systems).as("Map system %s", name).hasSize(1);
        return systems.getFirst();
    }

    protected Button button(String text) {
        return find(Button.class).withText(text).single();
    }

    protected Dialog openedDialog() {
        var dialogs = find(Dialog.class).all().stream().filter(Dialog::isOpened).toList();
        org.assertj.core.api.Assertions.assertThat(dialogs).as("Open dialog").hasSize(1);
        return dialogs.getFirst();
    }

    protected void selectTab(String text) {
        Tab tab = find(Tab.class).withText(text).single();
        if (!(tab.getParent().orElseThrow() instanceof Tabs tabs)) {
            throw new IllegalStateException("Tab does not belong to a Tabs component");
        }
        tabs.setSelectedTab(tab);
        roundTrip();
    }

    /** UI itself has no recursive text; start from the current view and include property titles. */
    protected String viewText() {
        return elementText(getCurrentView().getElement());
    }

    protected static String elementText(com.vaadin.flow.dom.Element element) {
        if (element.isTextNode()) {
            return element.getText();
        }
        if (!element.isVisible()) {
            return "";
        }
        String own = element.getProperty("headerTitle", "")
                + " " + element.getProperty("cardTitle", "");
        return own + element.getChildren().map(AbstractBrowserlessTest::elementText)
                .collect(java.util.stream.Collectors.joining(" ", " ", ""));
    }
}
