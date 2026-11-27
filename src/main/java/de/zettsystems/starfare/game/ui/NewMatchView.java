package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.application.RulesetAccess;
import de.zettsystems.starfare.i18n.I18n;
import jakarta.annotation.security.PermitAll;
import de.zettsystems.gamesplatformapi.PlatformSettings;
import org.jspecify.annotations.Nullable;

/**
 * A new game as its own address, so a host can link "new game" without Starfare's lobby. The
 * wizard opens right away; after creating, it moves on to the waiting room or the map, and giving
 * up leads back to the overview.
 */
@Route(StarfareRoutes.NEW_MATCH)
@CssImport("./styles/starfare.css")
@PermitAll
public class NewMatchView extends VerticalLayout {
    private final GameService game;
    private final PlayerDirectory players;
    private final RulesetAccess rulesetAccess;
    private final @Nullable PlatformSettings settings;

    public NewMatchView(GameService game, PlayerDirectory players, RulesetAccess rulesetAccess,
                        @Nullable PlatformSettings settings) {
        this.game = game;
        this.players = players;
        this.rulesetAccess = rulesetAccess;
        this.settings = settings;
        addClassName("new-match");
        Button open = new Button(I18n.t(UiTexts.LOBBY_NEW_GAME), _ -> openWizard());
        open.addThemeVariants(ButtonVariant.PRIMARY);
        add(new H1(I18n.t(UiTexts.LOBBY_WIZARD_TITLE)), open);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        openWizard();
    }

    private void openWizard() {
        NewGameDialog.open(game, players, () -> { },
                RulesetOffer.of(rulesetAccess, UserContext.currentPlayerId().orElse(null)), settings,
                () -> getUI().ifPresent(ui -> ui.navigate(LobbyView.class)));
    }
}
