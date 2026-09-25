package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.report.values.BattleReplay;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;
import de.zettsystems.starfare.style.CssProperties;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Switchable right-hand map sidebar that keeps the map context visible. */
final class FleetAndOrdersPanel extends VerticalLayout {

    private enum Section { CONTACTS, DETAILS, FLEETS, ORDERS, RELOCATIONS, LOGISTICS, REPORT }

    private enum EventCategory { PRODUCTION, REINFORCEMENT, BATTLE_WON, BATTLE_LOST, SYSTEM_LOST, DEFENSE_HELD }

    private enum LogisticsFilter { NET_INFLOW, FREE_CAPACITY, DELIVERY_BOTTLENECK, NO_OUTGOING_ROUTE }

    private final VerticalLayout contactsPage = page();
    private final VerticalLayout detailsPage = page();
    private final VerticalLayout fleetsPage = page();
    private final VerticalLayout ordersPage = page();
    private final VerticalLayout relocationsPage = page();
    private final VerticalLayout logisticsPage = page();
    private final VerticalLayout reportPage = page();
    private final List<VerticalLayout> pages = List.of(contactsPage, detailsPage, fleetsPage, ordersPage,
            relocationsPage, logisticsPage, reportPage);
    private final Tabs tabs;
    private final List<Tab> sectionTabs;
    private final Tab reportTab;

    private boolean readOnly;
    private boolean battlePresentationEnabled;
    private @Nullable GameId gameId;
    private @Nullable VisibleSystem selectedSystem;
    private @Nullable Integer selectedFleetId;
    private @Nullable Integer selectedStandingOrderId;
    private final EnumSet<EventCategory> enabledEventCategories = EnumSet.allOf(EventCategory.class);
    private final EnumSet<LogisticsFilter> enabledLogisticsFilters = EnumSet.noneOf(LogisticsFilter.class);
    private final IntConsumer onReportSystemSelected;
    private final IntConsumer onResolvedBattleSelected;
    private final IntConsumer onStandingOrderSelected;
    private final BiConsumer<Integer, Integer> onGarrisonReserveChanged;
    private final Consumer<StandingOrderView> onEditStandingOrder;
    private final Consumer<StandingOrderView> onDeleteStandingOrder;
    private final Runnable onBattleAcknowledged;
    private final BattleAcknowledgements acknowledgements;
    private @Nullable Integer selectedReportSystemId;
    private final IntConsumer onFleetRowSelected;
    private final Consumer<PlannedOrder> onCancelOrder;
    private String routeFilter = "";

    FleetAndOrdersPanel(BattleAcknowledgements acknowledgements,
                        Consumer<PlannedOrder> onCancelOrder,
                        Consumer<StandingOrderView> onEditStandingOrder,
                        Consumer<StandingOrderView> onDeleteStandingOrder,
                        Runnable onBattleAcknowledged,
                        IntConsumer onFleetRowSelected,
                        IntConsumer onStandingOrderSelected,
                        IntConsumer onReportSystemSelected,
                        IntConsumer onResolvedBattleSelected,
                        BiConsumer<Integer, Integer> onGarrisonReserveChanged) {
        this.onReportSystemSelected = onReportSystemSelected;
        this.onFleetRowSelected = onFleetRowSelected;
        this.onCancelOrder = onCancelOrder;
        this.onResolvedBattleSelected = onResolvedBattleSelected;
        this.onStandingOrderSelected = onStandingOrderSelected;
        this.onGarrisonReserveChanged = onGarrisonReserveChanged;
        this.onEditStandingOrder = onEditStandingOrder;
        this.onDeleteStandingOrder = onDeleteStandingOrder;
        this.onBattleAcknowledged = onBattleAcknowledged;
        this.acknowledgements = acknowledgements;
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
                tab(UiTexts.MAP_SIDEBAR_LOGISTICS),
                tab(UiTexts.MAP_SIDEBAR_REPORT));
        reportTab = sectionTabs.getLast();
        tabs = new Tabs();
        tabs.add(sectionTabs.toArray(Tab[]::new));
        tabs.setWidthFull();
        tabs.addClassName("map-sidebar-tabs");
        tabs.addSelectedChangeListener(event -> showSection(sectionOf(event.getSelectedTab())));

