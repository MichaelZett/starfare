package de.zettsystems.starfare.legal.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import de.zettsystems.identity.application.UserAccountService;
import de.zettsystems.starfare.auth.ui.UserContext;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.context.SecurityContextHolder;

/** User-initiated account deletion backed by the identity component. */
@Route("account/delete")
@PermitAll
public class AccountDeletionView extends VerticalLayout {
    private final UserAccountService accounts;

    public AccountDeletionView(UserAccountService accounts) {
        this.accounts = accounts;
        setMaxWidth("42rem");
        Button delete = new Button("Konto endgültig löschen", _ -> confirmDeletion());
        delete.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.PRIMARY);
        add(new H1("Konto löschen"), new Paragraph("Dieser Vorgang löscht dein Konto und die Anmeldedaten dauerhaft. "
                + "Er kann nicht rückgängig gemacht werden."), delete);
    }

    private void confirmDeletion() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Konto wirklich löschen?");
        dialog.setText("Der Zugang zu Starfare wird sofort entfernt.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Endgültig löschen");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(_ -> UserContext.currentUserId().ifPresent(id -> {
            accounts.deleteAccount(id);
            SecurityContextHolder.clearContext();
            getUI().ifPresent(ui -> ui.getPage().setLocation("/"));
        }));
        dialog.open();
    }
}
