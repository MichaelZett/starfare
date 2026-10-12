package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.navigation.values.RoutePreview;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.ProductionFlow;
import de.zettsystems.starfare.game.values.VisibleSystem;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.CssProperties;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import org.jspecify.annotations.Nullable;

final class SendFleetDialog {

    private SendFleetDialog() {
    }

    private record SendContext(GameService game, GameId gameId, int pid,
                               VisibleSystem from, VisibleSystem to, int openedTurn, SendDialogOutcome outcome) {
    }

    private record DialogControls(Input slider, IntegerField shipsInput, Span consequence, Div capacityFill,
                                  Button halfBtn, Button doubleBtn, Button allBtn, Button exceptProductionBtn,
                                  Checkbox standingCheckbox, Button sendBtn) {
    }

    private record ConsequenceControls(Span consequence, Div capacityFill, SendContext context,
                                       BooleanSupplier standingSupplier, int routingHeadroom) {
    }

    private static final class AmountState {
        private int current;
        private int sliderMax;
        private int maximum;
        private final ConsequenceControls consequenceControls;

        private AmountState(int current, int sliderMax, ConsequenceControls consequenceControls) {
            this.current = current;
            this.sliderMax = sliderMax;
            this.consequenceControls = consequenceControls;
        }
    }

    private record DialogParams(int maxShips, boolean canSend, int sliderMax, int initial, int openedTurn) {
        static DialogParams from(VisibleSystem source, @Nullable Integer initialShips, int openedTurn) {
            Integer available = source.availableShips();
            int maxShips = Math.max(0, available != null ? available : 0);
            int initial = initialShips == null ? 1 : Math.max(1, initialShips);
            int sliderMax = Math.max(1, Math.max(maxShips, initial));
            return new DialogParams(maxShips, maxShips >= 1, sliderMax, initial, openedTurn);
        }
    }

    private record HeaderLabels(com.vaadin.flow.component.Component route, Span duration, Span available, Span production, com.vaadin.flow.component.details.Details details) {
    }

    static void open(GameService game, GameId gameId, int pid,
                     VisibleSystem from, VisibleSystem to, SendDialogOutcome outcome) {
        open(new SendContext(game, gameId, pid, from, to, game.viewFor(gameId, pid).turn(), outcome), false, null);
    }

    static void openRelocation(GameService game, GameId gameId, int pid,
                               VisibleSystem from, VisibleSystem to, SendDialogOutcome outcome) {
        open(new SendContext(game, gameId, pid, from, to, game.viewFor(gameId, pid).turn(), outcome), true, null);
    }

    static void editRelocation(GameService game, GameId gameId, int pid, VisibleSystem from, VisibleSystem to,
                               int ships, SendDialogOutcome outcome) {
        open(new SendContext(game, gameId, pid, from, to, game.viewFor(gameId, pid).turn(), outcome), true, ships);
    }

    private static void open(SendContext ctx, boolean relocation, @Nullable Integer initialShips) {
        DialogParams params = DialogParams.from(ctx.from(), initialShips, ctx.openedTurn());
        Dialog dialog = buildDialog();
        dialog.addOpenedChangeListener(event -> {
            if (!event.isOpened()) { ctx.outcome().closed(); }
        });

        Input slider = buildSlider(params);
        IntegerField shipsInput = buildShipsInput(params);
        Span consequence = new Span();
        consequence.addClassName("send-fleet-consequence");
        Div capacityTrack = new Div();
        capacityTrack.addClassName("send-fleet-capacity-track");
        Div capacityFill = new Div();
        capacityFill.addClassName("send-fleet-capacity-fill");
        capacityTrack.add(capacityFill);

        Checkbox standingCheckbox = new Checkbox(I18n.t(UiTexts.MAP_STANDING_ORDER_CHECKBOX));
        int routingHeadroom = ctx.game().routingHeadroom(ctx.gameId(), ctx.pid(), ctx.from().id(), ctx.to().id());
        ConsequenceControls consequenceControls = new ConsequenceControls(consequence, capacityFill, ctx,
                () -> Boolean.TRUE.equals(standingCheckbox.getValue()), routingHeadroom);
        AmountState amountState = new AmountState(params.initial(), params.sliderMax(), consequenceControls);
        wireTwoWaySync(slider, shipsInput, amountState);
        Button halfBtn = buildQuickButton(UiTexts.MAP_SEND_QUICK_HALF,
                _ -> shipsInput.setValue(Math.max(1, amountState.sliderMax / 2)));
        Button doubleBtn = buildQuickButton(UiTexts.MAP_SEND_QUICK_DOUBLE,
                _ -> shipsInput.setValue((int) Math.min(amountState.sliderMax, currentOr(shipsInput, 1) * 2L)));
        Button allBtn = buildQuickButton(UiTexts.MAP_SEND_QUICK_ALL,
                _ -> shipsInput.setValue(amountState.sliderMax));
        Button exceptProductionBtn = buildQuickButton(UiTexts.MAP_SEND_QUICK_EXCEPT_PRODUCTION,
                _ -> selectExceptProduction(shipsInput, amountState, productionOf(ctx.from())));
        ConquestSelection conquest = new ConquestSelection(ctx.game(), ctx.gameId(), ctx.pid());
        conquest.setVisible(conquest.availableFor(ctx.to().ownerId()) && !standingCheckbox.getValue());
        standingCheckbox.addValueChangeListener(event -> conquest.setVisible(conquest.availableFor(ctx.to().ownerId()) && !event.getValue()));
        Button sendBtn = buildSendButton(ctx, shipsInput, standingCheckbox, dialog, conquest);
        Button cancelBtn = new Button(I18n.t(UiTexts.MAP_DIALOG_CANCEL), _ -> dialog.close());

        DialogControls controls = new DialogControls(slider, shipsInput, consequence, capacityFill,
                halfBtn, doubleBtn, allBtn, exceptProductionBtn,
                standingCheckbox, sendBtn);
        standingCheckbox.setValue(relocation || !params.canSend());
        wireStandingToggle(controls, params, amountState);
        applyAmountMode(controls, params, amountState);

        layoutDialog(dialog, buildHeaderLabels(ctx, params), controls, capacityTrack, cancelBtn, conquest);
        highlightRoute(dialog, ctx);
        dialog.open();
    }

