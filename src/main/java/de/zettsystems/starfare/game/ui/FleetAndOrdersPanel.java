package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.report.values.BattleReplay;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;
import de.zettsystems.starfare.style.CssProperties;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Switchable right-hand map sidebar that keeps the map context visible. */
final class FleetAndOrdersPanel extends VerticalLayout {

    private enum Section { CONTACTS, DETAILS, FLEETS, ORDERS, RELOCATIONS, REPORT }

    private enum EventCategory { PRODUCTION, REINFORCEMENT, BATTLE_WON, BATTLE_LOST, SYSTEM_LOST, DEFENSE_HELD }

    private final Grid<FleetView> fleetGrid = new Grid<>(FleetView.class, false);
    private final Grid<PlannedOrder> ordersGrid = new Grid<>(PlannedOrder.class, false);
    private final Grid<StandingOrderView> relocationsGrid = new Grid<>(StandingOrderView.class, false);
    private final VerticalLayout contactsPage = page();
    private final VerticalLayout detailsPage = page();
    private final VerticalLayout fleetsPage = page();
    private final VerticalLayout ordersPage = page();
    private final VerticalLayout relocationsPage = page();
    private final VerticalLayout reportPage = page();
    private final List<VerticalLayout> pages = List.of(contactsPage, detailsPage, fleetsPage, ordersPage,
            relocationsPage, reportPage);
    private final Tabs tabs;
    private final List<Tab> sectionTabs;
    private final Tab reportTab;

    private boolean syncingSelection;
    private boolean readOnly;
    private boolean battlePresentationEnabled;
    private @Nullable GameId gameId;
    private @Nullable VisibleSystem selectedSystem;
    private @Nullable Integer selectedFleetId;
    private final EnumSet<EventCategory> enabledEventCategories = EnumSet.allOf(EventCategory.class);
    private final IntConsumer onReportSystemSelected;
    private final IntConsumer onResolvedBattleSelected;
    private final BiConsumer<Integer, Integer> onGarrisonReserveChanged;
    private final Consumer<StandingOrderView> onEditStandingOrder;
    private final Consumer<StandingOrderView> onDeleteStandingOrder;
    private final Runnable onBattleAcknowledged;
    private @Nullable Integer selectedReportSystemId;

    FleetAndOrdersPanel(Consumer<PlannedOrder> onCancelOrder,
                        Consumer<StandingOrderView> onEditStandingOrder,
                        Consumer<StandingOrderView> onDeleteStandingOrder,
                        Runnable onBattleAcknowledged,
                        IntConsumer onFleetRowSelected,
                        IntConsumer onReportSystemSelected,
                        IntConsumer onResolvedBattleSelected,
                        BiConsumer<Integer, Integer> onGarrisonReserveChanged) {
        this.onReportSystemSelected = onReportSystemSelected;
        this.onResolvedBattleSelected = onResolvedBattleSelected;
        this.onGarrisonReserveChanged = onGarrisonReserveChanged;
        this.onEditStandingOrder = onEditStandingOrder;
        this.onDeleteStandingOrder = onDeleteStandingOrder;
        this.onBattleAcknowledged = onBattleAcknowledged;
        setPadding(false);
        setSpacing(true);
        setWidthFull();
        setHeightFull();
        addClassName("map-right");

        sectionTabs = List.of(
                tab(UiTexts.MAP_SIDEBAR_CONTACTS),
                tab(UiTexts.MAP_SIDEBAR_DETAILS),
                tab(UiTexts.MAP_SIDEBAR_FLEETS),
                tab(UiTexts.MAP_SIDEBAR_ORDERS),
                tab(UiTexts.MAP_SIDEBAR_RELOCATIONS),
                tab(UiTexts.MAP_SIDEBAR_REPORT));
        reportTab = sectionTabs.getLast();
        tabs = new Tabs();
        tabs.add(sectionTabs.toArray(Tab[]::new));
        tabs.setWidthFull();
        tabs.addClassName("map-sidebar-tabs");
        tabs.addSelectedChangeListener(event -> showSection(sectionOf(event.getSelectedTab())));

        configureFleetGrid(onFleetRowSelected);
        configureOrdersGrid(onCancelOrder);
        configureRelocationsGrid();
        fleetsPage.add(header(UiTexts.MAP_OWN_FLEETS), fleetGrid);
        ordersPage.add(header(UiTexts.MAP_PLANNED_ORDERS), ordersGrid);
        relocationsPage.add(header(UiTexts.MAP_STANDING_ORDERS_HEADER), relocationsGrid);
        add(tabs, contactsPage, detailsPage, fleetsPage, ordersPage, relocationsPage, reportPage);
        showSection(Section.CONTACTS);
    }

