package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.VisibleSystem;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.CssProperties;

import java.util.Optional;

final class SendFleetDialog {

    private SendFleetDialog() {
    }

    private record SendContext(GameService game, GameId gameId, int pid,
                               VisibleSystem from, VisibleSystem to, Runnable onClose) {
    }

    private record DialogControls(Input slider, IntegerField shipsInput,
                                  Button halfBtn, Button doubleBtn, Button allBtn,
                                  Checkbox standingCheckbox, Button sendBtn) {
    }

    private record DialogParams(int maxShips, boolean canSend, int sliderMax, int initial) {
        static DialogParams from(VisibleSystem source) {
            int garrison = source.garrison() != null ? source.garrison() : 0;
            int maxShips = Math.max(0, garrison);
            return new DialogParams(maxShips, maxShips >= 1, Math.max(1, maxShips), 1);
        }
    }

    private record HeaderLabels(Span route, Span duration, Span available) {
    }

    static void open(GameService game, GameId gameId, int pid,
                     VisibleSystem from, VisibleSystem to, Runnable onClose) {
        SendContext ctx = new SendContext(game, gameId, pid, from, to, onClose);
        DialogParams params = DialogParams.from(from);
        Dialog dialog = buildDialog();

        Input slider = buildSlider(params);
        IntegerField shipsInput = buildShipsInput(params);
        wireTwoWaySync(slider, shipsInput, params.sliderMax(), params.initial());

        Button halfBtn = buildQuickButton(UiTexts.MAP_SEND_QUICK_HALF,
                _ -> shipsInput.setValue(clamp(Math.max(1, params.maxShips() / 2), 1, params.sliderMax())));
        Button doubleBtn = buildQuickButton(UiTexts.MAP_SEND_QUICK_DOUBLE,
                _ -> shipsInput.setValue(clamp(currentOr(shipsInput, 1) * 2, 1, params.sliderMax())));
        Button allBtn = buildQuickButton(UiTexts.MAP_SEND_QUICK_ALL,
                _ -> shipsInput.setValue(params.sliderMax()));

        Checkbox standingCheckbox = new Checkbox(I18n.t(UiTexts.MAP_STANDING_ORDER_CHECKBOX));
        Button sendBtn = buildSendButton(ctx, shipsInput, standingCheckbox, dialog);
        Button cancelBtn = new Button(I18n.t(UiTexts.MAP_DIALOG_CANCEL), _ -> {
            dialog.close();
            onClose.run();
        });

        DialogControls controls = new DialogControls(slider, shipsInput, halfBtn, doubleBtn, allBtn,
                standingCheckbox, sendBtn);
        wireStandingToggle(controls, params.canSend());
        setInitialEnablement(controls, params.canSend());
        if (!params.canSend()) {
            standingCheckbox.setValue(true);
        }

        layoutDialog(dialog, buildHeaderLabels(ctx, params), controls, cancelBtn);
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

        var availableLabel = new Span(I18n.t(UiTexts.MAP_AVAILABLE, params.maxShips()));
        availableLabel.addClassName("send-fleet-available");
        return new HeaderLabels(routeLabel, durationLabel, availableLabel);
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

    private static void wireTwoWaySync(Input slider, IntegerField shipsInput, int sliderMax, int initial) {
        int[] current = {initial};
        slider.addValueChangeListener(e -> onSliderChange(e.getValue(), current, sliderMax, shipsInput));
        shipsInput.addValueChangeListener(e -> onShipsInputChange(e.getValue(), current, sliderMax, slider, shipsInput));
    }

    private static void onSliderChange(String raw, int[] current, int sliderMax, IntegerField shipsInput) {
        tryParseInt(raw).ifPresent(parsed -> {
            int v = clamp(parsed, 1, sliderMax);
            if (v != current[0]) {
                current[0] = v;
                shipsInput.setValue(v);
            }
        });
    }

    private static void onShipsInputChange(Integer v, int[] current, int sliderMax, Input slider, IntegerField shipsInput) {
        if (v == null) {
            return;
        }
        int clamped = clamp(v, 1, sliderMax);
        if (clamped != current[0]) {
            current[0] = clamped;
            slider.setValue(String.valueOf(clamped));
        }
        if (!v.equals(clamped)) {
            shipsInput.setValue(clamped);
        }
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
        if (Boolean.TRUE.equals(standingCheckbox.getValue())) {
            if (!ctx.game().addStandingOrder(ctx.gameId(), ctx.pid(), ctx.from().id(), ctx.to().id())) {
                Notification.show(I18n.t(UiTexts.MAP_ADD_STANDING_ORDER_FAILED));
            }
        } else {
            Integer ships = shipsInput.getValue();
            if (ships == null || ships < 1) {
                return;
            }
            if (!ctx.game().sendFleet(ctx.gameId(), ctx.pid(), ctx.from().id(), ctx.to().id(), ships)) {
                Notification.show(I18n.t(UiTexts.MAP_INVALID_COMMAND));
            }
        }
        dialog.close();
        ctx.onClose().run();
    }

    private static void wireStandingToggle(DialogControls c, boolean canSend) {
        c.standingCheckbox().addValueChangeListener(e -> {
            boolean standing = Boolean.TRUE.equals(e.getValue());
            boolean shipInputsEnabled = canSend && !standing;
            c.shipsInput().setEnabled(shipInputsEnabled);
            c.slider().setEnabled(shipInputsEnabled);
            c.halfBtn().setEnabled(shipInputsEnabled);
            c.doubleBtn().setEnabled(shipInputsEnabled);
            c.allBtn().setEnabled(shipInputsEnabled);
            c.sendBtn().setEnabled(standing || canSend);
        });
    }

    private static void setInitialEnablement(DialogControls c, boolean canSend) {
        c.shipsInput().setEnabled(canSend);
        c.halfBtn().setEnabled(canSend);
        c.doubleBtn().setEnabled(canSend);
        c.allBtn().setEnabled(canSend);
        c.sendBtn().setEnabled(canSend);
    }

    private static void layoutDialog(Dialog dialog, HeaderLabels labels, DialogControls c, Button cancelBtn) {
        var sliderRow = new HorizontalLayout(c.slider());
        sliderRow.setWidthFull();
        sliderRow.setPadding(false);
        sliderRow.setSpacing(false);

        var quickRow = new HorizontalLayout(c.halfBtn(), c.doubleBtn(), c.allBtn());
        quickRow.addClassName("send-fleet-quick-row");
        quickRow.setWidthFull();
        quickRow.setPadding(false);
        quickRow.setSpacing(true);

        var body = new VerticalLayout(labels.route(), labels.duration(), labels.available(),
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
