package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteParameters;
import de.zettsystems.starfare.auth.application.PlayerDirectory;
import de.zettsystems.starfare.auth.ui.UserContext;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.GameSetup;
import de.zettsystems.starfare.game.values.GameVisibility;
import de.zettsystems.starfare.game.values.RulesetCatalog;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.gamesplatformapi.PlatformSettings;
import org.jspecify.annotations.Nullable;

/**
 * Dialog "new game": one page of choices without a summary. The creator always takes a seat; when every other
 * seat belongs to the computer the game starts right away and opens its map, otherwise its waiting room.
 */
public final class NewGameDialog {

    private NewGameDialog() {
    }

    /**
     * Offers only the variants {@code offer} allows the current account, with their marking, and starts from the
     * choices of the account's last new game that {@code settings} kept.
     */
    public static void open(GameService game, PlayerDirectory players, Runnable onCreated, RulesetOffer offer,
                            @Nullable PlatformSettings settings) {
        open(game, players, onCreated, game.rulesets(), offer, memory(settings), () -> { });
    }

    /** Like {@link #open(GameService, PlayerDirectory, Runnable, RulesetOffer, PlatformSettings)}; {@code onCancel} runs when the person gives up. */
    static void open(GameService game, PlayerDirectory players, Runnable onCreated, RulesetOffer offer,
                     @Nullable PlatformSettings settings, Runnable onCancel) {
        open(game, players, onCreated, game.rulesets(), offer, memory(settings), onCancel);
    }

    static void open(GameService game, PlayerDirectory players, Runnable onCreated, RulesetCatalog catalog) {
        open(game, players, onCreated, catalog, RulesetOffer.everything(), NewGameMemory.none(), () -> { });
    }

    private static NewGameMemory memory(@Nullable PlatformSettings settings) {
        return NewGameMemory.of(settings, UserContext.currentPlayerId().orElse(null));
    }

    private static void open(GameService game, PlayerDirectory players, Runnable onCreated, RulesetCatalog catalog,
                             RulesetOffer offer, NewGameMemory memory, Runnable onCancel) {
        Dialog dialog = new Dialog();
        dialog.addClassName("create-game-dialog");
        // A stray click next to the dialog must not throw away every choice; "Cancel" and Esc close it.
        dialog.setCloseOnOutsideClick(false);
        dialog.addDialogCloseActionListener(_ -> {
            dialog.close();
            onCancel.run();
        });
        dialog.setHeaderTitle(I18n.t(UiTexts.LOBBY_WIZARD_TITLE));
        dialog.setWidth("min(1180px, 96vw)");
        String hostId = UserContext.currentPlayerId().orElse("");
        String hostName = hostId.isBlank() ? "" : players.displayName(hostId);
        NewGamePanel panel = new NewGamePanel(catalog, offer, hostName);
        panel.recall(memory.recall());
        Button create = new Button(I18n.t(UiTexts.LOBBY_WIZARD_CREATE), VaadinIcon.ROCKET.create());
        create.setId("create-game");
        create.addThemeVariants(ButtonVariant.PRIMARY);
        create.addClickListener(_ -> {
            if (filled(panel.gameName(), UiTexts.LOBBY_GAME_NAME_REQUIRED)
                    && filled(panel.empireName(), UiTexts.LOBBY_EMPIRE_NAME_REQUIRED)) {
                create.setEnabled(false);
                memory.remember(panel.choices());
                createGame(game, dialog, panel, hostName, onCreated);
            }
        });
        Button cancel = new Button(I18n.t(UiTexts.LOBBY_WIZARD_CANCEL), _ -> {
            dialog.close();
            onCancel.run();
        });
        dialog.getFooter().add(cancel, create);
        dialog.add(panel);
        dialog.open();
    }

    private static boolean filled(TextField field, String requiredKey) {
        boolean filled = !field.getValue().isBlank();
        field.setInvalid(!filled);
        field.setErrorMessage(filled ? null : I18n.t(requiredKey));
        return filled;
    }

    private static void createGame(GameService game, Dialog dialog, NewGamePanel panel, String hostName,
                                   Runnable onCreated) {
        GameSetup setup = panel.setup();
        String accountId = UserContext.currentPlayerId().orElse(null);
        GameId gameId = game.newGame(setup, accountId, panel.gameName().getValue().trim());
        boolean joined = false;
        if (accountId != null) {
            joined = game.joinGame(gameId, accountId, hostName, panel.empireName().getValue()).isPresent();
            if (!joined) {
                Notification.show(I18n.t(UiTexts.LOBBY_JOIN_FAILED));
            }
            if (panel.openToAll()) {
                game.changeVisibility(gameId, accountId, GameVisibility.PUBLIC);
            }
        }
        boolean started = joined && setup.humanPlayers() == 1 && setup.aiPlayers() > 0
                && game.startGame(gameId, accountId);
        var currentUi = dialog.getUI();
        onCreated.run();
        dialog.close();
        // A running game against the computer opens its map; any other game waits for players in its waiting room.
        Class<? extends Component> target = started ? MainView.class : WaitingRoomView.class;
        currentUi.ifPresent(ui -> ui.navigate(target, new RouteParameters("gameId", gameId.value())));
    }
}