    void update(GameId gameId, PlayerViewState view, @Nullable VisibleSystem system, @Nullable Integer fleetId,
                boolean spectator) {
        this.gameId = gameId;
        currentView = view;
        selectedSystem = system == null ? null : view.systems().stream()
                .filter(candidate -> candidate.id() == system.id()).findFirst().orElse(null);
        selectedFleetId = fleetId;
        readOnly = view.gameOver() || spectator;
        TurnReport report = view.report();
        int reportTurn = report != null ? report.turn() : view.turn() - 1;
        battlePresentationEnabled = BattlePresentationPreference.enabled(gameId, reportTurn,
                view.battlePresentationEnabled());
        fleetGrid.setItems(UiMapper.toFleetViews(view));
        ordersGrid.setItems(view.plannedOrders());
        relocationsGrid.setItems(view.standingOrders());
        renderContacts(view);
        renderDetails(view);
        renderReport(view);
        syncFleetSelection();
    }

    void setViewsVisible(boolean visible) {
        tabs.setVisible(visible);
        if (!visible) {
            pages.forEach(page -> page.setVisible(false));
        } else {
            showSection(sectionOf(tabs.getSelectedTab()));
        }
    }

    void setReportAvailable(boolean available) {
        reportTab.setVisible(available);
        if (!available && Objects.equals(tabs.getSelectedTab(), reportTab)) {
            tabs.setSelectedTab(sectionTabs.getFirst());
        }
    }

    void showOrders() {
        select(Section.ORDERS);
    }

    void showReport() {
        select(Section.REPORT);
    }

    void showDetails() {
        select(Section.DETAILS);
    }

    void showRelocations() {
        select(Section.RELOCATIONS);
    }

    private void renderContacts(PlayerViewState view) {
        contactsPage.removeAll();
        contactsPage.add(header(UiTexts.MAP_SIDEBAR_CONTACTS));
        List<VisibleSystem> contacts = view.systems().stream()
                .filter(system -> !isBattlePending(system.id()))
                .filter(system -> !system.fullyVisible() && system.ownerId() != null)
                .sorted((left, right) -> Integer.compare(lastSeen(right), lastSeen(left)))
                .toList();
        if (contacts.isEmpty()) {
            contactsPage.add(new Paragraph(I18n.t(UiTexts.MAP_SIDEBAR_CONTACTS_EMPTY)));
            return;
        }
        contacts.forEach(system -> contactsPage.add(contact(view, system)));
    }

    private static VerticalLayout contact(PlayerViewState view, VisibleSystem system) {
        VerticalLayout card = page();
        card.addClassName("map-sidebar-contact");
        card.add(new H2(system.name()),
                detail(I18n.t(UiTexts.MAP_SIDEBAR_OWNER), ownerName(view, system.ownerId())),
                detail(I18n.t(system.approximate() ? UiTexts.MAP_SIDEBAR_ESTIMATED_SHIPS
                                : UiTexts.MAP_SIDEBAR_COUNTED_SHIPS), shipValue(system)),
                detail(I18n.t(UiTexts.MAP_SIDEBAR_PRODUCTION), productionValue(system)),
                detail(I18n.t(UiTexts.MAP_SIDEBAR_LAST_CONTACT), contactAge(system)));
        return card;
    }

    private static String ownerName(PlayerViewState view, @Nullable Integer ownerId) {
        if (ownerId == null) {
            return "—";
        }
        return view.players().stream().filter(player -> player.id() == ownerId)
                .map(Player::label).findFirst().orElse("?");
    }

    private static String shipValue(VisibleSystem system) {
        String ships = value(system.garrison());
        return system.approximate() && system.garrison() != null ? "~" + ships : ships;
    }

