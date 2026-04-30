package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.game.values.StandingOrderView;
import de.zettsystems.starfare.i18n.I18n;

import java.util.List;

final class StandingOrdersDialog {

    private StandingOrdersDialog() {
    }

    static void open(GameService game, GameId gameId, int pid, Runnable onChange) {
        PlayerViewState view = game.viewFor(gameId, pid);
        var dialog = new Dialog();
        dialog.setHeaderTitle(I18n.t(UiTexts.MAP_STANDING_ORDERS_DIALOG_TITLE));
        dialog.setWidth("min(640px, 96vw)");

        Grid<StandingOrderView> grid = new Grid<>(StandingOrderView.class, false);
        grid.addColumn(StandingOrderView::fromSystem).setHeader(I18n.t(UiTexts.MAP_COLUMN_STANDING_FROM))
                .setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(StandingOrderView::toSystem).setHeader(I18n.t(UiTexts.MAP_COLUMN_STANDING_TO))
                .setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(StandingOrderView::productionPerTurn).setHeader(I18n.t(UiTexts.MAP_COLUMN_STANDING_PRODUCTION))
                .setAutoWidth(true).setFlexGrow(0);
        grid.addComponentColumn(so -> {
            var del = new Button(I18n.t(UiTexts.MAP_ACTION_DELETE_STANDING), _ -> {
                boolean ok = game.removeStandingOrder(gameId, pid, so.id());
                if (!ok) {
                    Notification.show(I18n.t(UiTexts.MAP_REMOVE_STANDING_ORDER_FAILED));
                }
                PlayerViewState refreshed = game.viewFor(gameId, pid);
                List<StandingOrderView> list = refreshed.standingOrders() != null
                        ? refreshed.standingOrders() : List.of();
                grid.setItems(list);
                if (list.isEmpty()) {
                    dialog.close();
                }
                onChange.run();
            });
            del.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.SMALL, ButtonVariant.TERTIARY);
            return del;
        }).setHeader("").setAutoWidth(true).setFlexGrow(0);
        grid.setAllRowsVisible(true);
        grid.setItems(view.standingOrders() != null ? view.standingOrders() : List.of());

        dialog.add(grid);
        var closeBtn = new Button(I18n.t(UiTexts.MAP_STANDING_ORDERS_DIALOG_CLOSE), _ -> dialog.close());
        dialog.getFooter().add(closeBtn);
        dialog.open();
    }
}
