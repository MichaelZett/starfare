package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import org.jspecify.annotations.Nullable;
import de.zettsystems.starfare.economy.ui.IndustryPanel;
import de.zettsystems.starfare.economy.values.EconomyIssues;

/** Switchable right-hand map sidebar that keeps the map context visible. */
final class FleetAndOrdersPanel extends VerticalLayout {

    private enum Section { CONTACTS, DETAILS, FLEETS, ORDERS, RELOCATIONS, LOGISTICS, REPORT }

    private enum EventCategory { COALITION, PRODUCTION, REINFORCEMENT, BATTLE_WON, BATTLE_LOST, SYSTEM_LOST, DEFENSE_HELD }

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
    private final Div navigation = new Div();
    private final ComboBox<Section> compactNavigation = new ComboBox<>();
    private final List<Tab> sectionTabs;
    private final Tab reportTab;
    private boolean reportAvailable = true;

    private boolean readOnly;
    private Runnable onMining = () -> { };
    private BiConsumer<Integer, Boolean> onMiningProtection = (_, _) -> { };
    private java.util.Set<Integer> automatedSystems = java.util.Set.of();
    private java.util.Set<Integer> protectedSystems = java.util.Set.of();
    void onMining(Runnable action) { onMining = action; }
    void onMiningProtection(BiConsumer<Integer, Boolean> action) { onMiningProtection = action; }
    java.util.Set<Integer> protectedProductionSystems() { return productionInputs.protectedSystems(); }
    void miningPolicy(de.zettsystems.starfare.economy.values.@Nullable MiningAutomation policy) {
        automatedSystems = policy != null && policy.enabled() ? policy.systems() : java.util.Set.of();
        protectedSystems = policy != null && policy.enabled() ? policy.protectedSystems() : java.util.Set.of();
    }
    private final de.zettsystems.starfare.economy.values.ProductionInputs productionInputs = new de.zettsystems.starfare.economy.values.ProductionInputs();
    private @Nullable IndustryPanel industryPanel;
    private EconomyIssues.@Nullable Kind economyFilter;
    private boolean economyFiltered;
    private boolean sortByYield;
    private BiConsumer<Integer, Integer> onEconomySelected = (_, _) -> { };
    void onEconomySelected(BiConsumer<Integer, Integer> action) { onEconomySelected = action; }
    void openProduction(int index) {
        var panel = industryPanel;
        if (index >= 0 && panel != null) { panel.focusProduction(index); }
        if (index < 0) { detailsPage.getChildren().filter(Details.class::isInstance)
                .map(Details.class::cast).filter(details -> details.hasClassName("system-operations"))
                .forEach(details -> details.setOpened(true)); }
    }
    void showEconomy(EconomyIssues.Kind kind) {
        economyFilter = kind; economyFiltered = true;
        var view = currentView;
        if (view != null) { renderLogistics(view); }
        showLogistics();
    }
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
    private final Consumer<VisibleSystem> onFleetTargetSelection;
    private final Consumer<VisibleSystem> onRelocationTargetSelection;
    private final Runnable onTargetSelectionCancelled;
    private final Consumer<StandingOrderView> onEditStandingOrder;
    private final Consumer<StandingOrderView> onDeleteStandingOrder;
    private final Runnable onBattleAcknowledged;
    private final BattleAcknowledgements acknowledgements;
    private @Nullable Integer selectedReportSystemId;
    private final IntConsumer onFleetRowSelected;
    private IntConsumer onRouteStationSelected = _ -> { };
    private Runnable onDiplomacySelected = () -> { };
    private boolean diplomacyAvailable;
    private java.util.Set<Integer> alliedOwners = java.util.Set.of();
    void alliedOwners(java.util.Set<Integer> owners) { alliedOwners = owners; }
    void diplomacyAvailable(boolean available) { diplomacyAvailable = available; }
    void onRouteStationSelected(IntConsumer action) { onRouteStationSelected = action; }
    void onDiplomacySelected(Runnable action) { onDiplomacySelected = action; }
    private final Consumer<PlannedOrder> onCancelOrder;
    private String routeFilter = "";
    private Consumer<de.zettsystems.starfare.economy.values.RecyclingOrder> onRecycling = _ -> { };
    void onRecycling(Consumer<de.zettsystems.starfare.economy.values.RecyclingOrder> action) { onRecycling = action; }
    private java.util.function.Function<de.zettsystems.starfare.economy.values.ProductionAllocation, de.zettsystems.starfare.economy.values.ProductionCommit> onProductionAllocation = _ -> de.zettsystems.starfare.economy.values.ProductionCommit.REJECTED;
    private java.util.function.Function<de.zettsystems.starfare.economy.values.ProductionAllocation, java.util.Optional<de.zettsystems.starfare.economy.values.ProductionPlanPreview>> onPreview = _ -> java.util.Optional.empty();
    void onProductionPreview(java.util.function.Function<de.zettsystems.starfare.economy.values.ProductionAllocation, java.util.Optional<de.zettsystems.starfare.economy.values.ProductionPlanPreview>> action) { onPreview = action; }
    void onProductionAllocation(java.util.function.Function<de.zettsystems.starfare.economy.values.ProductionAllocation, de.zettsystems.starfare.economy.values.ProductionCommit> action) { onProductionAllocation = action; }

    private IntConsumer onFleetWait = _ -> { };
    private IntConsumer onFleetDisband = _ -> { };
    void onFleetWait(IntConsumer action) { onFleetWait = action; }
    void onFleetDisband(IntConsumer action) { onFleetDisband = action; }

    private Consumer<PlannedOrder> onEditOrder = _ -> { };

    void onEditOrder(Consumer<PlannedOrder> action) { onEditOrder = action; }

    private void restoreFilters() {
        routeFilter = DisplayPreferences.get(de.zettsystems.starfare.social.values.DisplaySetting.ROUTE_FILTER, "");
        String events = DisplayPreferences.get(de.zettsystems.starfare.social.values.DisplaySetting.EVENT_FILTERS, "*");
        if (!events.equals("*")) {
            enabledEventCategories.removeIf(category -> !List.of(events.split(",")).contains(category.name()));
        }
        String logistics = DisplayPreferences.get(de.zettsystems.starfare.social.values.DisplaySetting.LOGISTICS_FILTERS, "");
        for (LogisticsFilter filter : LogisticsFilter.values()) {
            if (List.of(logistics.split(",")).contains(filter.name())) { enabledLogisticsFilters.add(filter); }
        }
    }