    private static String productionValue(VisibleSystem system) {
        String production = value(system.productionPerTurn());
        return system.approximate() && system.productionPerTurn() != null ? "~" + production : production;
    }

    private static String contactAge(VisibleSystem system) {
        if (system.lastSeenTurn() == null) {
            return "—";
        }
        return I18n.t(UiTexts.MAP_SIDEBAR_LAST_SEEN_TURN, system.lastSeenTurn());
    }

    private static int lastSeen(VisibleSystem system) {
        Integer turn = system.lastSeenTurn();
        return turn != null ? turn : Integer.MIN_VALUE;
    }

    private void renderDetails(PlayerViewState view) {
        detailsPage.removeAll();
        detailsPage.add(header(UiTexts.MAP_SIDEBAR_DETAILS));
        if (selectedSystem != null) {
            VisibleSystem system = selectedSystem;
            if (isBattlePending(system.id())) {
                detailsPage.add(new Paragraph(I18n.t(UiTexts.ROUND_EVENT_BATTLE_READY, system.name())));
                return;
            }
            detailsPage.add(detail(I18n.t(UiTexts.MAP_SIDEBAR_SYSTEM), system.name()));
            if (!system.fullyVisible()) {
                detailsPage.add(intelligenceHeader(),
                        detail(I18n.t(UiTexts.MAP_SIDEBAR_OWNER), ownerName(view, system.ownerId())),
                        detail(I18n.t(system.approximate() ? UiTexts.MAP_SIDEBAR_ESTIMATED_SHIPS
                                        : UiTexts.MAP_SIDEBAR_COUNTED_SHIPS), shipValue(system)),
                        detail(I18n.t(UiTexts.MAP_SIDEBAR_PRODUCTION), productionValue(system)),
                        detail(I18n.t(UiTexts.MAP_SIDEBAR_LAST_CONTACT), contactAge(system)));
                return;
            }
            detailsPage.add(detail(I18n.t(UiTexts.MAP_SIDEBAR_GARRISON), value(system.garrison())),
                    detail(I18n.t(UiTexts.MAP_SIDEBAR_PRODUCTION), value(system.productionPerTurn())));
            if (system.routedProduction() != null) {
                ProductionFlow flow = ProductionFlow.at(system.id(), view.standingOrders());
                detailsPage.add(detail(I18n.t(UiTexts.MAP_PRODUCTION_INCOMING), flow.incoming()),
                        detail(I18n.t(UiTexts.MAP_PRODUCTION_OUTGOING), flow.outgoing()));
            }
            renderGarrisonReserve(system);
            renderOwnershipHistory(view, system);
            return;
        }
        Fleet selectedFleet = view.ownFleets().stream()
                .filter(fleet -> selectedFleetId != null && fleet.globalId() == selectedFleetId)
                .findFirst().orElse(null);
        if (selectedFleet != null) {
            FleetView fleet = UiMapper.toFleetViews(view).stream()
                    .filter(row -> row.fleetId() == selectedFleet.globalId()).findFirst().orElseThrow();
            detailsPage.add(detail(I18n.t(UiTexts.MAP_SIDEBAR_FLEET), String.valueOf(fleet.localNo())),
                    detail(I18n.t(UiTexts.MAP_SIDEBAR_OWNER), ownerName(view, selectedFleet.ownerId())),
                    detail(I18n.t(UiTexts.MAP_COLUMN_FROM), fleet.fromName()),
                    detail(I18n.t(UiTexts.MAP_COLUMN_TO), fleet.toName()),
                    detail(I18n.t(UiTexts.MAP_COLUMN_SHIPS), fleet.ships()),
                    detail(I18n.t(UiTexts.MAP_SIDEBAR_FLEET_LAUNCHED), selectedFleet.launchTurn()),
                    detail(I18n.t(UiTexts.MAP_SIDEBAR_FLEET_TRAVELLING),
                            Math.max(0, view.turn() - selectedFleet.launchTurn())),
                    detail(I18n.t(UiTexts.MAP_COLUMN_ETA), fleet.eta()),
                    detail(I18n.t(UiTexts.MAP_COLUMN_ARRIVAL_TURN), view.turn() + fleet.eta()));
            return;
        }
        detailsPage.add(new Paragraph(I18n.t(UiTexts.MAP_SIDEBAR_DETAILS_EMPTY)));
    }

