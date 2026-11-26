package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.report.values.BattleReplay;
import de.zettsystems.starfare.report.values.TurnEvent;
import de.zettsystems.starfare.report.values.TurnReport;
import de.zettsystems.starfare.style.CssProperties;
import de.zettsystems.starfare.style.HtmlAttributes;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import org.jspecify.annotations.Nullable;
import de.zettsystems.starfare.economy.ui.IndustryPanel;

/** Switchable right-hand map sidebar that keeps the map context visible. */
final class FleetAndOrdersPanel extends VerticalLayout {

    private enum Section { DETAILS, FLEETS, RELOCATIONS, REPORT }

    private enum EventCategory { COALITION, PRODUCTION, REINFORCEMENT, BATTLE_WON, BATTLE_LOST, SYSTEM_LOST, DEFENSE_HELD }

    private final VerticalLayout detailsPage = page();
    private final VerticalLayout fleetsPage = page();
    private final VerticalLayout relocationsPage = page();
    private final VerticalLayout reportPage = page();
    private final List<VerticalLayout> pages = List.of(detailsPage, fleetsPage, relocationsPage, reportPage);
    private final Tabs tabs;
    private final Div navigation = new Div();
    private final List<Tab> sectionTabs;
    private final Tab reportTab;
    private boolean reportAvailable = true;

    private boolean readOnly;
    /** The open event of the round walkthrough; while there is one, the panel offers no orders. */
    private RoundWalkthrough.@Nullable Step walkthrough;
    private final de.zettsystems.starfare.economy.values.ProductionInputs productionInputs = new de.zettsystems.starfare.economy.values.ProductionInputs();
    private boolean battlePresentationEnabled;
    private @Nullable GameId gameId;
    private @Nullable VisibleSystem selectedSystem;
    private @Nullable Integer selectedFleetId;
    private @Nullable Integer selectedStandingOrderId;
    private final EnumSet<EventCategory> enabledEventCategories = EnumSet.allOf(EventCategory.class);
    private final IntConsumer onReportSystemSelected;
    private final IntConsumer onResolvedBattleSelected;
    private final IntConsumer onStandingOrderSelected;
    private final Consumer<VisibleSystem> onFleetTargetSelection;
    private final Consumer<VisibleSystem> onRelocationTargetSelection;
    private final Runnable onTargetSelectionCancelled;
    private final Consumer<StandingOrderView> onEditStandingOrder;
    private final Consumer<StandingOrderView> onDeleteStandingOrder;
    private final Runnable onBattleAcknowledged;
    private final BattleAcknowledgements acknowledgements;
    private @Nullable Integer selectedReportSystemId;
    private final IntConsumer onFleetRowSelected;
    private IntConsumer onSystemSelected = _ -> { };
    private Runnable onDiplomacySelected = () -> { };
    private boolean diplomacyAvailable;
    private java.util.Set<Integer> alliedOwners = java.util.Set.of();
    void alliedOwners(java.util.Set<Integer> owners) { alliedOwners = owners; }
    void diplomacyAvailable(boolean available) { diplomacyAvailable = available; }
    void onSystemSelected(IntConsumer action) { onSystemSelected = action; }
    void onDiplomacySelected(Runnable action) { onDiplomacySelected = action; }
    private final Consumer<PlannedOrder> onCancelOrder;
    private String routeFilter = "";
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
    private Consumer<PlannedDeparture> onCancelDeparture = _ -> { };
    void onCancelDeparture(Consumer<PlannedDeparture> action) { onCancelDeparture = action; }

    private static final String NO_EVENT_CATEGORY = "-";

    private void restoreFilters() {
        routeFilter = DisplayPreferences.get(de.zettsystems.starfare.game.values.DisplaySetting.ROUTE_FILTER, "");
        String events = DisplayPreferences.get(de.zettsystems.starfare.game.values.DisplaySetting.EVENT_FILTERS, "*");
        if (!events.equals("*")) {
            enabledEventCategories.removeIf(category -> !List.of(events.split(",")).contains(category.name()));
        }
    }

    // Each change writes only its own filter: the others may still show what this map read when it was
    // built, and writing them would overwrite a choice made meanwhile on another device.
    private void saveRouteFilter() {
        DisplayPreferences.choose(de.zettsystems.starfare.game.values.DisplaySetting.ROUTE_FILTER, routeFilter);
    }