    private void saveFilters() {
        DisplayPreferences.choose(de.zettsystems.starfare.social.values.DisplaySetting.ROUTE_FILTER, routeFilter);
        DisplayPreferences.choose(de.zettsystems.starfare.social.values.DisplaySetting.EVENT_FILTERS,
                enabledEventCategories.stream().map(Enum::name).collect(java.util.stream.Collectors.joining(",")));
        DisplayPreferences.choose(de.zettsystems.starfare.social.values.DisplaySetting.LOGISTICS_FILTERS,
                enabledLogisticsFilters.stream().map(Enum::name).collect(java.util.stream.Collectors.joining(",")));
    }
    private final VerticalLayout fleetCards = page();
    private final VerticalLayout orderCards = page();
    private final VerticalLayout relocationCards = page();
    private final List<TextField> routeFilters = new java.util.ArrayList<>();
    private @Nullable Object lastDetails;
    private @Nullable DetailedSystemPanel detailedSystem;
    private Object detailedSystemKey = "";
    private boolean fleetArrivalsOpened;
    private final java.util.Set<Integer> openedFleetDetails = new java.util.HashSet<>();
    private final java.util.Set<Integer> expandedFleetRoutes = new java.util.HashSet<>();
    private boolean systemOperationsOpened;
    private boolean systemBackgroundOpened;
    private record ReserveEditor(int systemId, int savedReserve, IntegerField input) { }
    private @Nullable ReserveEditor reserveEditor;
    private @Nullable Object lastTravel;
    private @Nullable Object lastLogistics;
    private @Nullable Object lastReport;

    FleetAndOrdersPanel(BattleAcknowledgements acknowledgements,
                        Consumer<PlannedOrder> onCancelOrder,
                        Consumer<StandingOrderView> onEditStandingOrder,
                        Consumer<StandingOrderView> onDeleteStandingOrder,
                        Runnable onBattleAcknowledged,
                        IntConsumer onFleetRowSelected,
                        IntConsumer onStandingOrderSelected,
                        IntConsumer onReportSystemSelected,
                        IntConsumer onResolvedBattleSelected,
                        BiConsumer<Integer, Integer> onGarrisonReserveChanged,
                        Consumer<VisibleSystem> onFleetTargetSelection,
                        Consumer<VisibleSystem> onRelocationTargetSelection,
                        Runnable onTargetSelectionCancelled) {
        this.onReportSystemSelected = onReportSystemSelected;
        this.onFleetRowSelected = onFleetRowSelected;
        this.onCancelOrder = onCancelOrder;
        this.onResolvedBattleSelected = onResolvedBattleSelected;
        this.onStandingOrderSelected = onStandingOrderSelected;
        this.onGarrisonReserveChanged = onGarrisonReserveChanged;
        this.onFleetTargetSelection = onFleetTargetSelection;
        this.onRelocationTargetSelection = onRelocationTargetSelection;
        this.onTargetSelectionCancelled = onTargetSelectionCancelled;
        this.onEditStandingOrder = onEditStandingOrder;
        this.onDeleteStandingOrder = onDeleteStandingOrder;
        this.onBattleAcknowledged = onBattleAcknowledged;
        this.acknowledgements = acknowledgements;
        restoreFilters();
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
        compactNavigation.setLabel(I18n.t(UiTexts.MAP_VIEW));
        compactNavigation.setItems(Section.values());
        compactNavigation.setItemLabelGenerator(section -> tabFor(section).getElement().getText());
        compactNavigation.setAllowCustomValue(false);
        compactNavigation.setWidthFull();
        compactNavigation.addClassName("map-sidebar-select");
        compactNavigation.addValueChangeListener(event -> {
            if (event.getValue() != null) { select(event.getValue()); }
        });
        navigation.addClassName("map-sidebar-navigation");
        navigation.setWidthFull();
        navigation.add(tabs, compactNavigation);
        add(navigation, contactsPage, detailsPage, fleetsPage, ordersPage, relocationsPage, logisticsPage, reportPage);
        showSection(Section.CONTACTS);
        fleetsPage.add(header(UiTexts.MAP_OWN_FLEETS), routeFilter(), fleetCards);
        ordersPage.add(header(UiTexts.MAP_PLANNED_ORDERS), routeFilter(), orderCards);
        relocationsPage.add(header(UiTexts.MAP_STANDING_ORDERS_HEADER), routeFilter(), relocationCards);
    }

    void update(GameId gameId, PlayerViewState view, @Nullable VisibleSystem system, @Nullable Integer fleetId,
                @Nullable Integer standingOrderId,
                boolean spectator, boolean targetSelectionActive) {
        if (!gameId.equals(this.gameId)) {
            openedFleetDetails.clear();
            expandedFleetRoutes.clear();
            fleetArrivalsOpened = false;
            lastTravel = null;
        }
        this.gameId = gameId;
        currentView = view;
        selectedSystem = system == null ? null : view.systems().stream()
                .filter(candidate -> candidate.id() == system.id()).findFirst().orElse(null);
        selectedFleetId = fleetId;
        selectedStandingOrderId = standingOrderId;
        readOnly = view.gameOver() || spectator;
        productionInputs.reconcile(gameId.value(), view.turn(), view.systems().stream()
                .filter(candidate -> !spectator && candidate.fullyVisible() && candidate.industry() != null
                        && candidate.garrisonReserve() != null && !isBattlePending(candidate.id()))
                .collect(java.util.stream.Collectors.toMap(VisibleSystem::id, candidate -> Objects.requireNonNull(candidate.industry()))));
        TurnReport report = view.report();
        battlePresentationEnabled = BattlePresentationPreference.enabled(view.battlePresentationEnabled());
        Object travel = java.util.Arrays.asList(view.turn(), view.ownFleets(), view.plannedOrders(),
                view.standingOrders(), view.waitingFleetIds(), readOnly, fleetId, standingOrderId);
        if (!travel.equals(lastTravel)) { renderTravelCards(view); lastTravel = travel; }
        renderContacts(view);
        Integer detailOwner = selectedSystem == null ? null : selectedSystem.ownerId();
        boolean industryLocked = detailOwner != null && view.roundStatus().hasSubmitted(detailOwner);
        Object details = java.util.Arrays.asList(gameId, selectedSystem, fleetId, view.ownFleets(), view.plannedOrders(),
                view.empire(), automatedSystems, protectedSystems,
                view.waitingFleetIds(), view.standingOrders(), view.turn(), industryLocked, view.roundStatus(), readOnly, targetSelectionActive,
                selectedSystem != null && isBattlePending(selectedSystem.id()));
        if (!details.equals(lastDetails)) { renderDetails(view, targetSelectionActive); lastDetails = details; }
        Object logistics = List.of(view.systems(), view.standingOrders(), readOnly, automatedSystems, protectedSystems);
        if (!logistics.equals(lastLogistics)) { renderLogistics(view); lastLogistics = logistics; }
        Object reportState = java.util.Arrays.asList(report, battlePresentationEnabled,
                report == null ? List.of() : pendingBattleIndices(report.events()));
        if (!reportState.equals(lastReport)) { renderReport(view); lastReport = reportState; }
    }

    void setViewsVisible(boolean visible) {
        navigation.setVisible(visible);
        if (!visible) {
            pages.forEach(page -> page.setVisible(false));
        } else {
            showSection(sectionOf(tabs.getSelectedTab()));
        }
    }

    void setReportAvailable(boolean available) {
        if (reportAvailable == available) { return; }
        reportAvailable = available;
        reportTab.setVisible(available);
        if (!available && Objects.equals(tabs.getSelectedTab(), reportTab)) {
            tabs.setSelectedTab(sectionTabs.getFirst());
        }
        Section selected = sectionOf(tabs.getSelectedTab());
        compactNavigation.setItems(java.util.Arrays.stream(Section.values())
                .filter(section -> available || section != Section.REPORT).toList());
        compactNavigation.setValue(selected);
    }

