package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.zettsystems.starfare.game.values.FleetView;
import de.zettsystems.starfare.game.values.PlannedOrder;
import de.zettsystems.starfare.game.values.PlayerViewState;
import de.zettsystems.starfare.game.values.StandingOrderView;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.CssProperties;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Right-hand panel of the map view: fleet grid, planned-orders grid, and
 * standing-orders manager. Stateless w.r.t. game state — pass a fresh
 * {@link PlayerViewState} to {@link #update(PlayerViewState)} on each refresh.
 */
final class FleetAndOrdersPanel extends VerticalLayout {

    private final Grid<FleetView> fleetGrid = new Grid<>(FleetView.class, false);
    private final Grid<PlannedOrder> ordersGrid = new Grid<>(PlannedOrder.class, false);
    private final Span standingOrdersSummary = new Span();
    private final Button standingOrdersManageBtn;
    private final Checkbox fleetShowStanding;
    private final Checkbox ordersShowStanding;

    private boolean syncingSelection;

    FleetAndOrdersPanel(Runnable onRefresh,
                        Consumer<PlannedOrder> onCancelOrder,
                        Runnable onManageStanding,
                        IntConsumer onFleetRowSelected) {
        setPadding(false);
        setSpacing(true);
        setWidth("25%");
        setHeightFull();
        addClassName("map-right");

        fleetShowStanding = new Checkbox(I18n.t(UiTexts.MAP_SHOW_STANDING_TOGGLE));
        ordersShowStanding = new Checkbox(I18n.t(UiTexts.MAP_SHOW_STANDING_TOGGLE));
        fleetShowStanding.addValueChangeListener(_ -> onRefresh.run());
        ordersShowStanding.addValueChangeListener(_ -> onRefresh.run());

        standingOrdersManageBtn = new Button(I18n.t(UiTexts.MAP_STANDING_ORDERS_MANAGE), _ -> onManageStanding.run());
        standingOrdersManageBtn.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);

        configureFleetGrid();
        fleetGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COMPACT);
        fleetGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        fleetGrid.addSelectionListener(event -> {
            if (syncingSelection) {
                return;
            }
            event.getFirstSelectedItem().ifPresentOrElse(fv -> {
                if (!fv.standing() && fv.fleetId() >= 0) {
                    onFleetRowSelected.accept(fv.fleetId());
                }
            }, () -> onFleetRowSelected.accept(-1));
        });
        configureOrdersGrid(onCancelOrder);

        add(buildFleetsHeader(), fleetGrid,
                buildOrdersHeader(), ordersGrid,
                buildStandingHeader(), buildStandingRow());
    }

    void update(PlayerViewState view) {
        List<StandingOrderView> standing = view.standingOrders() != null ? view.standingOrders() : List.of();

        var fleetRows = new ArrayList<>(UiMapper.toFleetViews(view));
        if (Boolean.TRUE.equals(fleetShowStanding.getValue())) {
            fleetRows.addAll(UiMapper.standingAsFleetRows(view));
        }
        // setItems clears the selection — keep the listener silent so it does not
        // echo a phantom "deselect" back to the view that just established the highlight.
        syncingSelection = true;
        try {
            fleetGrid.setItems(fleetRows);
        } finally {
            syncingSelection = false;
        }

        var orderRows = new ArrayList<>(view.plannedOrders());
        if (Boolean.TRUE.equals(ordersShowStanding.getValue())) {
            for (StandingOrderView so : standing) {
                orderRows.add(new PlannedOrder(-1, UiTexts.MAP_ORDER_TYPE_STANDING,
                        so.fromSystemId(), so.toSystemId(), so.fromSystem(), so.toSystem(),
                        so.productionPerTurn(), true, so.id()));
            }
        }
        ordersGrid.setItems(orderRows);

        standingOrdersSummary.setText(standing.isEmpty()
                ? I18n.t(UiTexts.MAP_STANDING_ORDERS_EMPTY)
                : I18n.t(UiTexts.MAP_STANDING_ORDERS_HEADER_COUNT, standing.size()));
        standingOrdersManageBtn.setEnabled(!standing.isEmpty());
    }

    void setGridsVisible(boolean visible) {
        fleetGrid.setVisible(visible);
        ordersGrid.setVisible(visible);
    }

    /**
     * Mirrors the externally tracked highlight onto the grid's selection. A null id
     * clears the selection. The {@code syncingSelection} latch prevents the selection
     * listener from echoing this programmatic change back as a user click.
     */
    void setHighlightedFleet(@Nullable Integer fleetId) {
        syncingSelection = true;
        try {
            if (fleetId == null) {
                fleetGrid.deselectAll();
                return;
            }
            fleetGrid.getGenericDataView().getItems()
                    .filter(fv -> !fv.standing() && fv.fleetId() == fleetId)
                    .findFirst()
                    .ifPresentOrElse(fleetGrid::select, fleetGrid::deselectAll);
        } finally {
            syncingSelection = false;
        }
    }

    private void configureFleetGrid() {
        fleetGrid.removeAllColumns();
        fleetGrid.addColumn(f -> f.standing() ? "∞" : String.valueOf(f.localNo()))
                .setHeader(I18n.t(UiTexts.MAP_COLUMN_NO))
                .setSortable(true).setAutoWidth(true).setFlexGrow(0);
        fleetGrid.addColumn(FleetView::fromName).setHeader(I18n.t(UiTexts.MAP_COLUMN_FROM))
                .setSortable(true).setAutoWidth(true).setFlexGrow(1);
        fleetGrid.addColumn(FleetView::toName).setHeader(I18n.t(UiTexts.MAP_COLUMN_TO))
                .setSortable(true).setAutoWidth(true).setFlexGrow(1);
        fleetGrid.addColumn(FleetView::ships).setHeader(I18n.t(UiTexts.MAP_COLUMN_SHIPS))
                .setSortable(true).setAutoWidth(true).setFlexGrow(0);
        fleetGrid.addColumn(f -> f.standing() ? "—" : String.valueOf(f.eta()))
                .setHeader(I18n.t(UiTexts.MAP_COLUMN_ETA))
                .setSortable(true).setAutoWidth(true).setFlexGrow(0);
        fleetGrid.setHeight("min(40vh, 550px)");
    }

    private void configureOrdersGrid(Consumer<PlannedOrder> onCancelOrder) {
        ordersGrid.addColumn(o -> I18n.t(o.type())).setHeader(I18n.t(UiTexts.MAP_COLUMN_ORDER_TYPE))
                .setAutoWidth(true).setFlexGrow(0);
        ordersGrid.addColumn(PlannedOrder::fromSystem).setHeader(I18n.t(UiTexts.MAP_COLUMN_ORDER_FROM))
                .setAutoWidth(true).setFlexGrow(1);
        ordersGrid.addColumn(PlannedOrder::toSystem).setHeader(I18n.t(UiTexts.MAP_COLUMN_ORDER_TO))
                .setAutoWidth(true).setFlexGrow(1);
        ordersGrid.addColumn(o -> o.ships() != null ? String.valueOf(o.ships()) : "-")
                .setHeader(I18n.t(UiTexts.MAP_COLUMN_ORDER_SHIPS)).setAutoWidth(true).setFlexGrow(0);
        ordersGrid.addComponentColumn(o -> {
            var cancelBtn = new Button(I18n.t(UiTexts.MAP_ACTION_CANCEL_ORDER), _ -> onCancelOrder.accept(o));
            cancelBtn.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.SMALL, ButtonVariant.TERTIARY);
            return cancelBtn;
        }).setHeader("").setAutoWidth(true).setFlexGrow(0);
        ordersGrid.setAllRowsVisible(true);
    }

    private HorizontalLayout buildFleetsHeader() {
        var fleetsHdr = new H2(I18n.t(UiTexts.MAP_OWN_FLEETS));
        fleetsHdr.getStyle().set(CssProperties.MARGIN, "0");
        return expandingHeaderRow(fleetsHdr, fleetShowStanding);
    }

    private HorizontalLayout buildOrdersHeader() {
        var ordersHdr = new H2(I18n.t(UiTexts.MAP_PLANNED_ORDERS));
        ordersHdr.getStyle().set(CssProperties.MARGIN, "0");
        return expandingHeaderRow(ordersHdr, ordersShowStanding);
    }

    private H2 buildStandingHeader() {
        var hdr = new H2(I18n.t(UiTexts.MAP_STANDING_ORDERS_HEADER));
        hdr.getStyle().set(CssProperties.MARGIN, "0");
        return hdr;
    }

    private HorizontalLayout buildStandingRow() {
        standingOrdersSummary.addClassName("standing-orders-summary");
        var row = new HorizontalLayout(standingOrdersSummary, standingOrdersManageBtn);
        row.setAlignItems(Alignment.CENTER);
        row.setPadding(false);
        row.setSpacing(true);
        return row;
    }

    private static HorizontalLayout expandingHeaderRow(H2 hdr, Checkbox toggle) {
        var row = new HorizontalLayout(hdr, toggle);
        row.setWidthFull();
        row.setAlignItems(Alignment.BASELINE);
        row.setPadding(false);
        row.expand(hdr);
        return row;
    }
}