        add(tabs, contactsPage, detailsPage, fleetsPage, ordersPage, relocationsPage, reportPage);
        showSection(Section.CONTACTS);
    }

    void update(GameId gameId, PlayerViewState view, @Nullable VisibleSystem system, @Nullable Integer fleetId,
                @Nullable Integer standingOrderId,
                boolean spectator) {
        this.gameId = gameId;
        currentView = view;
        selectedSystem = system == null ? null : view.systems().stream()
                .filter(candidate -> candidate.id() == system.id()).findFirst().orElse(null);
        selectedFleetId = fleetId;
        selectedStandingOrderId = standingOrderId;
        readOnly = view.gameOver() || spectator;
        TurnReport report = view.report();
        int reportTurn = report != null ? report.turn() : view.turn() - 1;
        battlePresentationEnabled = BattlePresentationPreference.enabled(gameId, reportTurn,
                view.battlePresentationEnabled());
        renderTravelCards(view);
        renderContacts(view);
        renderDetails(view);
        renderLogistics(view);
        renderReport(view);
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

    void showLogistics() {
        select(Section.LOGISTICS);
    }

    private void renderLogistics(PlayerViewState view) {
        logisticsPage.removeAll();
        logisticsPage.add(header(UiTexts.MAP_SIDEBAR_LOGISTICS),
                new Paragraph(I18n.t(UiTexts.MAP_LOGISTICS_INTRO)));
        Map<Integer, LogisticsSystemSummary> summaries = LogisticsSystemSummary.forSystems(view.systems(),
                view.standingOrders());
        addLogisticsFilters();
        List<VisibleSystem> ownSystems = view.systems().stream()
                .filter(VisibleSystem::fullyVisible)
                .filter(system -> summaries.containsKey(system.id()))
                .filter(system -> matchesLogisticsFilters(logisticsSummary(summaries, system.id())))
                .sorted((left, right) -> Integer.compare(logisticsPriority(right, summaries),
                        logisticsPriority(left, summaries)))
                .toList();
        if (ownSystems.isEmpty()) {
            logisticsPage.add(new Paragraph(I18n.t(UiTexts.MAP_LOGISTICS_EMPTY)));
            return;
        }
        ownSystems.forEach(system -> logisticsPage.add(logisticsSystemCard(system,
                logisticsSummary(summaries, system.id()))));
        if (!view.standingOrders().isEmpty()) {
            Div routes = new Div();
            routes.addClassName("logistics-routes");
            view.standingOrders().forEach(order -> routes.add(logisticsRouteCard(order)));
            logisticsPage.add(header(UiTexts.MAP_LOGISTICS_ROUTES), routes);
        }
    }

    private void addLogisticsFilters() {
        VerticalLayout filters = new VerticalLayout();
        filters.setPadding(false);
        filters.setSpacing(false);
        addLogisticsFilter(filters, LogisticsFilter.NET_INFLOW, UiTexts.MAP_LOGISTICS_FILTER_NET_INFLOW);
        addLogisticsFilter(filters, LogisticsFilter.FREE_CAPACITY, UiTexts.MAP_LOGISTICS_FILTER_FREE_CAPACITY);
        addLogisticsFilter(filters, LogisticsFilter.DELIVERY_BOTTLENECK, UiTexts.MAP_LOGISTICS_FILTER_BOTTLENECK);
        addLogisticsFilter(filters, LogisticsFilter.NO_OUTGOING_ROUTE, UiTexts.MAP_LOGISTICS_FILTER_NO_OUTGOING);
        Details details = new Details(I18n.t(UiTexts.MAP_LOGISTICS_FILTERS), filters);
        logisticsPage.add(details);
    }

    private void addLogisticsFilter(VerticalLayout filters, LogisticsFilter filter, String key) {
        Checkbox checkbox = new Checkbox(I18n.t(key), enabledLogisticsFilters.contains(filter));
        checkbox.addValueChangeListener(event -> {
            if (event.getValue()) {
                enabledLogisticsFilters.add(filter);
            } else {
                enabledLogisticsFilters.remove(filter);
            }
            PlayerViewState view = currentView;
            if (view != null) {
                renderLogistics(view);
            }
        });
        filters.add(checkbox);
    }

    private boolean matchesLogisticsFilters(LogisticsSystemSummary summary) {
        return (!enabledLogisticsFilters.contains(LogisticsFilter.NET_INFLOW) || summary.netFlow() > 0)
                && (!enabledLogisticsFilters.contains(LogisticsFilter.FREE_CAPACITY)
                || summary.freePlanningCapacity() > 0)
                && (!enabledLogisticsFilters.contains(LogisticsFilter.DELIVERY_BOTTLENECK)
                || summary.deliveryBottleneck())
                && (!enabledLogisticsFilters.contains(LogisticsFilter.NO_OUTGOING_ROUTE)
                || summary.plannedOutgoing() == 0);
    }

    private static int logisticsPriority(VisibleSystem system, Map<Integer, LogisticsSystemSummary> summaries) {
        LogisticsSystemSummary summary = logisticsSummary(summaries, system.id());
        return (summary.deliveryBottleneck() ? 10_000 : 0) + Math.abs(summary.netFlow());
    }

    private static LogisticsSystemSummary logisticsSummary(Map<Integer, LogisticsSystemSummary> summaries, int systemId) {
        return Objects.requireNonNull(summaries.get(systemId));
    }

    private static Div logisticsSystemCard(VisibleSystem system, LogisticsSystemSummary summary) {
        Div card = new Div();
        card.addClassName("logistics-system-card");
        Span name = new Span(system.name());
        name.addClassName("logistics-system-name");
        Span direction = new Span(summary.netFlow() > 0 ? "↓ " + I18n.t(UiTexts.MAP_LOGISTICS_SINK)
                : summary.netFlow() < 0 ? "↑ " + I18n.t(UiTexts.MAP_LOGISTICS_SOURCE)
                : "◆ " + I18n.t(UiTexts.MAP_LOGISTICS_BALANCED));
        direction.addClassName("logistics-system-direction");
        Div metrics = new Div(logisticsMetric("⚙", "P", summary.production()),
                logisticsMetric("↓", "I", summary.plannedIncoming()),
                logisticsMetric("↑", "O", summary.plannedOutgoing()),
                logisticsMetric("Σ", "P+I−O", summary.plannedAccumulation()));
        metrics.addClassName("logistics-metrics");
        Span reserve = new Span(I18n.t(UiTexts.MAP_LOGISTICS_RESERVE_AVAILABLE,
                summary.reserve(), summary.availableShips()));
        reserve.addClassName("logistics-system-reserve");
        Span delivery = new Span(I18n.t(summary.deliveryBottleneck()
                ? UiTexts.MAP_LOGISTICS_DELIVERY_BOTTLENECK : UiTexts.MAP_LOGISTICS_NEXT_DELIVERY,
                summary.nextDelivery()));
        delivery.addClassName(summary.deliveryBottleneck() ? "logistics-delivery-bottleneck" : "logistics-delivery");
        card.add(name, direction, metrics, reserve, delivery);
        return card;
    }

    private Div logisticsRouteCard(StandingOrderView order) {
        Div card = new Div();
        card.addClassName("logistics-route-card");
        Button edit = new Button(I18n.t(UiTexts.MAP_ACTION_EDIT_STANDING), _ -> onEditStandingOrder.accept(order));
        edit.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);
        card.add(new Span(order.fromSystem() + " → " + order.toSystem()),
                new Span("◆ " + order.ships() + " / " + I18n.t(UiTexts.MAP_LOGISTICS_PER_TURN)), edit);
        card.addClickListener(_ -> onStandingOrderSelected.accept(order.id()));
        card.getElement().setAttribute("role", "button");
        card.getElement().setAttribute("tabindex", "0");
        return card;
    }

    private static Span logisticsMetric(String icon, String label, int value) {
        Span metric = new Span(icon + " " + label + " " + value);
        metric.addClassName("logistics-metric");
        return metric;
    }

    private static int valueOrZero(@Nullable Integer value) {
        return value == null ? 0 : value;
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
            H2 systemHeading = new H2(system.name());
            systemHeading.addClassName("system-details-heading");
            detailsPage.add(systemHeading, detail(I18n.t(UiTexts.MAP_SIDEBAR_OWNER), ownerName(view, system.ownerId())));
            Div metrics = new Div(
                    systemMetric("🛡", I18n.t(UiTexts.MAP_SIDEBAR_GARRISON), value(system.garrison())),
                    systemMetric("⚙", I18n.t(UiTexts.MAP_SIDEBAR_PRODUCTION),
                            value(system.productionPerTurn())));
            if (system.garrisonReserve() != null) {
                metrics.add(systemMetric("⛨", I18n.t(UiTexts.MAP_SIDEBAR_GARRISON_RESERVE),
                        system.garrisonReserve()));
            }
            if (system.availableShips() != null) {
                metrics.add(systemMetric("➤", I18n.t(UiTexts.MAP_SIDEBAR_SHIPS), system.availableShips()));
            }
            if (system.routedProduction() != null) {
                ProductionFlow flow = ProductionFlow.at(system.id(), view.standingOrders());
                detailsPage.add(metrics, systemFlow(system, flow));
            } else {
                detailsPage.add(metrics);
            }
            metrics.addClassName("system-metrics");
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
        TurnReport report = view.report();
        List<TurnEvent> events = report != null ? report.events() : List.of();
        VerticalLayout filterOptions = new VerticalLayout();
        filterOptions.setPadding(false);
        filterOptions.setSpacing(false);
        filterOptions.addClassName("report-filter-options");
        addReportFilter(filterOptions, events, EventCategory.PRODUCTION, UiTexts.ROUND_FILTER_PRODUCTION);
        addReportFilter(filterOptions, events, EventCategory.REINFORCEMENT, UiTexts.ROUND_FILTER_REINFORCEMENT);
        addReportFilter(filterOptions, events, EventCategory.BATTLE_WON, UiTexts.ROUND_FILTER_BATTLE_WON);
        addReportFilter(filterOptions, events, EventCategory.BATTLE_LOST, UiTexts.ROUND_FILTER_BATTLE_LOST);
        addReportFilter(filterOptions, events, EventCategory.SYSTEM_LOST, UiTexts.ROUND_FILTER_SYSTEM_LOST);
        addReportFilter(filterOptions, events, EventCategory.DEFENSE_HELD, UiTexts.ROUND_FILTER_DEFENSE_HELD);
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
        filterOptions.add(presentation);
        Details filterDetails = new Details(new Span(I18n.t(UiTexts.ROUND_FILTER_SUMMARY,
                enabledEventCategories.size(), EventCategory.values().length)), filterOptions);
        filterDetails.addClassName("report-filter-details");
        filters.add(filterDetails);
        List<Integer> pendingBattles = pendingBattleIndices(events);
        if (!pendingBattles.isEmpty()) {
            Button nextBattle = new Button(I18n.t(UiTexts.ROUND_NEXT_OPEN_BATTLE),
                    _ -> openNextPendingBattle(events, pendingBattles.getFirst()));
            nextBattle.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.SMALL);
            nextBattle.setTooltipText(I18n.t(UiTexts.ROUND_OPEN_BATTLES, pendingBattles.size()));
            filters.add(nextBattle);
        }
        reportPage.add(filters);
        List<Integer> filtered = java.util.stream.IntStream.range(0, events.size()).boxed()
                .filter(eventIndex -> isEventEnabled(events.get(eventIndex)))
                .sorted(java.util.Comparator.comparingInt(this::eventPriority)).toList();
        if (events.isEmpty()) {
            reportPage.add(new Paragraph(I18n.t(UiTexts.ROUND_NO_EVENTS)));
            return;
        }
        if (filtered.isEmpty()) {
            reportPage.add(new Paragraph(I18n.t(UiTexts.ROUND_NO_EVENTS_FILTERED)));
            return;
        }
        for (int index = 0; index < filtered.size(); index++) {
            reportPage.add(eventCard(events.get(filtered.get(index)), filtered.get(index), index));
        }
    }

    private void addReportFilter(VerticalLayout filters, List<TurnEvent> events,
                                 EventCategory category, String key) {
        long count = events.stream().filter(event -> categoryOf(event).filter(category::equals).isPresent()).count();
        Checkbox filter = new Checkbox(I18n.t(key) + " (" + count + ")", enabledEventCategories.contains(category));
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

    private int eventPriority(int eventIndex) {
        PlayerViewState view = currentView;
        TurnReport report = view == null ? null : view.report();
        List<TurnEvent> events = report == null ? List.of() : report.events();
        TurnEvent event = events.get(eventIndex);
        if (isEventPending(eventIndex)) {
            return 0;
        }
        return switch (event) {
            case TurnEvent.BattleWon _, TurnEvent.BattleLost _, TurnEvent.SystemLost _, TurnEvent.DefenseHeld _ -> 1;
            case TurnEvent.Victory _, TurnEvent.Defeat _ -> 2;
            case TurnEvent.Reinforcement _ -> 3;
            case TurnEvent.Production _ -> 4;
        };
    }

    private List<Integer> pendingBattleIndices(List<TurnEvent> events) {
        return java.util.stream.IntStream.range(0, events.size()).boxed().filter(this::isEventPending).toList();
    }

    private void openNextPendingBattle(List<TurnEvent> events, int eventIndex) {
        TurnEvent event = events.get(eventIndex);
        BattleReplay replay = BattleReplay.from(event).orElse(null);
        event.battleSystemId().ifPresent(onReportSystemSelected);
        Runnable acknowledge = battleAcknowledgement(event, eventIndex);
        if (battlePresentationEnabled && replay != null) {
            BattleReplayDialog.open(replay, battleSides(event), acknowledge);
        } else {
            acknowledge.run();
        }
    }

    private @Nullable PlayerViewState currentView;

    private boolean isEventEnabled(TurnEvent event) {
        PlayerViewState view = currentView;
        TurnReport report = view == null ? null : view.report();
        if ((event instanceof TurnEvent.Victory || event instanceof TurnEvent.Defeat)
                && gameId != null && report != null
                && !acknowledgements.pending(gameId, report).isEmpty()) {
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

    private Div eventCard(TurnEvent event, int eventIndex, int index) {
        Runnable acknowledge = battleAcknowledgement(event, eventIndex);
        Div card = new Div();
        card.addClassNames("event-card", eventCss(event, eventIndex));
        card.getStyle().set(CssProperties.ANIMATION_DELAY, (index * 0.1) + "s");
        card.add(new Span(eventIcon(event, eventIndex)), new Span(eventText(event, eventIndex)));
        BattleReplay replay = BattleReplay.from(event).orElse(null);
        event.battleSystemId().ifPresent(systemId -> {
            card.addClassName("event-map-linked");
            if (Objects.equals(selectedReportSystemId, systemId)) {
                card.addClassName("event-map-selected");
            }
            if (replay == null) {
                card.addClickListener(_ -> onReportSystemSelected.accept(systemId));
            } else if (!isEventPending(eventIndex)) {
                card.addClickListener(_ -> onResolvedBattleSelected.accept(systemId));
            }
        });
        if (battlePresentationEnabled && replay != null && event.battleSystemId().isPresent()
                && isEventPending(eventIndex)) {
            card.addClassName("event-battle-interactive");
            card.addClickListener(_ -> {
                onReportSystemSelected.accept(event.battleSystemId().getAsInt());
                BattleReplayDialog.open(replay, battleSides(event), acknowledge);
            });
            return card;
        }
        if (event.battleSystemId().isPresent() && isEventPending(eventIndex)) {
            card.addClassName("event-battle-interactive");
            card.addClickListener(_ -> acknowledge.run());
            return card;
        }
        return card;
    }

    private static BattleReplayDialog.BattleSides battleSides(TurnEvent event) {
        return switch (event) {
            case TurnEvent.BattleWon battle -> new BattleReplayDialog.BattleSides(youName(battle.attackerName()),
                    battle.wasNeutral() ? I18n.t(UiTexts.BATTLE_REPLAY_NEUTRAL) : opponentName(battle.defenderName()));
            case TurnEvent.BattleLost battle -> new BattleReplayDialog.BattleSides(youName(battle.attackerName()),
                    opponentName(battle.defenderName()));
            case TurnEvent.SystemLost lost -> new BattleReplayDialog.BattleSides(opponentName(lost.attackerName()),
                    youName(lost.defenderName()));
            case TurnEvent.DefenseHeld held -> new BattleReplayDialog.BattleSides(opponentName(held.attackerName()),
                    youName(held.defenderName()));
            case TurnEvent.Production _, TurnEvent.Reinforcement _, TurnEvent.Victory _, TurnEvent.Defeat _ ->
                    new BattleReplayDialog.BattleSides("", "");
        };
    }

    private static String youName(String name) {
        return name.isBlank() ? I18n.t(UiTexts.BATTLE_REPLAY_YOU) : I18n.t(UiTexts.BATTLE_REPLAY_YOU) + " · " + name;
    }

    private static String opponentName(String name) {
        return name.isBlank() ? I18n.t(UiTexts.BATTLE_REPLAY_OPPONENT) : name;
    }

    private boolean isBattlePending(int systemId) {
        GameId current = gameId;
        TurnReport report = currentView == null ? null : currentView.report();
        return current != null && report != null && acknowledgements.pending(current, report).contains(systemId);
    }

    private boolean isEventPending(int eventIndex) {
        GameId current = gameId;
        TurnReport report = currentView == null ? null : currentView.report();
        return current != null && report != null && acknowledgements.isPending(current, report, eventIndex);
    }
    private Runnable battleAcknowledgement(TurnEvent event, int eventIndex) {
        GameId current = gameId;
        TurnReport report = currentView == null ? null : currentView.report();
        // Capture the report shown by the card, not whichever round is current on dialog close.
        return () -> {
            if (current != null && report != null) {
                acknowledgements.acknowledge(current, report, eventIndex);
                onBattleAcknowledged.run();
                showCaptureSummary(event);
            }
        };
    }

    private void showCaptureSummary(TurnEvent event) {
        if (!(event instanceof TurnEvent.BattleWon battle)) {
            return;
        }
        PlayerViewState view = currentView;
        if (view == null) {
            return;
        }
        VisibleSystem system = view.systems().stream()
                .filter(candidate -> candidate.id() == battle.systemId()).findFirst().orElse(null);
        if (system == null || !system.fullyVisible()) {
            return;
        }
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(I18n.t(UiTexts.MAP_CAPTURE_SUMMARY_TITLE, system.name()));
        dialog.addClassName("capture-summary-dialog");
        Div metrics = new Div(
                systemMetric("🛡", I18n.t(UiTexts.MAP_SIDEBAR_GARRISON), value(system.garrison())),
                systemMetric("⚙", I18n.t(UiTexts.MAP_SIDEBAR_PRODUCTION), value(system.productionPerTurn())));
        metrics.addClassName("system-metrics");
        dialog.add(metrics);
        Button close = new Button(I18n.t(UiTexts.MAP_CAPTURE_SUMMARY_CLOSE), _ -> dialog.close());
        close.addThemeVariants(ButtonVariant.PRIMARY);
        dialog.getFooter().add(close);
        dialog.open();
    }

    void selectReportSystem(int systemId) {
        selectedReportSystemId = systemId;
        showReport();
        PlayerViewState view = currentView;
        if (view != null) {
            renderReport(view);
        }
    }

    private String eventIcon(TurnEvent event, int eventIndex) {
        if (event.battleSystemId().isPresent() && isEventPending(eventIndex)) {
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

    private String eventCss(TurnEvent event, int eventIndex) {
        if (event.battleSystemId().isPresent() && isEventPending(eventIndex)) {
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

    private String eventText(TurnEvent event, int eventIndex) {
        return switch (event) {
            case TurnEvent.Production production -> I18n.t(UiTexts.ROUND_EVENT_PRODUCTION,
                    production.systemName(), production.amount());
            case TurnEvent.Reinforcement reinforcement -> I18n.t(UiTexts.ROUND_EVENT_REINFORCEMENT,
                    reinforcement.systemName(), reinforcement.ships(), reinforcement.totalGarrison(), reinforcement.fleetLabel());
            case TurnEvent.BattleWon battle -> isEventPending(eventIndex)
                    ? I18n.t(UiTexts.ROUND_EVENT_BATTLE_READY, battle.systemName())
                    : I18n.t(battle.wasNeutral() ? UiTexts.ROUND_EVENT_BATTLE_WON_NEUTRAL
                    : UiTexts.ROUND_EVENT_BATTLE_WON_ENEMY, battle.systemName(), battle.attacking(),
                    battle.defending(), battle.remaining());
            case TurnEvent.BattleLost battle -> isEventPending(eventIndex)
                    ? I18n.t(UiTexts.ROUND_EVENT_BATTLE_READY, battle.systemName())
                    : I18n.t(UiTexts.ROUND_EVENT_BATTLE_LOST, battle.systemName(), battle.attacking(),
                    battle.defending(), battle.defendersLeft());
            case TurnEvent.SystemLost lost -> isEventPending(eventIndex)
                    ? I18n.t(UiTexts.ROUND_EVENT_BATTLE_READY, lost.systemName())
                    : I18n.t(UiTexts.ROUND_EVENT_SYSTEM_LOST, lost.systemName());
            case TurnEvent.DefenseHeld held -> isEventPending(eventIndex)
                    ? I18n.t(UiTexts.ROUND_EVENT_BATTLE_READY, held.systemName())
                    : I18n.t(UiTexts.ROUND_EVENT_DEFENSE_HELD,
                    held.systemName(), held.attacking(), held.defendersLeft());
            case TurnEvent.Victory _ -> I18n.t(UiTexts.ROUND_EVENT_VICTORY, GameConfig.VICTORY_SYSTEM_PERCENT);
            case TurnEvent.Defeat defeat -> I18n.t(UiTexts.ROUND_EVENT_DEFEAT, defeat.winnerName());
        };
    }

    private void renderTravelCards(PlayerViewState view) {
        fleetsPage.removeAll();
        ordersPage.removeAll();
        relocationsPage.removeAll();
        TextField filter = routeFilter();
        fleetsPage.add(header(UiTexts.MAP_OWN_FLEETS), filter);
        ordersPage.add(header(UiTexts.MAP_PLANNED_ORDERS), routeFilter());
        relocationsPage.add(header(UiTexts.MAP_STANDING_ORDERS_HEADER), routeFilter());

        List<FleetView> fleets = UiMapper.toFleetViews(view).stream()
                .filter(fleet -> matchesRoute(fleet.fromName(), fleet.toName()))
                .sorted(java.util.Comparator.comparingInt((FleetView fleet) -> view.turn() + fleet.eta())
                        .thenComparingInt(FleetView::fleetId))
                .toList();
        addArrivalGroups(fleetsPage, fleets, fleet -> view.turn() + fleet.eta(), this::fleetCard);

        List<PlannedOrder> orders = view.plannedOrders().stream()
                .filter(order -> matchesRoute(order.fromSystem(), order.toSystem()))
                .sorted(java.util.Comparator.comparingInt(this::orderArrival).thenComparingInt(PlannedOrder::index))
                .toList();
        addArrivalGroups(ordersPage, orders, this::orderArrival, this::orderCard);

        List<StandingOrderView> relocations = view.standingOrders().stream()
                .filter(order -> matchesRoute(order.fromSystem(), order.toSystem()))
                .sorted(java.util.Comparator.comparing(StandingOrderView::fromSystem)
                        .thenComparing(StandingOrderView::toSystem).thenComparingInt(StandingOrderView::id))
                .toList();
        if (relocations.isEmpty()) {
            relocationsPage.add(emptyTravelState());
        } else {
            relocations.forEach(order -> relocationsPage.add(relocationCard(order)));
        }
    }

    private TextField routeFilter() {
        TextField filter = new TextField(I18n.t(UiTexts.MAP_TRAVEL_FILTER));
        filter.setPlaceholder(I18n.t(UiTexts.MAP_TRAVEL_FILTER_PLACEHOLDER));
        filter.setValue(routeFilter);
        filter.setClearButtonVisible(true);
        filter.addValueChangeListener(event -> {
            routeFilter = event.getValue();
            PlayerViewState view = currentView;
            if (view != null) {
                renderTravelCards(view);
            }
        });
        return filter;
    }

    private boolean matchesRoute(String from, String to) {
        String filter = routeFilter.strip();
        return filter.isEmpty() || (from + " " + to).toLowerCase(java.util.Locale.ROOT)
                .contains(filter.toLowerCase(java.util.Locale.ROOT));
    }

    private <T> void addArrivalGroups(VerticalLayout page, List<T> items,
                                      java.util.function.ToIntFunction<T> arrival,
                                      java.util.function.Function<T, Div> card) {
        if (items.isEmpty()) {
            page.add(emptyTravelState());
            return;
        }
        int previousArrival = Integer.MIN_VALUE;
        for (T item : items) {
            int itemArrival = arrival.applyAsInt(item);
            if (itemArrival != previousArrival) {
                page.add(arrivalGroup(itemArrival));
                previousArrival = itemArrival;
            }
            page.add(card.apply(item));
        }
    }

    private static Span arrivalGroup(int arrivalTurn) {
        Span group = new Span(I18n.t(UiTexts.MAP_TRAVEL_ARRIVAL_GROUP, arrivalTurn));
        group.addClassName("fleet-travel-group");
        return group;
    }

    private static Paragraph emptyTravelState() {
        Paragraph empty = new Paragraph(I18n.t(UiTexts.MAP_TRAVEL_EMPTY));
        empty.addClassName("fleet-travel-empty");
        return empty;
    }

    private int orderArrival(PlannedOrder order) {
        Integer arrival = order.arrivalTurn();
        return arrival != null ? arrival : Integer.MAX_VALUE;
    }

    private Div fleetCard(FleetView fleet) {
        Div card = new Div();
        card.addClassName("fleet-travel-card");
        Div top = new Div();
        top.addClassName("fleet-travel-card-top");
        top.add(new Span("✦ #" + fleet.localNo()), fleetNumber(fleet.ships()));
        Span route = new Span(fleet.fromName() + " → " + fleet.toName());
        route.addClassName("fleet-travel-route");
        int arrivalTurn = currentView == null ? -1 : currentView.turn() + fleet.eta();
        Span arrival = new Span(I18n.t(UiTexts.MAP_COLUMN_ARRIVAL_TURN) + ": "
                + (arrivalTurn < 0 ? "—" : arrivalTurn) + " · " + I18n.t(UiTexts.MAP_COLUMN_ETA) + ": " + fleet.eta());
        arrival.addClassName("fleet-travel-arrival");
        card.add(top, route, travelProgress(fleet), arrival);
        card.addClickListener(_ -> onFleetRowSelected.accept(fleet.fleetId()));
        card.getElement().setAttribute("role", "button");
        card.getElement().setAttribute("tabindex", "0");
        if (Integer.valueOf(fleet.fleetId()).equals(selectedFleetId)) {
            card.addClassName("fleet-travel-card-selected");
        }
        return card;
    }

    private Div orderCard(PlannedOrder order) {
        Div card = new Div();
        card.addClassNames("fleet-travel-card", "fleet-travel-card-planned");
        Div top = new Div();
        top.addClassName("fleet-travel-card-top");
        top.add(new Span("◌ " + I18n.t(order.type())), fleetNumber(value(order.ships())));
        Span route = new Span(order.fromSystem() + " → " + order.toSystem());
        route.addClassName("fleet-travel-route");
        Span arrival = new Span(I18n.t(UiTexts.MAP_COLUMN_ARRIVAL_TURN) + ": " + value(order.arrivalTurn()));
        arrival.addClassName("fleet-travel-arrival");
        card.add(top, route, arrival);
        if (!readOnly) {
            Button cancel = new Button(I18n.t(UiTexts.MAP_ACTION_CANCEL_ORDER), _ -> onCancelOrder.accept(order));
            cancel.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.SMALL, ButtonVariant.TERTIARY);
            card.add(cancel);
        }
        return card;
    }

    private Div travelProgress(FleetView fleet) {
        int elapsed = currentView == null ? 0 : Math.max(0, currentView.turn()
                - currentView.ownFleets().stream().filter(candidate -> candidate.globalId() == fleet.fleetId())
                .map(Fleet::launchTurn).findFirst().orElse(currentView.turn()));
        int total = elapsed + fleet.eta();
        int percent = total == 0 ? 100 : Math.round(100f * elapsed / total);
        Div track = new Div();
        track.addClassName("fleet-travel-progress");
        Div fill = new Div();
        fill.addClassName("fleet-travel-progress-fill");
        fill.getStyle().set(CssProperties.WIDTH, percent + "%");
        track.add(fill);
        track.getElement().setAttribute("aria-label", I18n.t(UiTexts.MAP_FLEET_TRAVEL_PROGRESS, elapsed, total));
        return track;
    }

    private static Span fleetNumber(int ships) {
        return fleetNumber(String.valueOf(ships));
    }

    private static Span fleetNumber(String ships) {
        Span number = new Span("◆ " + ships);
        number.addClassName("fleet-travel-ships");
        return number;
    }

    private Div relocationCard(StandingOrderView order) {
        Div card = new Div();
        card.addClassNames("fleet-travel-card", "fleet-travel-card-relocation");
        card.add(new Span("⇢ " + I18n.t(UiTexts.MAP_ORDER_TYPE_STANDING)),
                new Span(order.fromSystem() + " → " + order.toSystem()),
                fleetNumber(order.ships() + " / " + I18n.t(UiTexts.MAP_LOGISTICS_PER_TURN)));
        if (!readOnly) {
            Button edit = new Button(I18n.t(UiTexts.MAP_ACTION_EDIT_STANDING), _ -> onEditStandingOrder.accept(order));
            edit.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);
            Button delete = new Button(I18n.t(UiTexts.MAP_ACTION_DELETE_STANDING), _ -> onDeleteStandingOrder.accept(order));
            delete.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY, ButtonVariant.ERROR);
            card.add(new HorizontalLayout(edit, delete));
        }
        card.addClickListener(_ -> onStandingOrderSelected.accept(order.id()));
        card.getElement().setAttribute("role", "button");
        card.getElement().setAttribute("tabindex", "0");
        if (Integer.valueOf(order.id()).equals(selectedStandingOrderId)) {
            card.addClassName("fleet-travel-card-selected");
        }
        return card;
    }

    private void showSection(Section section) {
        contactsPage.setVisible(section == Section.CONTACTS);
        detailsPage.setVisible(section == Section.DETAILS);
        fleetsPage.setVisible(section == Section.FLEETS);
        ordersPage.setVisible(section == Section.ORDERS);
        relocationsPage.setVisible(section == Section.RELOCATIONS);
        logisticsPage.setVisible(section == Section.LOGISTICS);
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
            case LOGISTICS -> sectionTabs.get(5);
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

    private static Div systemMetric(String icon, String label, Object value) {
        Span iconSpan = new Span(icon);
        iconSpan.addClassName("system-metric-icon");
        iconSpan.getElement().setAttribute("title", label);
        iconSpan.getElement().setAttribute("aria-label", label);
        Span valueSpan = new Span(String.valueOf(value));
        valueSpan.addClassName("system-metric-value");
        Div metric = new Div(iconSpan, valueSpan);
        metric.addClassName("system-metric");
        metric.getElement().setAttribute("aria-label", label + ": " + value);
        return metric;
    }

    private static Div systemFlow(VisibleSystem system, ProductionFlow flow) {
        int production = valueOrZero(system.productionPerTurn());
        Div flowCard = new Div();
        flowCard.addClassName("system-flow");
        flowCard.add(flowStep("↓", I18n.t(UiTexts.MAP_PRODUCTION_INCOMING), flow.incoming()),
                flowStep("⚙", I18n.t(UiTexts.MAP_SIDEBAR_PRODUCTION), production),
                flowStep("↑", I18n.t(UiTexts.MAP_PRODUCTION_OUTGOING), flow.outgoing()));
        flowCard.getElement().setAttribute("aria-label", I18n.t(UiTexts.MAP_PRODUCTION_FLOW,
                production, flow.incoming(), flow.outgoing()));
        return flowCard;
    }

    private static Span flowStep(String icon, String label, int value) {
        Span step = new Span(icon + " " + value);
        step.addClassName("system-flow-step");
        step.getElement().setAttribute("title", label);
        step.getElement().setAttribute("aria-label", label + ": " + value);
        return step;
    }

    private static String value(@Nullable Integer value) { return value == null ? "—" : String.valueOf(value); }
}
