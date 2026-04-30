package de.zettsystems.starfare.auth.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import de.zettsystems.starfare.auth.application.UserService;

@Route("register")
@PageTitle("Starfare – Registrieren")
@AnonymousAllowed
public class RegisterView extends VerticalLayout {

    public RegisterView(UserService users) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 title = new H1("Konto anlegen");
        TextField username = new TextField("Benutzername");
        username.setHelperText("Mindestens " + UserService.USERNAME_MIN + " Zeichen, nur Buchstaben/Ziffern/_.-");
        PasswordField password = new PasswordField("Passwort");
        password.setHelperText("Mindestens " + UserService.PASSWORD_MIN + " Zeichen");
        TextField displayName = new TextField("Anzeigename");
        displayName.setHelperText("Optional – leer lassen übernimmt den Benutzernamen");

        Button register = new Button("Registrieren", _ -> {
            try {
                users.register(username.getValue(), password.getValue(), displayName.getValue());
                Notification ok = Notification.show("Konto angelegt. Bitte einloggen.");
                ok.addThemeVariants(NotificationVariant.SUCCESS);
                UI.getCurrent().navigate(LoginView.class);
            } catch (IllegalArgumentException ex) {
                Notification err = Notification.show(ex.getMessage());
                err.addThemeVariants(NotificationVariant.ERROR);
            }
        });
        register.addThemeVariants(ButtonVariant.PRIMARY);

        Anchor toLogin = new Anchor("login", "Zurück zum Login");

        add(title, username, password, displayName, register, toLogin);
    }
}