    private void renderGarrisonReserve(VisibleSystem system) {
        Integer reserveValue = system.garrisonReserve();
        Integer garrison = system.garrison();
        if (readOnly || reserveValue == null || garrison == null) {
            return;
        }
        IntegerField reserve = new IntegerField(I18n.t(UiTexts.MAP_SIDEBAR_GARRISON_RESERVE));
        reserve.setMin(0);
        reserve.setMax(garrison);
        reserve.setValue(reserveValue);
        reserve.setStepButtonsVisible(true);
        Button save = new Button(I18n.t(UiTexts.MAP_SIDEBAR_GARRISON_RESERVE_SAVE), _ -> {
            Integer value = reserve.getValue();
            if (value != null) {
                onGarrisonReserveChanged.accept(system.id(), value);
            }
        });
        save.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);
        detailsPage.add(reserve, save);
    }

    private void renderOwnershipHistory(PlayerViewState view, VisibleSystem system) {
        if (!system.fullyVisible() || system.ownershipHistory().isEmpty()) {
            return;
        }
        List<SystemOwnership> periods = system.ownershipHistory();
        SystemOwnership current = periods.getLast();
        detailsPage.add(detail(I18n.t(UiTexts.MAP_SIDEBAR_OWNED_SINCE), current.sinceTurn()));
        if (periods.size() == 1) {
            detailsPage.add(detail(I18n.t(UiTexts.MAP_SIDEBAR_PREVIOUS_OWNER),
                    I18n.t(UiTexts.MAP_SIDEBAR_NONE)));
            return;
        }
        for (int index = periods.size() - 2; index >= 0; index--) {
            SystemOwnership previous = periods.get(index);
            detailsPage.add(detail(I18n.t(UiTexts.MAP_SIDEBAR_PREVIOUS_OWNER),
                    ownerName(view, previous.ownerId()) + " (" + I18n.t(UiTexts.MAP_SIDEBAR_UNTIL_TURN,
                    periods.get(index + 1).sinceTurn() - 1) + ")"));
        }
    }

    private void renderReport(PlayerViewState view) {
        reportPage.removeAll();
        reportPage.add(header(UiTexts.MAP_SIDEBAR_REPORT));
        Div filters = new Div();
        filters.addClassName("report-filter-bar");
        filters.add(new Span(I18n.t(UiTexts.ROUND_FILTER_LABEL)));
        addReportFilter(filters, EventCategory.PRODUCTION, UiTexts.ROUND_FILTER_PRODUCTION);
        addReportFilter(filters, EventCategory.REINFORCEMENT, UiTexts.ROUND_FILTER_REINFORCEMENT);
        addReportFilter(filters, EventCategory.BATTLE_WON, UiTexts.ROUND_FILTER_BATTLE_WON);
        addReportFilter(filters, EventCategory.BATTLE_LOST, UiTexts.ROUND_FILTER_BATTLE_LOST);
        addReportFilter(filters, EventCategory.SYSTEM_LOST, UiTexts.ROUND_FILTER_SYSTEM_LOST);
        addReportFilter(filters, EventCategory.DEFENSE_HELD, UiTexts.ROUND_FILTER_DEFENSE_HELD);
        Checkbox presentation = new Checkbox(I18n.t(UiTexts.BATTLE_PRESENTATION_TOGGLE), battlePresentationEnabled);
        presentation.addValueChangeListener(event -> {
            GameId current = gameId;
            TurnReport currentReport = view.report();
            if (current != null) {
                int reportTurn = currentReport != null ? currentReport.turn() : view.turn() - 1;
                battlePresentationEnabled = event.getValue();
                BattlePresentationPreference.set(current, reportTurn, battlePresentationEnabled);
                renderReport(view);
            }
        });
        filters.add(presentation);
        reportPage.add(filters);
        TurnReport report = view.report();
        List<TurnEvent> events = report != null ? report.events() : List.of();
        List<TurnEvent> filtered = events.stream().filter(this::isEventEnabled).toList();
        if (events.isEmpty()) {
            reportPage.add(new Paragraph(I18n.t(UiTexts.ROUND_NO_EVENTS)));
            return;
        }
        if (filtered.isEmpty()) {
            reportPage.add(new Paragraph(I18n.t(UiTexts.ROUND_NO_EVENTS_FILTERED)));
            return;
        }
        for (int index = 0; index < filtered.size(); index++) {
            reportPage.add(eventCard(filtered.get(index), index));
        }
    }

    private void addReportFilter(Div filters, EventCategory category, String key) {
        Checkbox filter = new Checkbox(I18n.t(key), enabledEventCategories.contains(category));
        filter.addClassName("report-filter-item");
        filter.addValueChangeListener(event -> {
            if (Boolean.TRUE.equals(event.getValue())) {
                enabledEventCategories.add(category);
            } else {
                enabledEventCategories.remove(category);
            }
            PlayerViewState view = currentView;
            if (view != null) {
                renderReport(view);
            }
        });
        filters.add(filter);
    }

    private @Nullable PlayerViewState currentView;

    private boolean isEventEnabled(TurnEvent event) {
        PlayerViewState view = currentView;
        TurnReport report = view == null ? null : view.report();
        if ((event instanceof TurnEvent.Victory || event instanceof TurnEvent.Defeat)
                && gameId != null && report != null
                && !BattleAcknowledgements.pending(gameId, report).isEmpty()) {
            return false;
        }
        return categoryOf(event).map(enabledEventCategories::contains).orElse(true);
    }

    private static Optional<EventCategory> categoryOf(TurnEvent event) {
        return Optional.ofNullable(switch (event) {
            case TurnEvent.Production _ -> EventCategory.PRODUCTION;
            case TurnEvent.Reinforcement _ -> EventCategory.REINFORCEMENT;
            case TurnEvent.BattleWon _ -> EventCategory.BATTLE_WON;
            case TurnEvent.BattleLost _ -> EventCategory.BATTLE_LOST;
            case TurnEvent.SystemLost _ -> EventCategory.SYSTEM_LOST;
            case TurnEvent.DefenseHeld _ -> EventCategory.DEFENSE_HELD;
            case TurnEvent.Victory _, TurnEvent.Defeat _ -> null;
        });
    }

    private Div eventCard(TurnEvent event, int index) {
        Div card = new Div();
        card.addClassNames("event-card", eventCss(event));
        card.getStyle().set(CssProperties.ANIMATION_DELAY, (index * 0.1) + "s");
        card.add(new Span(eventIcon(event)), new Span(eventText(event)));
        BattleReplay replay = BattleReplay.from(event).orElse(null);
        event.battleSystemId().ifPresent(systemId -> {
            card.addClassName("event-map-linked");
            if (Objects.equals(selectedReportSystemId, systemId)) {
                card.addClassName("event-map-selected");
            }
            if (replay == null) {
                card.addClickListener(_ -> onReportSystemSelected.accept(systemId));
            } else if (!isBattlePending(systemId)) {
                card.addClickListener(_ -> onResolvedBattleSelected.accept(systemId));
            }
        });
        if (battlePresentationEnabled && replay != null && event.battleSystemId().isPresent()
                && isBattlePending(event.battleSystemId().getAsInt())) {
            card.addClassName("event-battle-interactive");
            card.addClickListener(_ -> BattleReplayDialog.open(replay, battleSides(event), () -> acknowledgeBattle(event)));
            return card;
        }
        if (event.battleSystemId().isPresent()) {
            int systemId = event.battleSystemId().getAsInt();
            if (isBattlePending(systemId)) {
                card.addClassName("event-battle-interactive");
                card.addClickListener(_ -> acknowledgeBattle(event));
                return card;
            }
        }
        return card;
    }

    private static BattleReplayDialog.BattleSides battleSides(TurnEvent event) {
        return switch (event) {
            case TurnEvent.BattleWon battle -> new BattleReplayDialog.BattleSides(I18n.t(UiTexts.BATTLE_REPLAY_YOU),
                    battle.wasNeutral() ? I18n.t(UiTexts.BATTLE_REPLAY_NEUTRAL) : I18n.t(UiTexts.BATTLE_REPLAY_OPPONENT));
            case TurnEvent.BattleLost _ -> new BattleReplayDialog.BattleSides(I18n.t(UiTexts.BATTLE_REPLAY_YOU),
                    I18n.t(UiTexts.BATTLE_REPLAY_OPPONENT));
            case TurnEvent.SystemLost _, TurnEvent.DefenseHeld _ -> new BattleReplayDialog.BattleSides(
                    I18n.t(UiTexts.BATTLE_REPLAY_OPPONENT), I18n.t(UiTexts.BATTLE_REPLAY_YOU));
            case TurnEvent.Production _, TurnEvent.Reinforcement _, TurnEvent.Victory _, TurnEvent.Defeat _ ->
                    new BattleReplayDialog.BattleSides("", "");
        };
    }

    private boolean isBattlePending(int systemId) {
        GameId current = gameId;
        TurnReport report = currentView == null ? null : currentView.report();
        return current != null && report != null && BattleAcknowledgements.pending(current, report).contains(systemId);
    }

    private void acknowledgeBattle(TurnEvent event) {
        GameId current = gameId;
        TurnReport report = currentView == null ? null : currentView.report();
        event.battleSystemId().ifPresent(systemId -> {
            if (current != null && report != null) {
                BattleAcknowledgements.acknowledge(current, report, event);
                onBattleAcknowledged.run();
            }
        });
    }

    void selectReportSystem(int systemId) {
        selectedReportSystemId = systemId;
        showReport();
        PlayerViewState view = currentView;
        if (view != null) {
            renderReport(view);
        }
    }

    private String eventIcon(TurnEvent event) {
        if (event.battleSystemId().isPresent() && isBattlePending(event.battleSystemId().getAsInt())) {
            return "⚔";
        }
        return switch (event) {
            case TurnEvent.Production _ -> "⬆";
            case TurnEvent.Reinforcement _ -> "→";
            case TurnEvent.BattleWon _ -> "⚔";
            case TurnEvent.BattleLost _ -> "✕";
            case TurnEvent.SystemLost _ -> "☠";
            case TurnEvent.DefenseHeld _ -> "🛡";
            case TurnEvent.Victory _ -> "🏆";
            case TurnEvent.Defeat _ -> "☠";
        };
    }

    private String eventCss(TurnEvent event) {
        if (event.battleSystemId().isPresent() && isBattlePending(event.battleSystemId().getAsInt())) {
            return "event-battle-pending";
        }
        return switch (event) {
            case TurnEvent.Production _ -> "event-production";
            case TurnEvent.Reinforcement _ -> "event-reinforcement";
            case TurnEvent.BattleWon _ -> "event-battle-won";
            case TurnEvent.BattleLost _ -> "event-battle-lost";
            case TurnEvent.SystemLost _ -> "event-system-lost";
            case TurnEvent.DefenseHeld _ -> "event-defense-held";
            case TurnEvent.Victory _ -> "event-victory";
            case TurnEvent.Defeat _ -> "event-system-lost";
        };
    }

    private String eventText(TurnEvent event) {
        return switch (event) {
            case TurnEvent.Production production -> I18n.t(UiTexts.ROUND_EVENT_PRODUCTION,
                    production.systemName(), production.amount());
            case TurnEvent.Reinforcement reinforcement -> I18n.t(UiTexts.ROUND_EVENT_REINFORCEMENT,
                    reinforcement.systemName(), reinforcement.ships(), reinforcement.totalGarrison(), reinforcement.fleetLabel());
            case TurnEvent.BattleWon battle -> isBattlePending(battle.systemId())
                    ? I18n.t(UiTexts.ROUND_EVENT_BATTLE_READY, battle.systemName())
                    : I18n.t(battle.wasNeutral() ? UiTexts.ROUND_EVENT_BATTLE_WON_NEUTRAL
                    : UiTexts.ROUND_EVENT_BATTLE_WON_ENEMY, battle.systemName(), battle.attacking(),
                    battle.defending(), battle.remaining());
            case TurnEvent.BattleLost battle -> isBattlePending(battle.systemId())
                    ? I18n.t(UiTexts.ROUND_EVENT_BATTLE_READY, battle.systemName())
                    : I18n.t(UiTexts.ROUND_EVENT_BATTLE_LOST, battle.systemName(), battle.attacking(),
                    battle.defending(), battle.defendersLeft());
            case TurnEvent.SystemLost lost -> isBattlePending(lost.systemId())
                    ? I18n.t(UiTexts.ROUND_EVENT_BATTLE_READY, lost.systemName())
                    : I18n.t(UiTexts.ROUND_EVENT_SYSTEM_LOST, lost.systemName());
            case TurnEvent.DefenseHeld held -> isBattlePending(held.systemId())
                    ? I18n.t(UiTexts.ROUND_EVENT_BATTLE_READY, held.systemName())
                    : I18n.t(UiTexts.ROUND_EVENT_DEFENSE_HELD,
                    held.systemName(), held.attacking(), held.defendersLeft());
            case TurnEvent.Victory _ -> I18n.t(UiTexts.ROUND_EVENT_VICTORY, GameConfig.VICTORY_SYSTEM_PERCENT);
            case TurnEvent.Defeat defeat -> I18n.t(UiTexts.ROUND_EVENT_DEFEAT, defeat.winnerName());
        };
    }

    private void configureFleetGrid(IntConsumer onFleetRowSelected) {
        fleetGrid.addColumn(fleet -> String.valueOf(fleet.localNo())).setHeader(I18n.t(UiTexts.MAP_COLUMN_NO))
                .setSortable(true).setAutoWidth(true).setFlexGrow(0);
        fleetGrid.addColumn(FleetView::fromName).setHeader(I18n.t(UiTexts.MAP_COLUMN_FROM)).setAutoWidth(true);
        fleetGrid.addColumn(FleetView::toName).setHeader(I18n.t(UiTexts.MAP_COLUMN_TO)).setAutoWidth(true);
        fleetGrid.addColumn(FleetView::ships).setHeader(I18n.t(UiTexts.MAP_COLUMN_SHIPS)).setAutoWidth(true).setFlexGrow(0);
        fleetGrid.addColumn(FleetView::eta).setHeader(I18n.t(UiTexts.MAP_COLUMN_ETA)).setAutoWidth(true).setFlexGrow(0);
        fleetGrid.addColumn(fleet -> currentView == null ? "—" : String.valueOf(currentView.turn() + fleet.eta()))
                .setHeader(I18n.t(UiTexts.MAP_COLUMN_ARRIVAL_TURN)).setAutoWidth(true).setFlexGrow(0);
        fleetGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_COMPACT);
        fleetGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        fleetGrid.setHeight("min(58vh, 680px)");
        fleetGrid.addSelectionListener(event -> {
            if (!syncingSelection) {
                onFleetRowSelected.accept(event.getFirstSelectedItem().map(FleetView::fleetId).orElse(-1));
            }
        });
    }

    private void configureOrdersGrid(Consumer<PlannedOrder> onCancelOrder) {
        ordersGrid.addColumn(order -> I18n.t(order.type())).setHeader(I18n.t(UiTexts.MAP_COLUMN_ORDER_TYPE)).setAutoWidth(true);
        ordersGrid.addColumn(PlannedOrder::fromSystem).setHeader(I18n.t(UiTexts.MAP_COLUMN_ORDER_FROM)).setAutoWidth(true);
        ordersGrid.addColumn(PlannedOrder::toSystem).setHeader(I18n.t(UiTexts.MAP_COLUMN_ORDER_TO)).setAutoWidth(true);
        ordersGrid.addColumn(order -> value(order.ships())).setHeader(I18n.t(UiTexts.MAP_COLUMN_ORDER_SHIPS)).setAutoWidth(true);
        ordersGrid.addColumn(order -> value(order.arrivalTurn()))
                .setHeader(I18n.t(UiTexts.MAP_COLUMN_ARRIVAL_TURN)).setAutoWidth(true).setFlexGrow(0);
        ordersGrid.addComponentColumn(order -> {
            Button cancel = new Button(I18n.t(UiTexts.MAP_ACTION_CANCEL_ORDER), _ -> onCancelOrder.accept(order));
            cancel.setVisible(!readOnly);
            cancel.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.SMALL, ButtonVariant.TERTIARY);
            return cancel;
        }).setHeader("").setAutoWidth(true);
        ordersGrid.setAllRowsVisible(true);
    }

    private void configureRelocationsGrid() {
        relocationsGrid.addColumn(StandingOrderView::fromSystem).setHeader(I18n.t(UiTexts.MAP_COLUMN_STANDING_FROM)).setAutoWidth(true);
        relocationsGrid.addColumn(StandingOrderView::toSystem).setHeader(I18n.t(UiTexts.MAP_COLUMN_STANDING_TO)).setAutoWidth(true);
        relocationsGrid.addColumn(StandingOrderView::productionPerTurn)
                .setHeader(I18n.t(UiTexts.MAP_COLUMN_STANDING_PRODUCTION)).setAutoWidth(true);
        relocationsGrid.addComponentColumn(order -> {
            Button edit = new Button(I18n.t(UiTexts.MAP_ACTION_EDIT_STANDING), _ -> onEditStandingOrder.accept(order));
            edit.setVisible(!readOnly);
            edit.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);
            return edit;
        }).setHeader("").setAutoWidth(true).setFlexGrow(0);
        relocationsGrid.addComponentColumn(order -> {
            Button delete = new Button(I18n.t(UiTexts.MAP_ACTION_DELETE_STANDING), _ -> onDeleteStandingOrder.accept(order));
            delete.setVisible(!readOnly);
            delete.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY, ButtonVariant.ERROR);
            return delete;
        }).setHeader("").setAutoWidth(true).setFlexGrow(0);
        relocationsGrid.setAllRowsVisible(true);
    }

    private void syncFleetSelection() {
        syncingSelection = true;
        try {
            fleetGrid.getGenericDataView().getItems().filter(row -> Objects.equals(row.fleetId(), selectedFleetId))
                    .findFirst().ifPresentOrElse(fleetGrid::select, fleetGrid::deselectAll);
        } finally {
            syncingSelection = false;
        }
    }

    private void showSection(Section section) {
        contactsPage.setVisible(section == Section.CONTACTS);
        detailsPage.setVisible(section == Section.DETAILS);
        fleetsPage.setVisible(section == Section.FLEETS);
        ordersPage.setVisible(section == Section.ORDERS);
        relocationsPage.setVisible(section == Section.RELOCATIONS);
        reportPage.setVisible(section == Section.REPORT);
    }

    private void select(Section section) {
        tabs.setSelectedTab(tabFor(section));
        showSection(section);
    }

    private Tab tabFor(Section section) {
        return switch (section) {
            case CONTACTS -> sectionTabs.getFirst();
            case DETAILS -> sectionTabs.get(1);
            case FLEETS -> sectionTabs.get(2);
            case ORDERS -> sectionTabs.get(3);
            case RELOCATIONS -> sectionTabs.get(4);
            case REPORT -> reportTab;
        };
    }

    private Section sectionOf(Tab tab) {
        return Section.values()[sectionTabs.indexOf(tab)];
    }

    private static Tab tab(String key) {
        return new Tab(I18n.t(key));
    }

    private static VerticalLayout page() {
        VerticalLayout page = new VerticalLayout();
        page.setPadding(false);
        page.setSpacing(true);
        page.setWidthFull();
        return page;
    }

    private static H2 header(String key) {
        H2 title = new H2(I18n.t(key));
        title.getStyle().set(CssProperties.MARGIN, "0");
        return title;
    }

    private static H2 intelligenceHeader() {
        H2 title = new H2(I18n.t(UiTexts.MAP_SIDEBAR_INTELLIGENCE));
        title.addClassName("map-sidebar-intelligence-title");
        return title;
    }

    private static HorizontalLayout detail(String label, Object value) {
        Span labelSpan = new Span(label);
        Span valueSpan = new Span(String.valueOf(value));
        HorizontalLayout row = new HorizontalLayout(labelSpan, valueSpan);
        row.setWidthFull();
        row.addClassName("map-sidebar-detail");
        row.expand(labelSpan);
        return row;
    }

    private static String value(@Nullable Integer value) { return value == null ? "—" : String.valueOf(value); }
}
