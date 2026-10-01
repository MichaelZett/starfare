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
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.i18n.I18n;
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
        add(new de.zettsystems.starfare.game.ui.AccountNavigation(), new LegalFooter());
        Button delete = new Button(I18n.t(UiTexts.ACCOUNT_DELETE_ACTION), _ -> confirmDeletion());
        delete.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.PRIMARY);
        add(new H1(I18n.t(UiTexts.LEGAL_DELETE_ACCOUNT)), new Paragraph(I18n.t(UiTexts.ACCOUNT_DELETE_WARNING)), delete);
    }

    private void confirmDeletion() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(I18n.t(UiTexts.ACCOUNT_DELETE_CONFIRM));
        dialog.setText(I18n.t(UiTexts.ACCOUNT_DELETE_EFFECT));
        dialog.setCancelable(true);
        dialog.setCancelText(I18n.t(UiTexts.ACCOUNT_DELETE_CANCEL));
        dialog.setConfirmText(I18n.t(UiTexts.ACCOUNT_DELETE_ACTION));
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(_ -> UserContext.currentUserId().ifPresent(id -> {
            accounts.deleteAccount(id);
            SecurityContextHolder.clearContext();
            getUI().ifPresent(ui -> ui.getPage().setLocation("/"));
        }));
        dialog.open();
    }
}