    private static void highlightRoute(Dialog dialog, SendContext ctx) {
        var route = ctx.game().routeFor(ctx.gameId(), ctx.pid(), ctx.from().id(), ctx.to().id());
        var view = ctx.game().viewFor(ctx.gameId(), ctx.pid());
        java.util.List<Integer> ids = route.map(RoutePreview::systems).orElse(java.util.List.of(ctx.from().id(), ctx.to().id()));
        String points = ids.stream().map(id -> view.systems().stream().filter(system -> system.id() == id).findFirst().orElseThrow())
                .map(system -> system.x() + "," + system.y()).collect(java.util.stream.Collectors.joining(" "));
        dialog.addOpenedChangeListener(event -> dialog.getElement().executeJs("""
                document.querySelector('#route-preview')?.remove();
                if (!$0) return;
                const map = document.querySelector('#map');
                if (!map) return;
                const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
                svg.id = 'route-preview';
                svg.style.cssText = 'position:absolute;inset:0;width:100%;height:100%;pointer-events:none;z-index:4';
                const path = document.createElementNS('http://www.w3.org/2000/svg', 'polyline');
                path.setAttribute('points', $1);
                path.setAttribute('fill', 'none');
                path.setAttribute('stroke', '#80c8e8');
                path.setAttribute('stroke-width', '4');
                path.setAttribute('stroke-dasharray', '10 6');
                svg.append(path); map.append(svg);
                """, event.isOpened() && (ctx.from().industry() == null || route.isPresent()), points));
    }

    private static Dialog buildDialog() {
        var dialog = new Dialog();
        dialog.addClassName("send-fleet-dialog");
        dialog.setHeaderTitle(I18n.t(UiTexts.MAP_SEND_FLEET));
        dialog.setWidth("min(460px, 94vw)");
        return dialog;
    }