    void setActions(com.vaadin.flow.component.Component actions) {
        addComponentAsFirst(actions);
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
        if (economyFiltered) { renderEconomyList(view); return; }
        logisticsPage.add(header(UiTexts.MAP_SIDEBAR_LOGISTICS),
                new Paragraph(I18n.t(UiTexts.MAP_LOGISTICS_INTRO)));
        Map<Integer, LogisticsSystemSummary> summaries = LogisticsSystemSummary.forSystems(view.systems(),
                view.standingOrders());
        addLogisticsFilters();
        boolean mining = view.systems().stream().anyMatch(system -> metalOf(system) != null);
        if (mining && !readOnly) {
            logisticsPage.add(new Button(I18n.t(UiTexts.MINING_TITLE), _ -> onMining.run()));
        }
        if (mining) { addYieldSort(); }
        java.util.Comparator<VisibleSystem> ranking = (left, right) -> Integer.compare(logisticsPriority(right, summaries),
                logisticsPriority(left, summaries));
        if (mining && sortByYield) { ranking = yieldOrder().thenComparing(ranking); }
        List<VisibleSystem> ownSystems = view.systems().stream()
                .filter(VisibleSystem::fullyVisible)
                .filter(system -> summaries.containsKey(system.id()))
                .filter(system -> matchesLogisticsFilters(logisticsSummary(summaries, system.id())))
                .sorted(ranking)
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

    private void renderEconomyList(PlayerViewState view) {
        if (view.systems().stream().anyMatch(system -> isBattlePending(system.id()))) { return; }
        int player = view.systems().stream().filter(system -> system.garrisonReserve() != null)
                .map(VisibleSystem::ownerId).filter(Objects::nonNull).findFirst().orElse(-1);
        var issues = EconomyIssues.forView(view, player, java.util.Set.of());
        var issueMap = issues.stream().collect(java.util.stream.Collectors.toMap(issue -> issue.system().id(), issue -> issue));
        var shown = view.systems().stream().filter(system ->
                        issueMap.containsKey(system.id()) && issueMap.get(system.id()).reasons().contains(economyFilter))
                .sorted(java.util.Comparator.comparing(VisibleSystem::name).thenComparingInt(VisibleSystem::id)).toList();
        String key = de.zettsystems.starfare.economy.ui.EconomyNoticeBar.key(Objects.requireNonNull(economyFilter));
        Button clear = new Button(I18n.t(de.zettsystems.starfare.economy.ui.ProductionKeys.CLEAR), _ -> {
            economyFiltered = false; var current = currentView; if (current != null) { renderLogistics(current); }
        });
        logisticsPage.add(new H2(I18n.t(key, shown.size())), clear);
        int section = 0;
        if (economyFilter == EconomyIssues.Kind.DEPOSIT) { section = 1; }
        if (economyFilter == EconomyIssues.Kind.LABOR) { section = -1; }
        for (var system : shown) { logisticsPage.add(economyIssueCard(view, system, Optional.ofNullable(issueMap.get(system.id())), section)); }
    }

    private Div economyIssueCard(PlayerViewState view, VisibleSystem system, Optional<EconomyIssues.Issue> issue, int section) {
        Div card = new Div(new Span(system.name())); card.addClassName("economy-issue-card");
        issue.ifPresent(candidate -> candidate.reasons().forEach(reason ->
                card.add(new Span(I18n.t(de.zettsystems.starfare.economy.ui.EconomyNoticeBar.key(reason), 1)))));
        var industry = system.industry();
        if (industry != null) {
            var outcome = de.zettsystems.starfare.economy.values.ProductionOutcome.of(
                    de.zettsystems.starfare.economy.values.ProductionPreset.saved(industry), industry);
            card.add(new Span(outcome.shipsShort()
                    ? I18n.t(de.zettsystems.starfare.economy.ui.ProductionKeys.OUTCOME_SHIPS_SHORT, outcome.shipPoints(), outcome.ships(), outcome.plannedShips())
                    : I18n.t(de.zettsystems.starfare.economy.ui.ProductionKeys.OUTCOME_SHIPS, outcome.shipPoints(), outcome.ships())));
        }
        card.add(new Button(I18n.t(section < 0 ? de.zettsystems.starfare.economy.ui.ProductionKeys.COLONY
                : de.zettsystems.starfare.economy.ui.ProductionKeys.CHANGE), _ -> onEconomySelected.accept(system.id(), section)));
        if (issue.map(candidate -> candidate.reasons().contains(EconomyIssues.Kind.DELIVERY)).orElse(false)) {
            view.standingOrders().stream().filter(order -> order.fromSystemId() == system.id())
                    .forEach(order -> card.add(logisticsRouteCard(order)));
            view.ownFleets().stream().filter(fleet -> fleet.toSystemId() == system.id())
                    .filter(fleet -> { var journey = fleet.journey(); return journey != null && journey.blocked(); })
                    .forEach(fleet -> card.add(new Button(I18n.t(UiTexts.MAP_OWN_FLEETS), _ -> onFleetRowSelected.accept(fleet.globalId()))));
        }
        return card;
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
            saveFilters();
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

    private void addYieldSort() {
        Checkbox sort = new Checkbox(I18n.t(de.zettsystems.starfare.economy.ui.ProductionKeys.LOGISTICS_SORT_YIELD), sortByYield);
        sort.setId("logistics-sort-yield");
        sort.addValueChangeListener(event -> {
            sortByYield = event.getValue();
            PlayerViewState view = currentView;
            if (view != null) { renderLogistics(view); }
        });
        logisticsPage.add(sort);
    }

    private static de.zettsystems.starfare.economy.values.@Nullable MetalView metalOf(VisibleSystem system) {
        var industry = system.industry();
        return industry == null ? null : industry.metal();
    }

    /** Best mining yield first; equal yield prefers the larger remaining deposit. */
    private static java.util.Comparator<VisibleSystem> yieldOrder() {
        return java.util.Comparator.comparingInt((VisibleSystem system) -> {
                    var metal = metalOf(system); return metal == null ? -1 : metal.yieldPerPoint(); }).reversed()
                .thenComparing(java.util.Comparator.comparingLong((VisibleSystem system) -> {
                    var metal = metalOf(system); return metal == null ? -1 : metal.remaining(); }).reversed());
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
        var metal = metalOf(system);
        if (metal != null) {
            Span yield = new Span(I18n.t(de.zettsystems.starfare.economy.ui.ProductionKeys.LOGISTICS_YIELD,
                    metal.yieldPerPoint(), metal.remaining()));
            yield.addClassName("logistics-system-yield");
            card.add(yield);
        }
        return card;
    }

    private Div logisticsRouteCard(StandingOrderView order) {
        Div card = new Div();
        card.addClassName("logistics-route-card");
        Button edit = new Button(I18n.t(UiTexts.MAP_ACTION_EDIT_STANDING), _ -> onEditStandingOrder.accept(order));
        edit.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);
        card.add(new Span(order.fromSystem() + " → " + order.toSystem()),
                new Span("◆ " + order.ships() + " / " + I18n.t(UiTexts.MAP_LOGISTICS_PER_TURN)), edit);
        card.getElement().addEventListener("click", _ -> onStandingOrderSelected.accept(order.id()))
                .setFilter("!event.target.closest('vaadin-button, vaadin-details')");
        KeyboardActions.enable(card);
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
                .map(Player::empireNameOrName).findFirst().orElse("?");
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

    private void renderDetails(PlayerViewState view, boolean targetSelectionActive) {
        detailsPage.removeAll();
        if (selectedSystem != null) {
            VisibleSystem system = selectedSystem;
            if (isBattlePending(system.id())) {
                reserveEditor = null;
                detailsPage.add(header(UiTexts.MAP_SIDEBAR_DETAILS),
                        new Paragraph(I18n.t(UiTexts.ROUND_EVENT_BATTLE_READY, system.name())));
                return;
            }
            if (!system.fullyVisible()) {
                reserveEditor = null;
                detailsPage.add(header(UiTexts.MAP_SIDEBAR_DETAILS),
                        detail(I18n.t(UiTexts.MAP_SIDEBAR_SYSTEM), system.name()), intelligenceHeader(),
                        detail(I18n.t(UiTexts.MAP_SIDEBAR_OWNER), ownerName(view, system.ownerId())),
                        detail(I18n.t(system.approximate() ? UiTexts.MAP_SIDEBAR_ESTIMATED_SHIPS
                                        : UiTexts.MAP_SIDEBAR_COUNTED_SHIPS), shipValue(system)),
                        detail(I18n.t(UiTexts.MAP_SIDEBAR_PRODUCTION), productionValue(system)),
                        detail(I18n.t(UiTexts.MAP_SIDEBAR_LAST_CONTACT), contactAge(system)));
                return;
            }
            renderVisibleSystem(view, system, targetSelectionActive);
            return;
        }
        detailsPage.add(header(UiTexts.MAP_SIDEBAR_DETAILS));
        reserveEditor = null;
        Fleet selectedFleet = view.ownFleets().stream()
                .filter(fleet -> selectedFleetId != null && fleet.globalId() == selectedFleetId)
                .findFirst().orElse(null);
        if (selectedFleet != null) {
            FleetView fleet = UiMapper.toFleetViews(view).stream()
                    .filter(row -> row.fleetId() == selectedFleet.globalId()).findFirst().orElseThrow();
            detailsPage.add(detail(I18n.t(UiTexts.MAP_SIDEBAR_FLEET), String.valueOf(fleet.localNo())),
                    detail(I18n.t(UiTexts.MAP_COLUMN_FROM), fleet.fromName()),
                    detail(I18n.t(UiTexts.MAP_COLUMN_TO), fleet.toName()),
                    detail(I18n.t(UiTexts.MAP_COLUMN_SHIPS), fleet.ships()),
                    detail(I18n.t(UiTexts.MAP_COLUMN_ARRIVAL_TURN), fleet.eta() < 0 ? I18n.t(UiTexts.NAV_UNKNOWN) : view.turn() + fleet.eta()));
            detailsPage.add(fleetActions(selectedFleet, view));
            Div journeyDetails = new Div(
                    detail(I18n.t(UiTexts.MAP_SIDEBAR_OWNER), ownerName(view, selectedFleet.ownerId())),
                    detail(I18n.t(UiTexts.MAP_SIDEBAR_FLEET_LAUNCHED), selectedFleet.launchTurn()),
                    detail(I18n.t(UiTexts.MAP_SIDEBAR_FLEET_TRAVELLING), Math.max(0, view.turn() - selectedFleet.launchTurn())),
                    detail(I18n.t(UiTexts.MAP_COLUMN_ETA), fleet.eta() < 0 ? I18n.t(UiTexts.NAV_UNKNOWN) : fleet.eta()));
            if (selectedFleet.journey() != null) {
                detailsPage.add(new Span(NavigationDisplay.status(selectedFleet, view)));
            }
            rememberRoute(NavigationDisplay.addItinerary(detailsPage, selectedFleet, view), selectedFleet.globalId());
            Details travelDetails = new Details(I18n.t(UiTexts.MAP_FLEET_DETAILS), journeyDetails);
            rememberTravelDetails(travelDetails, selectedFleet.globalId());
            detailsPage.add(travelDetails);
            return;
        }
        detailsPage.add(new Paragraph(I18n.t(UiTexts.MAP_SIDEBAR_DETAILS_EMPTY)));
    }

    private void renderVisibleSystem(PlayerViewState view, VisibleSystem system, boolean targetSelectionActive) {
        H2 heading = new H2(system.name());
        heading.addClassName("system-details-heading");
        var owner = detail(I18n.t(UiTexts.MAP_SIDEBAR_OWNER), ownerName(view, system.ownerId()));
        owner.addClassName("system-details-owner");
        detailsPage.add(heading, owner, systemHeaderMetrics(system, ProductionFlow.at(system.id(), view.standingOrders())));
        renderSystemActions(system, targetSelectionActive);
        renderSystemTransfers(view, system);
        var logistics = LogisticsSystemSummary.forSystems(view.systems(), view.standingOrders()).get(system.id());
        if (logistics != null && logistics.deliveryBottleneck()) {
            Span warning = new Span(I18n.t(UiTexts.SYSTEM_DELIVERY_BOTTLENECK));
            warning.addClassName("logistics-delivery-bottleneck");
            detailsPage.add(warning);
        }
        VerticalLayout operations = page();
        VerticalLayout background = page();
        Details backgroundDetails = new Details(I18n.t(UiTexts.UX_BACKGROUND), background);
        backgroundDetails.addClassName("system-background");
        backgroundDetails.setOpened(systemBackgroundOpened);
        backgroundDetails.addOpenedChangeListener(event -> systemBackgroundOpened = event.isOpened());
        renderIndustry(view, system, operations, background);
        renderSystemIllustration(system, background, () -> backgroundDetails.setOpened(true));
        if (system.routedProduction() != null) {
            operations.add(systemFlow(system, ProductionFlow.at(system.id(), view.standingOrders())));
        }
        ArrivalPreview.from(view).stream().filter(arrival -> arrival.systemId() == system.id())
                .forEach(arrival -> operations.add(new Paragraph(I18n.t(UiTexts.PLAN_ARRIVAL,
                        arrival.systemName(), arrival.turn(), arrival.ships()))));
        renderGarrisonReserve(system, operations);
        if (operations.getChildren().findAny().isPresent()) {
            var industry = system.industry();
            Details operationalDetails = new Details(I18n.t(industry != null && industry.colony() != null
                    ? UiTexts.SYSTEM_COLONY_LOGISTICS : UiTexts.SYSTEM_LOGISTICS_RESERVE), operations);
            operationalDetails.addClassName("system-operations");
            operationalDetails.setOpened(systemOperationsOpened);
            operationalDetails.addOpenedChangeListener(event -> systemOperationsOpened = event.isOpened());
            DetailedSystemPanel systemPanel = detailedSystem;
            if (systemPanel != null && industry != null && industry.colony() != null) {
                systemPanel.onColonyInspect(() -> {
                    operationalDetails.setOpened(true);
                    operations.getElement().executeJs("this.querySelector('.colony-details')?.scrollIntoView({block:'nearest'})");
                });
            }
            detailsPage.add(operationalDetails);
        }
        renderOwnershipHistory(view, system, background);
        detailsPage.add(backgroundDetails);
    }

    private void renderSystemTransfers(PlayerViewState view, VisibleSystem system) {
        view.standingOrders().stream()
                .filter(order -> order.fromSystemId() == system.id() || order.toSystemId() == system.id())
                .forEach(order -> {
                    Div row = relocationCard(order);
                    row.addClassName("system-transfer-row");
                    row.addComponentAsFirst(new Span(I18n.t(order.toSystemId() == system.id()
                            ? UiTexts.SYSTEM_TRANSFER_INCOMING : UiTexts.SYSTEM_TRANSFER_OUTGOING)));
                    detailsPage.add(row);
                });
    }

    private static Div systemHeaderMetrics(VisibleSystem system, ProductionFlow flow) {
        Div metrics = new Div();
        metrics.addClassNames("system-metrics", "system-header-metrics");
        Integer available = system.availableShips();
        if (available != null) {
            Div sendable = systemMetric("➤", I18n.t(UiTexts.SYSTEM_SENDABLE_SHIPS), available);
            sendable.addClassName("system-metric-sendable");
            metrics.add(sendable);
        }
        metrics.add(systemMetric("🛡", I18n.t(UiTexts.MAP_SIDEBAR_GARRISON), value(system.garrison())));
        Integer reserve = system.garrisonReserve();
        if (reserve != null) {
            metrics.add(systemMetric("⛨", I18n.t(UiTexts.MAP_SIDEBAR_GARRISON_RESERVE), reserve));
        }
        String rate = system.routedProduction() == null ? value(system.productionPerTurn())
                : I18n.t(UiTexts.SYSTEM_PRODUCTION_INCOMING, value(system.productionPerTurn()), flow.incoming());
        Div production = systemMetric("⚙", I18n.t(UiTexts.SYSTEM_PRODUCTION_RATE), rate);
        production.addClassName("system-metric-rate");
        metrics.add(production);
        return metrics;
    }

    private void renderSystemIllustration(VisibleSystem system, VerticalLayout background, Runnable revealBackground) {
        GameId currentGame = gameId;
        if (currentGame == null) { return; }
        SystemComposition.forVisible(currentGame, system).ifPresent(composition -> {
            boolean shipyard = system.ownerId() != null && system.productionPerTurn() != null;
            Object identity = List.of(currentGame, system.id(), shipyard, composition);
            DetailedSystemPanel panel = detailedSystem;
            if (panel == null || !identity.equals(detailedSystemKey)) {
                panel = new DetailedSystemPanel(composition, shipyard, currentGame, system.id());
                detailedSystem = panel;
                detailedSystemKey = identity;
            }
            panel.onInspect(revealBackground);
            detailsPage.add(panel);
            background.add(panel.background());
        });
    }

    private void renderSystemActions(VisibleSystem system, boolean targetSelectionActive) {
        if (readOnly || system.availableShips() == null) {
            return;
        }
        if (targetSelectionActive) {
            Button cancel = new Button(I18n.t(UiTexts.MAP_TARGET_SELECTION_CANCEL), _ -> onTargetSelectionCancelled.run());
            cancel.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);
            detailsPage.add(new Paragraph(I18n.t(UiTexts.MAP_TARGET_SELECTION_HINT)), cancel);
            return;
        }
        HorizontalLayout actions = new HorizontalLayout();
        actions.addClassName("system-details-actions");
        Button sendFleet = new Button(I18n.t(UiTexts.MAP_SEND_FLEET), _ -> onFleetTargetSelection.accept(system));
        sendFleet.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.PRIMARY);
        Button relocate = new Button(I18n.t(UiTexts.MAP_ADD_RELOCATION),
                _ -> onRelocationTargetSelection.accept(system));
        relocate.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);
        actions.add(sendFleet, relocate);
        detailsPage.add(actions);
    }

    private void renderIndustry(PlayerViewState view, VisibleSystem system, VerticalLayout operations,
                                VerticalLayout background) {
        var industry = system.industry();
        if (industry == null) { return; }
        Integer owner = system.ownerId();
        boolean editable = !readOnly && owner != null && system.availableShips() != null
                && !view.roundStatus().hasSubmitted(owner);
        var input = productionInputs.forSystem(system.id(), view.turn(), industry);
        var panel = new de.zettsystems.starfare.economy.ui.IndustryPanel(industry, editable,
                new de.zettsystems.starfare.economy.ui.IndustryPanel.Actions(
                ships -> onRecycling.accept(new de.zettsystems.starfare.economy.values.RecyclingOrder(system.id(), view.turn(), ships)),
                split -> onPreview.apply(new de.zettsystems.starfare.economy.values.ProductionAllocation(system.id(), view.turn(), split.expansion(), split.mining())),
                split -> onProductionAllocation.apply(new de.zettsystems.starfare.economy.values.ProductionAllocation(system.id(), view.turn(), split.expansion(),
                        industry.metal() == null ? -1 : split.mining(), input.saved()))), input,
                protect -> onMiningProtection.accept(system.id(), protect));
        industryPanel = panel;
        if (automatedSystems.contains(system.id())) {
            detailsPage.add(new Span(I18n.t(protectedSystems.contains(system.id()) ? UiTexts.MINING_SYSTEM_PROTECTED : UiTexts.MINING_SYSTEM_ACTIVE)));
        } else if (editable && industry.metal() != null) { detailsPage.add(new Span(I18n.t(UiTexts.MINING_SYSTEM_MANUAL))); }
        detailsPage.add(panel);
        if (industry.colony() != null) { operations.add(panel.colonyDetails()); }
        background.add(panel.background());
    }

    private void renderGarrisonReserve(VisibleSystem system, VerticalLayout operations) {
        Integer reserveValue = system.garrisonReserve();
        Integer garrison = system.garrison();
        if (readOnly || reserveValue == null || garrison == null) {
            reserveEditor = null;
            return;
        }
        IntegerField reserve = new IntegerField(I18n.t(UiTexts.MAP_SIDEBAR_GARRISON_RESERVE));
        reserve.setMin(0);
        reserve.setMax(garrison);
        ReserveEditor previous = reserveEditor;
        Integer draft = previous == null ? null : previous.input().getValue();
        boolean retainDraft = previous != null && previous.systemId() == system.id()
                && previous.savedReserve() == reserveValue && draft != null && draft >= 0 && draft <= garrison;
        reserve.setValue(retainDraft ? draft : reserveValue);
        reserveEditor = new ReserveEditor(system.id(), reserveValue, reserve);
        reserve.setStepButtonsVisible(true);
        Button save = new Button(I18n.t(UiTexts.MAP_SIDEBAR_GARRISON_RESERVE_SAVE), _ -> {
            Integer value = reserve.getValue();
            if (value != null) {
                onGarrisonReserveChanged.accept(system.id(), value);
            }
        });
        save.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);
        operations.add(reserve, save);
    }

    private void renderOwnershipHistory(PlayerViewState view, VisibleSystem system, VerticalLayout background) {
        if (!system.fullyVisible() || system.ownershipHistory().isEmpty()) {
            return;
        }
        List<SystemOwnership> periods = system.ownershipHistory();
        SystemOwnership current = periods.getLast();
        background.add(detail(I18n.t(UiTexts.MAP_SIDEBAR_OWNED_SINCE), current.sinceTurn()));
        if (periods.size() == 1) {
            background.add(detail(I18n.t(UiTexts.MAP_SIDEBAR_PREVIOUS_OWNER),
                    I18n.t(UiTexts.MAP_SIDEBAR_NONE)));
            return;
        }
        for (int index = periods.size() - 2; index >= 0; index--) {
            SystemOwnership previous = periods.get(index);
            background.add(detail(I18n.t(UiTexts.MAP_SIDEBAR_PREVIOUS_OWNER),
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
        boolean hasCoalition = events.stream().anyMatch(event -> event instanceof TurnEvent.CoalitionBattle);
        if (hasCoalition) {
            addReportFilter(filterOptions, events, EventCategory.COALITION, UiTexts.COALITION_FILTER);
        }
        addReportFilter(filterOptions, events, EventCategory.PRODUCTION, UiTexts.ROUND_FILTER_PRODUCTION);
        addReportFilter(filterOptions, events, EventCategory.REINFORCEMENT, UiTexts.ROUND_FILTER_REINFORCEMENT);
        addReportFilter(filterOptions, events, EventCategory.BATTLE_WON, UiTexts.ROUND_FILTER_BATTLE_WON);
        addReportFilter(filterOptions, events, EventCategory.BATTLE_LOST, UiTexts.ROUND_FILTER_BATTLE_LOST);
        addReportFilter(filterOptions, events, EventCategory.SYSTEM_LOST, UiTexts.ROUND_FILTER_SYSTEM_LOST);
        addReportFilter(filterOptions, events, EventCategory.DEFENSE_HELD, UiTexts.ROUND_FILTER_DEFENSE_HELD);
        ComboBox<Double> presentation = new ComboBox<>(I18n.t(UiTexts.BATTLE_REPLAY_SPEED));
        presentation.setItems(0.5, 1.0, 1.5, 2.0, 0.0);
        presentation.setItemLabelGenerator(value -> value == 0
                ? I18n.t(UiTexts.BATTLE_REPLAY_OFF) : value + "×");
        GameId presentationGame = gameId;
        presentation.setValue(presentationGame == null ? (view.battlePresentationEnabled() ? 1.0 : 0.0)
                : BattlePresentationPreference.speed(view.battlePresentationEnabled()));
        presentation.addValueChangeListener(event -> {
            if (event.getValue() == null) { return; }
            GameId current = gameId;
            if (current != null) {
                battlePresentationEnabled = event.getValue() > 0;
                BattlePresentationPreference.setSpeed(event.getValue());
                renderReport(view);
            }
        });
        filterOptions.add(presentation);
        Details filterDetails = new Details(new Span(I18n.t(UiTexts.ROUND_FILTER_SUMMARY,
                enabledEventCategories.stream().filter(category -> hasCoalition || category != EventCategory.COALITION).count(),
                EventCategory.values().length - (hasCoalition ? 0 : 1))), filterOptions);
        filterDetails.addClassName("report-filter-details");
        filters.add(filterDetails);
        List<Integer> pendingBattles = pendingBattleIndices(events);
        if (!pendingBattles.isEmpty()) {
            Button nextBattle = new Button(I18n.t(UiTexts.ROUND_NEXT_OPEN_BATTLE),
                    _ -> openNextPendingBattle(events, pendingBattles.getFirst()));
            nextBattle.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.SMALL);
            nextBattle.setTooltipText(I18n.t(UiTexts.ROUND_OPEN_BATTLES, pendingBattles.size()));
            filters.add(nextBattle);
            if (!battlePresentationEnabled && report != null && presentationGame != null) {
                Button all = new Button(I18n.t(UiTexts.PLAN_ACK_ALL), _ -> {
                    pendingBattles.forEach(index -> acknowledgements.acknowledge(presentationGame, report, index));
                    onBattleAcknowledged.run();
                });
                all.setDisableOnClick(true);
                filters.add(all);
            }
        }
        reportPage.add(filters);
        List<Integer> filtered = java.util.stream.IntStream.range(0, events.size()).boxed()
                .filter(eventIndex -> isEventEnabled(events.get(eventIndex)))
                .sorted(java.util.Comparator.comparingInt(this::eventPriority)).toList();
        if (events.stream().allMatch(TurnEvent.EconomySummary.class::isInstance)) {
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
            saveFilters();
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
            case TurnEvent.CoalitionBattle _, TurnEvent.BattleWon _, TurnEvent.BattleLost _, TurnEvent.SystemLost _, TurnEvent.DefenseHeld _ -> 1;
            case TurnEvent.Victory _, TurnEvent.Defeat _ -> 2;
            case TurnEvent.Reinforcement _ -> 3;
            case TurnEvent.Recycling _, TurnEvent.Production _, TurnEvent.EconomySummary _, TurnEvent.ResearchCompleted _ -> 4;
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
        if (event instanceof TurnEvent.EconomySummary) { return false; }
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
            case TurnEvent.CoalitionBattle _ -> EventCategory.COALITION;
            case TurnEvent.Recycling _, TurnEvent.Production _, TurnEvent.ResearchCompleted _ -> EventCategory.PRODUCTION;
            case TurnEvent.Reinforcement _ -> EventCategory.REINFORCEMENT;
            case TurnEvent.BattleWon _ -> EventCategory.BATTLE_WON;
            case TurnEvent.BattleLost _ -> EventCategory.BATTLE_LOST;
            case TurnEvent.SystemLost _ -> EventCategory.SYSTEM_LOST;
            case TurnEvent.DefenseHeld _ -> EventCategory.DEFENSE_HELD;
            case TurnEvent.Victory _, TurnEvent.Defeat _, TurnEvent.EconomySummary _ -> null;
        });
    }

    private Div eventCard(TurnEvent event, int eventIndex, int index) {
        Runnable acknowledge = battleAcknowledgement(event, eventIndex);
        Div card = new Div();
        card.addClassNames("event-card", eventCss(event, eventIndex));
        card.getStyle().set(CssProperties.ANIMATION_DELAY, (index * 0.1) + "s");
        card.add(new Span(eventIcon(event, eventIndex)), new Span(eventText(event, eventIndex)));
        BattleReplay replay = BattleReplay.from(event).orElse(null);
        if (event.battleSystemId().isPresent()) { KeyboardActions.enable(card); }
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
            case TurnEvent.CoalitionBattle _, TurnEvent.Recycling _, TurnEvent.Production _, TurnEvent.Reinforcement _, TurnEvent.Victory _, TurnEvent.Defeat _,
                 TurnEvent.EconomySummary _, TurnEvent.ResearchCompleted _ ->
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
            case TurnEvent.Recycling _ -> "♻";
            case TurnEvent.Production _, TurnEvent.EconomySummary _, TurnEvent.ResearchCompleted _ -> "⬆";
            case TurnEvent.Reinforcement _ -> "→";
            case TurnEvent.CoalitionBattle _, TurnEvent.BattleWon _ -> "⚔";
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
            case TurnEvent.Recycling _ -> "event-recycling";
            case TurnEvent.Production _, TurnEvent.EconomySummary _, TurnEvent.ResearchCompleted _ -> "event-production";
            case TurnEvent.Reinforcement _ -> "event-reinforcement";
            case TurnEvent.CoalitionBattle _ -> "event-coalition-battle";
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
            case TurnEvent.CoalitionBattle battle -> isEventPending(eventIndex)
                    ? I18n.t(UiTexts.ROUND_EVENT_BATTLE_READY, battle.systemName()) : CoalitionPresentation.report(battle);
            case TurnEvent.Recycling r -> I18n.t(UiTexts.RECYCLING_EVENT, r.systemName(), r.ships(), r.recoveredTenths() / 10.0);
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
            case TurnEvent.Victory victory -> victory.allianceId() == null
                    ? I18n.t(UiTexts.ROUND_EVENT_VICTORY, victory.systemPercent())
                    : I18n.t(UiTexts.ROUND_EVENT_ALLIANCE_VICTORY, victory.allianceId(),
                            String.join(", ", victory.winnerNames()), victory.systemPercent());
            case TurnEvent.Defeat defeat -> I18n.t(UiTexts.ROUND_EVENT_DEFEAT, defeat.winnerName());
            case TurnEvent.ResearchCompleted research -> I18n.t(UiTexts.RESEARCH_COMPLETED, research.level(), research.level() * 10);
            case TurnEvent.EconomySummary _ -> "";
        };
    }

    private void renderTravelCards(PlayerViewState view) {
        fleetCards.removeAll();
        orderCards.removeAll();
        relocationCards.removeAll();
        Div arrivals = new Div();
        ArrivalPreview.from(view).forEach(arrival -> arrivals.add(new Paragraph(I18n.t(UiTexts.PLAN_ARRIVAL,
                arrival.systemName(), arrival.turn(), arrival.ships()))));
        Details arrivalDetails = new Details(I18n.t(UiTexts.PLAN_ARRIVALS), arrivals);
        arrivalDetails.setOpened(fleetArrivalsOpened);
        arrivalDetails.addOpenedChangeListener(event -> fleetArrivalsOpened = event.isOpened());
        fleetCards.add(arrivalDetails);

        List<FleetView> fleets = UiMapper.toFleetViews(view).stream()
                .filter(fleet -> matchesRoute(fleet.fromName(), fleet.toName()))
                .sorted(java.util.Comparator.comparingInt((FleetView fleet) -> view.turn() + fleet.eta())
                        .thenComparingInt(FleetView::fleetId))
                .toList();
        addArrivalGroups(fleetCards, fleets, fleet -> fleet.eta() < 0 ? Integer.MAX_VALUE : view.turn() + fleet.eta(), this::fleetCard);

        List<PlannedOrder> orders = view.plannedOrders().stream()
                .filter(order -> matchesRoute(order.fromSystem(), order.toSystem()))
                .sorted(java.util.Comparator.comparingInt(this::orderArrival).thenComparingInt(PlannedOrder::index))
                .toList();
        addArrivalGroups(orderCards, orders, this::orderArrival, this::orderCard);

        List<StandingOrderView> relocations = view.standingOrders().stream()
                .filter(order -> matchesRoute(order.fromSystem(), order.toSystem()))
                .sorted(java.util.Comparator.comparing(StandingOrderView::fromSystem)
                        .thenComparing(StandingOrderView::toSystem).thenComparingInt(StandingOrderView::id))
                .toList();
        if (relocations.isEmpty()) {
            relocationCards.add(emptyTravelState());
        } else {
            relocations.forEach(order -> relocationCards.add(relocationCard(order)));
        }
    }

    private TextField routeFilter() {
        TextField filter = new TextField(I18n.t(UiTexts.MAP_TRAVEL_FILTER));
        filter.setPlaceholder(I18n.t(UiTexts.MAP_TRAVEL_FILTER_PLACEHOLDER));
        filter.setMaxLength(512);
        filter.setValue(routeFilter);
        filter.setClearButtonVisible(true);
        filter.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.EAGER);
        routeFilters.add(filter);
        filter.addValueChangeListener(event -> {
            if (!event.isFromClient()) { return; }
            routeFilter = event.getValue();
            routeFilters.stream().filter(other -> !other.equals(filter)).forEach(other -> other.setValue(routeFilter));
            saveFilters();
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
        Span group = new Span(arrivalTurn == Integer.MAX_VALUE ? I18n.t(UiTexts.NAV_UNKNOWN) : I18n.t(UiTexts.MAP_TRAVEL_ARRIVAL_GROUP, arrivalTurn));
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
        int arrivalTurn = currentView == null || fleet.eta() < 0 ? -1 : currentView.turn() + fleet.eta();
        String arrivalText = I18n.t(UiTexts.NAV_UNKNOWN);
        if (arrivalTurn >= 0) {
            arrivalText = I18n.t(UiTexts.MAP_COLUMN_ARRIVAL_TURN) + ": " + arrivalTurn
                    + " · " + I18n.t(UiTexts.MAP_COLUMN_ETA) + ": " + fleet.eta();
        }
        Span arrival = new Span(arrivalText);
        arrival.addClassName("fleet-travel-arrival");
        card.add(top, travelProgress(fleet), arrival);
        PlayerViewState view = currentView;
        if (view != null) {
            view.ownFleets().stream().filter(candidate -> candidate.globalId() == fleet.fleetId()).findFirst()
                    .ifPresent(candidate -> {
                        if (candidate.composition().levels().keySet().stream().anyMatch(level -> level > 0)) {
                            card.add(de.zettsystems.starfare.research.ui.ResearchDisplay.ships(candidate.composition()));
                        }
                        NavigationDisplay.addJourney(card, candidate, view)
                                .ifPresent(details -> rememberTravelDetails(details, candidate.globalId()));
                        card.getChildren().filter(de.zettsystems.starfare.navigation.ui.RouteStrip.class::isInstance)
                                .map(de.zettsystems.starfare.navigation.ui.RouteStrip.class::cast)
                                .forEach(strip -> rememberRoute(strip, candidate.globalId()));
                        card.add(fleetActions(candidate, view));
                    });
        }
        card.getElement().addEventListener("click", _ -> onFleetRowSelected.accept(fleet.fleetId()))
                .setFilter("!event.target.closest('vaadin-button, vaadin-details')");
        KeyboardActions.enable(card);
        if (Integer.valueOf(fleet.fleetId()).equals(selectedFleetId)) {
            card.addClassName("fleet-travel-card-selected");
        }
        return card;
    }

    private void rememberRoute(de.zettsystems.starfare.navigation.ui.RouteStrip strip, int fleetId) {
        strip.alliedOwners(alliedOwners);
        strip.rememberExpansion(expandedFleetRoutes.contains(fleetId), expanded -> {
            if (expanded) { expandedFleetRoutes.add(fleetId); }
            else { expandedFleetRoutes.remove(fleetId); }
        });
    }

    private void rememberTravelDetails(Details details, int fleetId) {
        details.setOpened(openedFleetDetails.contains(fleetId));
        details.addOpenedChangeListener(event -> {
            if (event.isOpened()) { openedFleetDetails.add(fleetId); }
            else { openedFleetDetails.remove(fleetId); }
        });
    }

    private Div fleetActions(Fleet fleet, PlayerViewState view) {
        Div actions = new Div();
        actions.addClassName("fleet-direct-actions");
        if (readOnly || fleet.evacuating() || view.roundStatus().hasSubmitted(fleet.ownerId())) { return actions; }
        var journey = fleet.journey();
        if (journey != null && journey.blocked()) {
            actions.add(new Button(I18n.t(de.zettsystems.starfare.navigation.ui.RouteKeys.SYSTEM),
                    _ -> onRouteStationSelected.accept(fleet.toSystemId())));
            boolean foreignStation = view.systems().stream().anyMatch(system -> system.id() == fleet.toSystemId()
                    && system.ownerId() != null && !Objects.equals(system.ownerId(), fleet.ownerId()));
            if (diplomacyAvailable && foreignStation) {
                actions.add(new Button(I18n.t(de.zettsystems.starfare.navigation.ui.RouteKeys.ALLIANCES), _ -> onDiplomacySelected.run()));
            }
        }
        boolean waiting = view.waitingFleetIds().contains(fleet.globalId());
        Button wait = new Button(I18n.t(waiting ? UiTexts.MAP_ACTION_WAITING : UiTexts.MAP_ACTION_WAIT),
                _ -> onFleetWait.accept(fleet.globalId()));
        wait.setEnabled(!waiting);
        wait.addThemeVariants(ButtonVariant.SMALL);
        actions.add(wait);
        int location = fleet.inFlight() ? fleet.fromSystemId() : fleet.toSystemId();
        boolean ownLocation = view.systems().stream().anyMatch(system -> system.id() == location
                && Objects.equals(system.ownerId(), fleet.ownerId()));
        if ((!fleet.inFlight() || fleet.launchTurn() == view.turn()) && ownLocation) {
            Button disband = new Button(I18n.t(UiTexts.MAP_ACTION_DISBAND),
                    _ -> onFleetDisband.accept(fleet.globalId()));
            disband.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);
            actions.add(disband);
        }
        return actions;
    }

    private Div orderCard(PlannedOrder order) {
        Div card = new Div();
        card.addClassNames("fleet-travel-card", "fleet-travel-card-planned");
        Div top = new Div();
        top.addClassName("fleet-travel-card-top");
        top.add(new Span("◌ " + I18n.t(order.type())), fleetNumber(value(order.ships())));
        Span route = new Span(order.fromSystem() + " → " + order.toSystem());
        Integer beneficiary = order.beneficiaryId();
        PlayerViewState view = currentView;
        if (beneficiary != null && view != null) {
            card.add(new Span(I18n.t(UiTexts.CONQUEST_ORDER, view.players().stream()
                    .filter(player -> player.id() == beneficiary).map(Player::empireNameOrName).findFirst().orElse("?"))));
        }
        route.addClassName("fleet-travel-route");
        Span arrival = new Span(I18n.t(UiTexts.MAP_COLUMN_ARRIVAL_TURN) + ": " + value(order.arrivalTurn()));
        arrival.addClassName("fleet-travel-arrival");
        card.add(top, route, arrival);
        if (!readOnly) {
            Button cancel = new Button(I18n.t(UiTexts.MAP_ACTION_CANCEL_ORDER), _ -> onCancelOrder.accept(order));
            cancel.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.SMALL, ButtonVariant.TERTIARY);
            card.add(cancel);
            if (order.isSend()) {
                card.add(new Button(I18n.t(UiTexts.PLAN_EDIT), _ -> onEditOrder.accept(order)));
            }
        }
        return card;
    }

    private Div travelProgress(FleetView fleet) {
        int elapsed = currentView == null ? 0 : Math.max(0, currentView.turn()
                - currentView.ownFleets().stream().filter(candidate -> candidate.globalId() == fleet.fleetId())
                .map(Fleet::launchTurn).findFirst().orElse(currentView.turn()));
        int total = elapsed + fleet.eta();
        int percent = total <= 0 || fleet.eta() < 0 ? 0 : Math.round(100f * elapsed / total);
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
                fleetNumber(order.ships() + " " + I18n.t(UiTexts.MAP_COLUMN_STANDING_SHIPS)));
        var route = order.route();
        if (order.rangeLimited()) {
            card.add(new Span(route == null ? I18n.t(UiTexts.NAV_PAUSED)
                    : I18n.t(UiTexts.NAV_TRANSFER, route.description(), route.rounds())));
        }
        if (!readOnly) {
            Button edit = new Button(I18n.t(UiTexts.MAP_ACTION_EDIT_STANDING), _ -> onEditStandingOrder.accept(order));
            edit.addThemeVariants(ButtonVariant.SMALL);
            Button delete = new Button(I18n.t(UiTexts.SYSTEM_CANCEL_TRANSFER), _ -> onDeleteStandingOrder.accept(order));
            delete.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);
            PlayerViewState view = currentView;
            boolean editable = view != null && view.systems().stream()
                    .filter(source -> source.id() == order.fromSystemId())
                    .anyMatch(source -> {
                        Integer owner = source.ownerId();
                        return source.availableShips() != null && owner != null
                                && !view.roundStatus().hasSubmitted(owner);
                    });
            edit.setEnabled(editable);
            delete.setEnabled(editable);
            HorizontalLayout actions = new HorizontalLayout(edit, delete);
            actions.addClassName("system-transfer-actions");
            card.add(actions);
        }
        card.getElement().addEventListener("click", _ -> onStandingOrderSelected.accept(order.id()))
                .setFilter("!event.target.closest('vaadin-button, vaadin-details')");
        KeyboardActions.enable(card);
        if (Integer.valueOf(order.id()).equals(selectedStandingOrderId)) {
            card.addClassName("fleet-travel-card-selected");
        }
        return card;
    }

    private void showSection(Section section) {
        if (compactNavigation.getValue() != section) { compactNavigation.setValue(section); }
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
        Span labelSpan = new Span(label);
        labelSpan.addClassName("system-metric-label");
        Div metric = new Div(iconSpan, new Div(valueSpan, labelSpan));
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
        Span step = new Span(new Span(icon + " " + value), new Span(label));
        step.addClassName("system-flow-step");
        step.getElement().setAttribute("title", label);
        step.getElement().setAttribute("aria-label", label + ": " + value);
        return step;
    }

    private static String value(@Nullable Integer value) { return value == null ? "—" : String.valueOf(value); }
}