    private void saveEventFilters() {
        String events = enabledEventCategories.stream().map(Enum::name).collect(java.util.stream.Collectors.joining(","));
        // Blank would mean the default (every category); no category is kept as a name no category has.
        DisplayPreferences.choose(de.zettsystems.starfare.game.values.DisplaySetting.EVENT_FILTERS,
                events.isEmpty() ? NO_EVENT_CATEGORY : events);
    }
    private final VerticalLayout fleetCards = page();
    private final VerticalLayout relocationCards = page();
    private final List<TextField> routeFilters = new java.util.ArrayList<>();
    private @Nullable Object lastDetails;
    private boolean fleetArrivalsOpened;
    private final java.util.Set<Integer> openedFleetDetails = new java.util.HashSet<>();
    private final java.util.Set<Integer> expandedFleetRoutes = new java.util.HashSet<>();
    private boolean systemOperationsOpened;
    private boolean systemBackgroundOpened;
    private @Nullable Object lastTravel;
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
                        Consumer<VisibleSystem> onFleetTargetSelection,
                        Consumer<VisibleSystem> onRelocationTargetSelection,
                        Runnable onTargetSelectionCancelled) {
        this.onReportSystemSelected = onReportSystemSelected;
        this.onFleetRowSelected = onFleetRowSelected;
        this.onCancelOrder = onCancelOrder;
        this.onResolvedBattleSelected = onResolvedBattleSelected;
        this.onStandingOrderSelected = onStandingOrderSelected;
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
                tab(UiTexts.MAP_SIDEBAR_DETAILS),
                tab(UiTexts.MAP_SIDEBAR_FLEETS),
                tab(UiTexts.MAP_SIDEBAR_RELOCATIONS),
                tab(UiTexts.MAP_SIDEBAR_REPORT));
        reportTab = sectionTabs.getLast();
        tabs = new Tabs();
        tabs.add(sectionTabs.toArray(Tab[]::new));
        tabs.setWidthFull();
        tabs.addClassName("map-sidebar-tabs");
        tabs.getElement().setAttribute(HtmlAttributes.ARIA_LABEL, I18n.t(UiTexts.MAP_VIEW));
        tabs.addSelectedChangeListener(event -> showSection(sectionOf(event.getSelectedTab())));
        navigation.addClassName("map-sidebar-navigation");
        navigation.setWidthFull();
        navigation.add(tabs);
        add(navigation, detailsPage, fleetsPage, relocationsPage, reportPage);
        showSection(Section.DETAILS);
        fleetsPage.add(header(UiTexts.MAP_OWN_FLEETS), routeFilter(), fleetCards);
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
        readOnly = view.gameOver() || spectator || walkthrough != null;
        productionInputs.reconcile(gameId.value(), view.turn(), view.systems().stream()
                .filter(candidate -> !spectator && candidate.fullyVisible() && candidate.industry() != null
                        && candidate.availableShips() != null && !isBattlePending(candidate.id()))
                .collect(java.util.stream.Collectors.toMap(VisibleSystem::id, candidate -> Objects.requireNonNull(candidate.industry()))));
        TurnReport report = view.report();
        battlePresentationEnabled = BattlePresentationPreference.enabled(view.battlePresentationEnabled());
        Object travel = java.util.Arrays.asList(view.turn(), view.ownFleets(), view.plannedOrders(),
                view.plannedDepartures(), view.standingOrders(), view.waitingFleetIds(), readOnly, fleetId, standingOrderId);
        if (!travel.equals(lastTravel)) { renderTravelCards(view); lastTravel = travel; }
        Integer detailOwner = selectedSystem == null ? null : selectedSystem.ownerId();
        boolean industryLocked = detailOwner != null && view.roundStatus().hasSubmitted(detailOwner);
        Object details = java.util.Arrays.asList(gameId, selectedSystem, fleetId, view.ownFleets(), view.plannedOrders(),
                view.empire(),
                view.waitingFleetIds(), view.standingOrders(), view.turn(), industryLocked, view.roundStatus(), readOnly,
                walkthrough != null, targetSelectionActive,
                selectedSystem != null && isBattlePending(selectedSystem.id()));
        if (!details.equals(lastDetails)) { renderDetails(view, targetSelectionActive); lastDetails = details; }
        Object reportState = java.util.Arrays.asList(report, battlePresentationEnabled, walkthrough,
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
    }

    void setActions(com.vaadin.flow.component.Component actions) {
        addComponentAsFirst(actions);
    }

    void showFleets() {
        select(Section.FLEETS);
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

    /** Set before {@link #update}: the open walkthrough event, or null once the map is free for orders. */
    void walkthrough(RoundWalkthrough.@Nullable Step step) {
        walkthrough = step;
    }

    /** A new walkthrough event: the report shows and marks it on the next {@link #update}. */
    void focusReportSystem(int systemId) {
        selectedReportSystemId = systemId;
        lastReport = null;
        showReport();
    }

    /** A fresh card of the open walkthrough event for the curtain in front of the board. */
    Optional<Div> walkthroughCard() {
        RoundWalkthrough.Step step = walkthrough;
        PlayerViewState view = currentView;
        TurnReport report = view == null ? null : view.report();
        if (step == null || report == null) {
            return Optional.empty();
        }
        boolean replay = battlePresentationEnabled && BattleReplay.from(step.event()).isPresent();
        List<TurnEvent> events = report.events();
        return Optional.of(RoundWalkthrough.card(step, replay, () -> openNextPendingBattle(events, step.eventIndex())));
    }

    private static int valueOrZero(@Nullable Integer value) {
        return value == null ? 0 : value;
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

    private void renderDetails(PlayerViewState view, boolean targetSelectionActive) {
        detailsPage.removeAll();
        if (selectedSystem != null) {
            VisibleSystem system = selectedSystem;
            if (isBattlePending(system.id())) {
                detailsPage.add(header(UiTexts.MAP_SIDEBAR_DETAILS),
                        new Paragraph(I18n.t(UiTexts.ROUND_EVENT_BATTLE_READY, system.name())));
                return;
            }
            if (!system.fullyVisible()) {
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
        VerticalLayout operations = page();
        VerticalLayout background = page();
        Details backgroundDetails = new Details(I18n.t(UiTexts.UX_BACKGROUND), background);
        backgroundDetails.addClassName("system-background");
        backgroundDetails.setOpened(systemBackgroundOpened);
        backgroundDetails.addOpenedChangeListener(event -> systemBackgroundOpened = event.isOpened());
        renderIndustry(view, system);
        if (system.routedProduction() != null) {
            operations.add(systemFlow(system, ProductionFlow.at(system.id(), view.standingOrders())));
        }
        ArrivalPreview.from(view).stream().filter(arrival -> arrival.systemId() == system.id())
                .forEach(arrival -> operations.add(new Paragraph(I18n.t(UiTexts.PLAN_ARRIVAL,
                        arrival.systemName(), arrival.turn(), arrival.ships()))));
        if (operations.getChildren().findAny().isPresent()) {
            Details operationalDetails = new Details(I18n.t(UiTexts.SYSTEM_LOGISTICS), operations);
            operationalDetails.addClassName("system-operations");
            operationalDetails.setOpened(systemOperationsOpened);
            operationalDetails.addOpenedChangeListener(event -> systemOperationsOpened = event.isOpened());
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
            Div sendable = systemMetric(VaadinIcon.PAPERPLANE, I18n.t(UiTexts.SYSTEM_SENDABLE_SHIPS), available);
            sendable.addClassName("system-metric-sendable");
            metrics.add(sendable);
        }
        metrics.add(systemMetric(VaadinIcon.SHIELD, I18n.t(UiTexts.MAP_SIDEBAR_GARRISON), value(system.garrison())));
        String rate = system.routedProduction() == null ? value(system.productionPerTurn())
                : I18n.t(UiTexts.SYSTEM_PRODUCTION_INCOMING, value(system.productionPerTurn()), flow.incoming());
        Div production = systemMetric(VaadinIcon.COGS, I18n.t(UiTexts.SYSTEM_PRODUCTION_RATE), rate);
        production.addClassName("system-metric-rate");
        metrics.add(production);
        return metrics;
    }

    private void renderSystemActions(VisibleSystem system, boolean targetSelectionActive) {
        if (walkthrough != null && system.availableShips() != null) {
            Paragraph locked = new Paragraph(I18n.t(UiTexts.ROUND_WALKTHROUGH_HINT));
            locked.addClassName("round-walkthrough-locked");
            detailsPage.add(locked);
        }
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

    private void renderIndustry(PlayerViewState view, VisibleSystem system) {
        var industry = system.industry();
        if (industry == null) { return; }
        Integer owner = system.ownerId();
        boolean editable = !readOnly && owner != null && system.availableShips() != null
                && !view.roundStatus().hasSubmitted(owner);
        var input = productionInputs.forSystem(system.id(), view.turn(), industry);
        var panel = new IndustryPanel(industry, editable, new IndustryPanel.Actions(
                split -> onPreview.apply(new de.zettsystems.starfare.economy.values.ProductionAllocation(system.id(), view.turn(), split.expansion())),
                split -> onProductionAllocation.apply(new de.zettsystems.starfare.economy.values.ProductionAllocation(system.id(), view.turn(), split.expansion(),
                        input.saved()))), input);
        detailsPage.add(panel);
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
            if (walkthrough == null) {
                Button nextBattle = new Button(I18n.t(UiTexts.ROUND_NEXT_OPEN_BATTLE),
                        _ -> openNextPendingBattle(events, pendingBattles.getFirst()));
                nextBattle.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.SMALL);
                nextBattle.setTooltipText(I18n.t(UiTexts.ROUND_OPEN_BATTLES, pendingBattles.size()));
                filters.add(nextBattle);
            }
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
            saveEventFilters();
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
            case TurnEvent.Victory _, TurnEvent.Defeat _, TurnEvent.FinalScore _ -> 2;
            case TurnEvent.Cosmic _, TurnEvent.JointDeparture _ -> 3;
            case TurnEvent.Reinforcement _ -> 3;
            case TurnEvent.Production _, TurnEvent.EconomySummary _ -> 4;
        };
    }

    private List<Integer> pendingBattleIndices(List<TurnEvent> events) {
        return java.util.stream.IntStream.range(0, events.size()).boxed().filter(this::isEventPending).toList();
    }

    private void openNextPendingBattle(List<TurnEvent> events, int eventIndex) {
        TurnEvent event = events.get(eventIndex);
        BattleReplay replay = BattleReplay.from(event).orElse(null);
        event.battleSystemId().ifPresent(onReportSystemSelected);
        Runnable acknowledge = battleAcknowledgement(eventIndex);
        if (battlePresentationEnabled && replay != null) {
            BattleReplayDialog.open(replay, RoundEventTexts.sides(event), battleProduction(event), acknowledge);
        } else {
            acknowledge.run();
        }
    }

    private @Nullable PlayerViewState currentView;

    private boolean isEventEnabled(TurnEvent event) {
        if (event instanceof TurnEvent.EconomySummary) { return false; }
        PlayerViewState view = currentView;
        TurnReport report = view == null ? null : view.report();
        // The result of the game follows the walkthrough of the final round, never before it.
        boolean battlesOpen = gameId != null && report != null && !acknowledgements.pending(gameId, report).isEmpty();
        if (((event instanceof TurnEvent.Victory || event instanceof TurnEvent.Defeat) && (walkthrough != null || battlesOpen))
                || (event instanceof TurnEvent.FinalScore && walkthrough != null)) {
            return false;
        }
        return categoryOf(event).map(enabledEventCategories::contains).orElse(true);
    }

    private static Optional<EventCategory> categoryOf(TurnEvent event) {
        return Optional.ofNullable(switch (event) {
            case TurnEvent.CoalitionBattle _ -> EventCategory.COALITION;
            case TurnEvent.Production _ -> EventCategory.PRODUCTION;
            case TurnEvent.Reinforcement _ -> EventCategory.REINFORCEMENT;
            case TurnEvent.BattleWon _ -> EventCategory.BATTLE_WON;
            case TurnEvent.BattleLost _ -> EventCategory.BATTLE_LOST;
            case TurnEvent.SystemLost _ -> EventCategory.SYSTEM_LOST;
            case TurnEvent.DefenseHeld _ -> EventCategory.DEFENSE_HELD;
            case TurnEvent.Victory _, TurnEvent.Defeat _, TurnEvent.EconomySummary _, TurnEvent.Cosmic _,
                 TurnEvent.FinalScore _, TurnEvent.JointDeparture _ -> null;
        });
    }

    private Div eventCard(TurnEvent event, int eventIndex, int index) {
        Runnable acknowledge = battleAcknowledgement(eventIndex);
        Div card = new Div();
        card.addClassNames("event-card", eventCss(event, eventIndex));
        card.getStyle().set(CssProperties.ANIMATION_DELAY, (index * 0.1) + "s");
        card.add(new Span(eventIcon(event, eventIndex)), new Span(RoundEventTexts.text(event, isEventPending(eventIndex))));
        BattleReplay replay = BattleReplay.from(event).orElse(null);
        java.util.OptionalInt site = RoundWalkthrough.siteOf(event);
        if (site.isPresent()) { KeyboardActions.enable(card); }
        RoundWalkthrough.Step step = walkthrough;
        if (step != null && step.eventIndex() == eventIndex) { card.addClassName("event-walkthrough-current"); }
        site.ifPresent(systemId -> {
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
                BattleReplayDialog.open(replay, RoundEventTexts.sides(event), battleProduction(event), acknowledge);
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
    private Runnable battleAcknowledgement(int eventIndex) {
        GameId current = gameId;
        TurnReport report = currentView == null ? null : currentView.report();
        // Capture the report shown by the card, not whichever round is current on dialog close.
        return () -> {
            if (current != null && report != null) {
                acknowledgements.acknowledge(current, report, eventIndex);
                onBattleAcknowledged.run();
            }
        };
    }

    /** The production of the embattled system as far as the player knows it; the replay shows it beside the numbers. */
    private @Nullable String battleProduction(TurnEvent event) {
        PlayerViewState view = currentView;
        if (view == null || event.battleSystemId().isEmpty()) {
            return null;
        }
        int systemId = event.battleSystemId().getAsInt();
        return view.systems().stream().filter(system -> system.id() == systemId && system.productionPerTurn() != null)
                .findFirst().map(FleetAndOrdersPanel::productionValue).orElse(null);
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
            case TurnEvent.Production _, TurnEvent.EconomySummary _ -> "⬆";
            case TurnEvent.Reinforcement _ -> "→";
            case TurnEvent.CoalitionBattle _, TurnEvent.BattleWon _ -> "⚔";
            case TurnEvent.BattleLost _ -> "✕";
            case TurnEvent.SystemLost _ -> "☠";
            case TurnEvent.DefenseHeld _ -> "🛡";
            case TurnEvent.Victory _ -> "🏆";
            case TurnEvent.Defeat _ -> "☠";
            case TurnEvent.Cosmic _ -> "☄";
            case TurnEvent.FinalScore _ -> "🏁";
            case TurnEvent.JointDeparture _ -> "⏱";
        };
    }

    private String eventCss(TurnEvent event, int eventIndex) {
        if (event.battleSystemId().isPresent() && isEventPending(eventIndex)) {
            return "event-battle-pending";
        }
        return switch (event) {
            case TurnEvent.Production _, TurnEvent.EconomySummary _ -> "event-production";
            case TurnEvent.Reinforcement _ -> "event-reinforcement";
            case TurnEvent.CoalitionBattle _ -> "event-coalition-battle";
            case TurnEvent.BattleWon _ -> "event-battle-won";
            case TurnEvent.BattleLost _ -> "event-battle-lost";
            case TurnEvent.SystemLost _ -> "event-system-lost";
            case TurnEvent.DefenseHeld _ -> "event-defense-held";
            case TurnEvent.Victory _ -> "event-victory";
            case TurnEvent.Defeat _ -> "event-system-lost";
            case TurnEvent.Cosmic _ -> "event-cosmic";
            case TurnEvent.FinalScore _ -> "event-victory";
            case TurnEvent.JointDeparture _ -> "event-joint-departure";
        };
    }

    private void renderTravelCards(PlayerViewState view) {
        fleetCards.removeAll();
        relocationCards.removeAll();
        Div arrivals = new Div();
        ArrivalPreview.from(view).forEach(arrival -> arrivals.add(new Paragraph(I18n.t(UiTexts.PLAN_ARRIVAL,
                arrival.systemName(), arrival.turn(), arrival.ships()))));
        Details arrivalDetails = new Details(I18n.t(UiTexts.PLAN_ARRIVALS), arrivals);
        arrivalDetails.setOpened(fleetArrivalsOpened);
        arrivalDetails.addOpenedChangeListener(event -> fleetArrivalsOpened = event.isOpened());
        fleetCards.add(arrivalDetails);

        // Fleets under way and orders planned this round in one list; at each arrival round the fleets come first.
        List<TravelEntry> travel = new java.util.ArrayList<>();
        UiMapper.toFleetViews(view).stream()
                .filter(fleet -> matchesRoute(fleet.fromName(), fleet.toName()))
                .forEach(fleet -> travel.add(new TravelEntry(fleet.eta() < 0 ? Integer.MAX_VALUE : view.turn() + fleet.eta(),
                        0, fleet.fleetId(), () -> fleetCard(fleet))));
        view.plannedOrders().stream()
                .filter(order -> matchesRoute(order.fromSystem(), order.toSystem()))
                .forEach(order -> travel.add(new TravelEntry(orderArrival(order), 1, order.index(), () -> orderCard(order))));
        List<PlannedDeparture> departures = view.plannedDepartures();
        for (int i = 0; i < departures.size(); i++) {
            PlannedDeparture departure = departures.get(i);
            if (matchesRoute(departure.fromSystem(), departure.toSystem())) {
                travel.add(new TravelEntry(departure.arrivalTurn(), 2, i, () -> departureCard(departure)));
            }
        }
        travel.sort(java.util.Comparator.comparingInt(TravelEntry::arrival).thenComparingInt(TravelEntry::kind)
                .thenComparingInt(TravelEntry::order));
        addArrivalGroups(fleetCards, travel, TravelEntry::arrival, entry -> entry.card().get());

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

    /** A fleet ({@code kind} 0), a planned order (1) or a planned later start (2) in the travel list. */
    private record TravelEntry(int arrival, int kind, int order, java.util.function.Supplier<Div> card) { }

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
            saveRouteFilter();
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
                        NavigationDisplay.addJourney(card, candidate, view);
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
                    _ -> onSystemSelected.accept(fleet.toSystemId())));
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

    /** A joint attack's later start: the ships wait in the source garrison until the start round. */
    private Div departureCard(PlannedDeparture departure) {
        Div card = new Div();
        card.addClassNames("fleet-travel-card", "fleet-travel-card-planned", "fleet-travel-card-departure");
        Div top = new Div();
        top.addClassName("fleet-travel-card-top");
        top.add(new Span("⏱ " + I18n.t(UiTexts.JOINT_DEPARTURE)), fleetNumber(departure.ships()));
        Span route = new Span(departure.fromSystem() + " → " + departure.toSystem());
        route.addClassName("fleet-travel-route");
        Span times = new Span(I18n.t(UiTexts.JOINT_DEPARTURE_TIMES, departure.departureTurn(), departure.arrivalTurn()));
        times.addClassName("fleet-travel-arrival");
        card.add(top, route, times);
        if (!readOnly) {
            Button cancel = new Button(I18n.t(UiTexts.MAP_ACTION_CANCEL_ORDER), _ -> onCancelDeparture.accept(departure));
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
        detailsPage.setVisible(section == Section.DETAILS);
        fleetsPage.setVisible(section == Section.FLEETS);
        relocationsPage.setVisible(section == Section.RELOCATIONS);
        reportPage.setVisible(section == Section.REPORT);
    }

    private void select(Section section) {
        tabs.setSelectedTab(tabFor(section));
        showSection(section);
    }

    private Tab tabFor(Section section) {
        return switch (section) {
            case DETAILS -> sectionTabs.getFirst();
            case FLEETS -> sectionTabs.get(1);
            case RELOCATIONS -> sectionTabs.get(2);
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

    private static Div systemMetric(VaadinIcon icon, String label, Object value) {
        Span iconSpan = new Span(icon.create());
        iconSpan.addClassName("system-metric-icon");
        Span valueSpan = new Span(String.valueOf(value));
        valueSpan.addClassName("system-metric-value");
        Span labelSpan = new Span(label);
        labelSpan.addClassName("system-metric-label");
        Div metric = new Div(iconSpan, new Div(valueSpan, labelSpan));
        metric.addClassName("system-metric");
        metric.setTitle(label);
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
