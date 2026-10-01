package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Div;
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
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.BooleanSupplier;

final class SendFleetDialog {

    private SendFleetDialog() {
    }

    private record SendContext(GameService game, GameId gameId, int pid,
                               VisibleSystem from, VisibleSystem to, int openedTurn, Runnable onClose) {
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

    private record HeaderLabels(Span route, Span duration, Span arrival, Span available, Span production) {
    }

    static void open(GameService game, GameId gameId, int pid,
                     VisibleSystem from, VisibleSystem to, Runnable onClose) {
        open(new SendContext(game, gameId, pid, from, to, game.viewFor(gameId, pid).turn(), onClose), false, null);
    }

    static void openRelocation(GameService game, GameId gameId, int pid,
                               VisibleSystem from, VisibleSystem to, Runnable onClose) {
        open(new SendContext(game, gameId, pid, from, to, game.viewFor(gameId, pid).turn(), onClose), true, null);
    }

    static void editRelocation(GameService game, GameId gameId, int pid, VisibleSystem from, VisibleSystem to,
                               int ships, Runnable onClose) {
        open(new SendContext(game, gameId, pid, from, to, game.viewFor(gameId, pid).turn(), onClose), true, ships);
    }

    private static void open(SendContext ctx, boolean relocation, @Nullable Integer initialShips) {
        DialogParams params = DialogParams.from(ctx.from(), initialShips, ctx.openedTurn());
        Dialog dialog = buildDialog();

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
        Button sendBtn = buildSendButton(ctx, shipsInput, standingCheckbox, dialog);
        Button cancelBtn = new Button(I18n.t(UiTexts.MAP_DIALOG_CANCEL), _ -> {
            dialog.close();
            ctx.onClose().run();
        });

        DialogControls controls = new DialogControls(slider, shipsInput, consequence, capacityFill,
                halfBtn, doubleBtn, allBtn, exceptProductionBtn,
                standingCheckbox, sendBtn);
        standingCheckbox.setValue(relocation || !params.canSend());
        wireStandingToggle(controls, params, amountState);
        applyAmountMode(controls, params, amountState);

        layoutDialog(dialog, buildHeaderLabels(ctx, params), controls, capacityTrack, cancelBtn);
        dialog.open();
    }

    private static Dialog buildDialog() {
        var dialog = new Dialog();
        dialog.addClassName("send-fleet-dialog");
        dialog.setHeaderTitle(I18n.t(UiTexts.MAP_SEND_FLEET));
        dialog.setWidth("min(460px, 94vw)");
        return dialog;
    }

    private static HeaderLabels buildHeaderLabels(SendContext ctx, DialogParams params) {
        var routeLabel = new Span(ctx.from().name() + " → " + ctx.to().name());
        routeLabel.addClassName("send-fleet-route");

        int travelTurns = ctx.game().travelTurns(ctx.gameId(), ctx.from().id(), ctx.to().id());
        String durationKey = travelTurns == 1 ? UiTexts.MAP_DURATION_SINGULAR : UiTexts.MAP_DURATION_PLURAL;
        var durationLabel = new Span(I18n.t(durationKey, travelTurns));
        durationLabel.addClassName("send-fleet-duration");
        int arrivalTurn = params.openedTurn() + travelTurns;
        var arrivalLabel = new Span(I18n.t(UiTexts.MAP_COLUMN_ARRIVAL_TURN) + ": " + arrivalTurn);
        arrivalLabel.addClassName("send-fleet-arrival");

        var availableLabel = new Span(I18n.t(UiTexts.MAP_AVAILABLE, params.maxShips()));
        availableLabel.addClassName("send-fleet-available");
        ProductionFlow flow = ProductionFlow.at(ctx.from().id(),
                ctx.game().viewFor(ctx.gameId(), ctx.pid()).standingOrders());
        Span production = new Span(I18n.t(UiTexts.MAP_PRODUCTION_FLOW, productionOf(ctx.from()),
                flow.incoming(), flow.outgoing()));
        production.addClassName("send-fleet-production");
        return new HeaderLabels(routeLabel, durationLabel, arrivalLabel, availableLabel, production);
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
        int ships = state.sliderMax - production;
        if (ships > 0) {
            shipsInput.setValue(clamp(ships, 1, state.sliderMax));
        }
    }

    private static int currentOr(IntegerField shipsInput, int fallback) {
        Integer cur = shipsInput.getValue();
        return cur == null ? fallback : cur;
    }

    private static Button buildSendButton(SendContext ctx, IntegerField shipsInput,
                                          Checkbox standingCheckbox, Dialog dialog) {
        Button sendBtn = new Button(I18n.t(UiTexts.MAP_SEND_FLEET), _ ->
                onSendClicked(ctx, shipsInput, standingCheckbox, dialog));
        sendBtn.addThemeVariants(ButtonVariant.PRIMARY);
        return sendBtn;
    }

    private static void onSendClicked(SendContext ctx, IntegerField shipsInput,
                                      Checkbox standingCheckbox, Dialog dialog) {
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
            accepted = ctx.game().sendFleet(ctx.gameId(), ctx.pid(), ctx.from().id(), ctx.to().id(), ships);
            if (!accepted) {
                Notification.show(I18n.t(UiTexts.MAP_INVALID_COMMAND));
            }
        }
        if (!accepted) {
            return;
        }
        dialog.close();
        ctx.onClose().run();
    }

    private static void wireStandingToggle(DialogControls c, DialogParams params, AmountState state) {
        c.standingCheckbox().addValueChangeListener(_ -> applyAmountMode(c, params, state));
    }

    private static void applyAmountMode(DialogControls c, DialogParams params, AmountState state) {
        boolean standing = Boolean.TRUE.equals(c.standingCheckbox().getValue());
        ConsequenceControls consequence = state.consequenceControls;
        int max = standing ? consequence.routingHeadroom() : params.maxShips();
        state.sliderMax = Math.max(1, max);
        c.slider().getElement().setAttribute("max", String.valueOf(state.sliderMax));
        c.shipsInput().setMax(state.sliderMax);
        int amount = clamp(currentOr(c.shipsInput(), 1), 1, state.sliderMax);
        c.shipsInput().setValue(amount);
        c.slider().setValue(String.valueOf(amount));
        setAmountEnablement(c, max >= 1);
        c.exceptProductionBtn().setEnabled(max > productionOf(consequence.context().from()));
        c.sendBtn().setText(I18n.t(standing ? UiTexts.MAP_SAVE_STANDING_ORDER : UiTexts.MAP_SEND_FLEET));
        updateConsequence(c.consequence(), c.capacityFill(), consequence.context(), amount,
                standing, consequence.routingHeadroom());
    }

    private static void updateConsequence(Span consequence, Div capacityFill, SendContext context,
                                          int ships, boolean standing,
                                          int routingHeadroom) {
        int travelTurns = context.game().travelTurns(context.gameId(), context.from().id(), context.to().id());
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
                                    Div capacityTrack, Button cancelBtn) {
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
        var body = new VerticalLayout(labels.route(), labels.duration(), labels.arrival(), labels.available(),
                labels.production(), c.consequence(), capacityTrack,
                sliderRow, c.shipsInput(), quickRow, c.standingCheckbox());
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