    private static HeaderLabels buildHeaderLabels(SendContext ctx, DialogParams params) {
        var view = ctx.game().viewFor(ctx.gameId(), ctx.pid());
        var route = ctx.game().routeFor(ctx.gameId(), ctx.pid(), ctx.from().id(), ctx.to().id());
        RoutePreview path = route.orElseGet(() -> new RoutePreview(java.util.List.of(ctx.from().id(), ctx.to().id()),
                java.util.List.of(ctx.from().name(), ctx.to().name()), 0, 0));
        var routeLabel = new de.zettsystems.starfare.navigation.ui.RouteStrip(path, view, ctx.pid(), route.isPresent());
        routeLabel.addClassName("send-fleet-route");

        int travelTurns = travelRounds(ctx);
        var durationLabel = new Span(travelTurns < 0 ? I18n.t(UiTexts.UX_ROUTE_BLOCKED)
                : I18n.t(de.zettsystems.starfare.navigation.ui.RouteKeys.ARRIVAL,
                        params.openedTurn() + travelTurns, travelTurns, Math.max(0, path.systems().size() - 2)));
        durationLabel.addClassName("send-fleet-duration");

        var availableLabel = new Span(I18n.t(UiTexts.MAP_AVAILABLE, params.maxShips()));
        availableLabel.addClassName("send-fleet-available");
        ProductionFlow flow = ProductionFlow.at(ctx.from().id(),
                view.standingOrders());
        Span production = new Span(I18n.t(UiTexts.SYSTEM_PRODUCTION_RATE) + ": "
                + I18n.t(UiTexts.SYSTEM_PRODUCTION_INCOMING, productionOf(ctx.from()), flow.incoming()));
        production.addClassName("send-fleet-production");
        Div itinerary = new Div(new Span(I18n.t(UiTexts.MAP_PRODUCTION_FLOW, productionOf(ctx.from()),
                flow.incoming(), flow.outgoing())));
        route.ifPresent(preview -> {
            for (int index = 0; index < preview.legRounds().size(); index++) {
                itinerary.add(new Div(new Span(I18n.t(de.zettsystems.starfare.navigation.ui.RouteKeys.LEG,
                        preview.names().get(index), preview.names().get(index + 1), preview.legRounds().get(index)))));
                if (index + 1 < preview.legRounds().size()) {
                    itinerary.add(new Div(new Span(I18n.t(de.zettsystems.starfare.navigation.ui.RouteKeys.STAY,
                            preview.names().get(index + 1)))));
                }
            }
        });
        return new HeaderLabels(routeLabel, durationLabel, availableLabel, production,
                new com.vaadin.flow.component.details.Details(I18n.t(UiTexts.UX_TRAVEL_DETAILS), itinerary));
    }

    private static Input buildSlider(DialogParams params) {
        var slider = new Input();
        slider.setType("range");
        slider.getElement().setAttribute("min", "1");
        slider.getElement().setAttribute("max", String.valueOf(params.sliderMax()));
        slider.getElement().setAttribute("step", "1");
        slider.setValue(String.valueOf(params.initial()));
        slider.addClassName("send-fleet-slider");
        slider.getStyle().set(CssProperties.WIDTH, "100%");
        return slider;
    }

    private static IntegerField buildShipsInput(DialogParams params) {
        var shipsInput = new IntegerField(I18n.t(UiTexts.MAP_SHIPS_LABEL));
        shipsInput.setMin(1);
        shipsInput.setMax(params.sliderMax());
        shipsInput.setValue(params.initial());
        shipsInput.setStepButtonsVisible(true);
        shipsInput.addClassName("send-fleet-amount");
        return shipsInput;
    }

    private static void wireTwoWaySync(Input slider, IntegerField shipsInput, AmountState amountState) {
        slider.addValueChangeListener(e -> onSliderChange(e.getValue(), shipsInput, amountState));
        shipsInput.addValueChangeListener(e -> onShipsInputChange(e.getValue(), slider, shipsInput, amountState));
    }

    private static void onSliderChange(String raw, IntegerField shipsInput, AmountState state) {
        tryParseInt(raw).ifPresent(parsed -> {
            int v = clamp(parsed, 1, state.sliderMax);
            if (v != state.current) {
                state.current = v;
                shipsInput.setValue(v);
            }
            ConsequenceControls controls = state.consequenceControls;
            updateConsequence(controls.consequence(), controls.capacityFill(), controls.context(), v,
                    controls.standingSupplier().getAsBoolean(), controls.routingHeadroom());
        });
    }

    private static void onShipsInputChange(Integer v, Input slider, IntegerField shipsInput, AmountState state) {
        if (v == null) {
            return;
        }
        int clamped = clamp(v, 1, state.sliderMax);
        if (clamped != state.current) {
            state.current = clamped;
            slider.setValue(String.valueOf(clamped));
        }
        if (!v.equals(clamped)) {
            shipsInput.setValue(clamped);
        }
        ConsequenceControls controls = state.consequenceControls;
        updateConsequence(controls.consequence(), controls.capacityFill(), controls.context(), clamped,
                controls.standingSupplier().getAsBoolean(), controls.routingHeadroom());
    }

    private static Optional<Integer> tryParseInt(String s) {
        try {
            return Optional.of(Integer.parseInt(s));
        } catch (NumberFormatException _) {
            return Optional.empty();
        }
    }

    private static Button buildQuickButton(String textKey, ComponentEventListener<ClickEvent<Button>> onClick) {
        Button b = new Button(I18n.t(textKey), onClick);
        b.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);
        return b;
    }

    private static int productionOf(VisibleSystem system) {
        Integer production = system.productionPerTurn();
        return production != null ? production : 0;
    }

    private static void selectExceptProduction(IntegerField shipsInput, AmountState state, int production) {
        int ships = productionFlow(state).sendExceptProduction(state.maximum, production);
        if (ships > 0) {
            shipsInput.setValue(clamp(ships, 1, state.sliderMax));
        }
    }

    private static ProductionFlow productionFlow(AmountState state) {
        SendContext ctx = state.consequenceControls.context();
        return ProductionFlow.at(ctx.from().id(), ctx.game().viewFor(ctx.gameId(), ctx.pid()).standingOrders());
    }

    private static int currentOr(IntegerField shipsInput, int fallback) {
        Integer cur = shipsInput.getValue();
        return cur == null ? fallback : cur;
    }

    private static Button buildSendButton(SendContext ctx, IntegerField shipsInput,
                                          Checkbox standingCheckbox, Dialog dialog, ConquestSelection conquest) {
        Button sendBtn = new Button(I18n.t(UiTexts.MAP_SEND_FLEET), _ ->
                onSendClicked(ctx, shipsInput, standingCheckbox, dialog, conquest));
        sendBtn.addThemeVariants(ButtonVariant.PRIMARY);
        return sendBtn;
    }

    private static void onSendClicked(SendContext ctx, IntegerField shipsInput,
                                      Checkbox standingCheckbox, Dialog dialog, ConquestSelection conquest) {
        if (ctx.game().viewFor(ctx.gameId(), ctx.pid()).turn() != ctx.openedTurn()) {
            Notification.show(I18n.t(UiTexts.MAP_SEND_ROUND_CHANGED));
            return;
        }
        boolean accepted;
        if (Boolean.TRUE.equals(standingCheckbox.getValue())) {
            int routed = shipsInput.getValue() == null ? 0 : shipsInput.getValue();
            accepted = ctx.game().addStandingOrder(ctx.gameId(), ctx.pid(), ctx.from().id(), ctx.to().id(), routed);
            if (!accepted) {
                Notification.show(I18n.t(UiTexts.MAP_ADD_STANDING_ORDER_FAILED));
            }
        } else {
            Integer ships = shipsInput.getValue();
            if (ships == null || ships < 1) {
                return;
            }
            accepted = ctx.from().industry() == null
                    ? ctx.game().sendFleet(ctx.gameId(), ctx.pid(), ctx.from().id(), ctx.to().id(), ships)
                    : ctx.game().sendFleet(ctx.gameId(), ctx.pid(), ctx.openedTurn(), ctx.from().id(), ctx.to().id(), ships, conquest.beneficiary());
            if (!accepted) {
                Notification.show(I18n.t(UiTexts.MAP_INVALID_COMMAND));
            }
        }
        if (!accepted) {
            return;
        }
        ctx.outcome().accepted();
        dialog.close();
        Notification.show(I18n.t(Boolean.TRUE.equals(standingCheckbox.getValue())
                ? UiTexts.MAP_RELOCATION_SAVED : UiTexts.MAP_FLEET_QUEUED));
    }

    private static void wireStandingToggle(DialogControls c, DialogParams params, AmountState state) {
        c.standingCheckbox().addValueChangeListener(_ -> applyAmountMode(c, params, state));
    }

    private static void applyAmountMode(DialogControls c, DialogParams params, AmountState state) {
        boolean standing = Boolean.TRUE.equals(c.standingCheckbox().getValue());
        ConsequenceControls consequence = state.consequenceControls;
        int max = standing ? consequence.routingHeadroom() : params.maxShips();
        state.maximum = max;
        state.sliderMax = Math.max(1, max);
        c.slider().getElement().setAttribute("max", String.valueOf(state.sliderMax));
        c.shipsInput().setMax(state.sliderMax);
        c.shipsInput().setLabel(I18n.t(standing ? UiTexts.MAP_COLUMN_STANDING_SHIPS : UiTexts.MAP_SHIPS_LABEL));
        int amount = clamp(currentOr(c.shipsInput(), 1), 1, state.sliderMax);
        c.shipsInput().setValue(amount);
        c.slider().setValue(String.valueOf(amount));
        setAmountEnablement(c, max >= 1 && travelRounds(consequence.context()) >= 0);
        ProductionFlow flow = productionFlow(state);
        int production = productionOf(consequence.context().from());
        c.exceptProductionBtn().setEnabled(travelRounds(consequence.context()) >= 0
                && flow.sendExceptProduction(max, production) > 0);
        c.exceptProductionBtn().getElement().setProperty(de.zettsystems.starfare.style.HtmlAttributes.TITLE,
                I18n.t(UiTexts.MAP_SEND_EXCEPT_FLOW, production, flow.incoming(), flow.outgoing(), flow.retainedShips(production)));
        c.exceptProductionBtn().setAriaLabel(I18n.t(UiTexts.MAP_SEND_QUICK_EXCEPT_PRODUCTION) + ". "
                + I18n.t(UiTexts.MAP_SEND_EXCEPT_FLOW, production, flow.incoming(), flow.outgoing(), flow.retainedShips(production)));
        c.sendBtn().setText(I18n.t(standing ? UiTexts.MAP_SAVE_STANDING_ORDER : UiTexts.MAP_SEND_FLEET));
        updateConsequence(c.consequence(), c.capacityFill(), consequence.context(), amount,
                standing, consequence.routingHeadroom());
    }

    private static void updateConsequence(Span consequence, Div capacityFill, SendContext context,
                                          int ships, boolean standing,
                                          int routingHeadroom) {
        int travelTurns = travelRounds(context);
        if (travelTurns < 0) { consequence.setText(I18n.t(UiTexts.NAV_UNREACHABLE)); return; }
        if (standing) {
            consequence.setText(I18n.t(UiTexts.MAP_SEND_RELOCATION_PREVIEW, ships, travelTurns,
                    Math.max(0, routingHeadroom - ships)));
            updateCapacityFill(capacityFill, ships, routingHeadroom);
            return;
        }
        int remaining = Math.max(0, availableShips(context.from()) - ships);
        consequence.setText(I18n.t(UiTexts.MAP_SEND_FLEET_PREVIEW, ships, travelTurns, remaining,
                reserveOf(context.from())));
        updateCapacityFill(capacityFill, ships, availableShips(context.from()));
    }

    private static int travelRounds(SendContext ctx) {
        if (ctx.from().industry() == null) { return ctx.game().travelTurns(ctx.gameId(), ctx.from().id(), ctx.to().id()); }
        return ctx.game().routeFor(ctx.gameId(), ctx.pid(), ctx.from().id(), ctx.to().id())
                .map(RoutePreview::rounds).orElse(-1);
    }

    private static void updateCapacityFill(Div fill, int amount, int capacity) {
        int percent = capacity <= 0 ? 0 : (int) Math.round(100.0 * amount / capacity);
        fill.getStyle().set(CssProperties.WIDTH, Math.clamp(percent, 0, 100) + "%");
        fill.getParent().ifPresent(parent -> parent.getElement().setAttribute("aria-valuenow",
                String.valueOf(Math.clamp(percent, 0, 100))));
    }

    private static int availableShips(VisibleSystem system) {
        Integer available = system.availableShips();
        return available == null ? 0 : available;
    }

    private static int reserveOf(VisibleSystem system) {
        Integer reserve = system.garrisonReserve();
        return reserve == null ? 0 : reserve;
    }

    private static void setAmountEnablement(DialogControls c, boolean canSend) {
        c.slider().setEnabled(canSend);
        c.shipsInput().setEnabled(canSend);
        c.halfBtn().setEnabled(canSend);
        c.doubleBtn().setEnabled(canSend);
        c.allBtn().setEnabled(canSend);
        c.exceptProductionBtn().setEnabled(canSend);
        c.sendBtn().setEnabled(canSend);
    }

    private static void layoutDialog(Dialog dialog, HeaderLabels labels, DialogControls c,
                                    Div capacityTrack, Button cancelBtn, ConquestSelection conquest) {
        var sliderRow = new HorizontalLayout(c.slider());
        sliderRow.setWidthFull();
        sliderRow.setPadding(false);
        sliderRow.setSpacing(false);

        var quickRow = new HorizontalLayout(c.halfBtn(), c.doubleBtn(), c.allBtn(), c.exceptProductionBtn());
        quickRow.addClassName("send-fleet-quick-row");
        quickRow.setWidthFull();
        quickRow.setPadding(false);
        quickRow.setSpacing(true);

        capacityTrack.getElement().setAttribute("role", "progressbar");
        capacityTrack.getElement().setAttribute("aria-valuemin", "0");
        capacityTrack.getElement().setAttribute("aria-valuemax", "100");
        var body = new VerticalLayout(labels.route(), labels.duration(), labels.available(), labels.production(),
                c.consequence(), capacityTrack,
                sliderRow, c.shipsInput(), quickRow, c.standingCheckbox(), conquest,
                labels.details());
        body.setPadding(false);
        body.setSpacing(false);
        body.addClassName("send-fleet-body");
        dialog.add(body);
        dialog.getFooter().add(cancelBtn, c.sendBtn());
    }

    private static int clamp(int value, int min, int max) {
        return Math.clamp(value, min, max);
    }
}
