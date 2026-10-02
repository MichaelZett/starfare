package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.fleet.values.FleetDispatch;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.i18n.I18n;
import java.util.List;
import java.util.Objects;

/** Editors keep a turn token so a delayed confirmation cannot change the next turn. */
final class PlanningDialogs {
    private final GameService games;
    private final GameId id;
    private final int player;
    private final PlayerViewState view;
    private final Runnable changed;

    PlanningDialogs(GameService games, GameId id, int player, PlayerViewState view, Runnable changed) {
        this.games = games;
        this.id = id;
        this.player = player;
        this.view = view;
        this.changed = changed;
    }

    void edit(PlannedOrder order) {
        Dialog dialog = dialog(UiTexts.PLAN_EDIT);
        ComboBox<VisibleSystem> target = targets();
        target.setValue(view.systems().stream().filter(s -> Objects.equals(order.toSystemId(), s.id())).findFirst().orElse(null));
        IntegerField amount = amount();
        amount.setValue(order.ships());
        Paragraph preview = new Paragraph();
        Runnable update = () -> {
            VisibleSystem destination = target.getValue();
            Integer source = order.fromSystemId();
            preview.setText(destination == null || source == null ? "" : I18n.t(UiTexts.PLAN_ARRIVAL,
                    destination.name(), view.turn() + games.travelTurns(id, source, destination.id()), amount.getValue()));
        };
        target.addValueChangeListener(_ -> update.run());
        amount.addValueChangeListener(_ -> update.run());
        update.run();
        Button save = new Button(I18n.t(UiTexts.PLAN_SAVE), _ -> {
            VisibleSystem destination = target.getValue();
            Integer ships = amount.getValue();
            finish(dialog, destination != null && ships != null
                    && games.editOrder(id, player, view.turn(), order, destination.id(), ships));
        });
        dialog.add(target, amount, preview);
        Integer beneficiary = order.beneficiaryId();
        if (beneficiary != null) {
            dialog.add(new Paragraph(I18n.t(UiTexts.CONQUEST_ORDER, view.players().stream()
                    .filter(candidate -> candidate.id() == beneficiary).map(Player::label).findFirst().orElse("?"))));
        }
        footer(dialog, save);
    }

    void dispatch() {
        Dialog dialog = dialog(UiTexts.PLAN_DISPATCH);
        MultiSelectComboBox<VisibleSystem> sources = sources();
        ComboBox<VisibleSystem> target = targets();
        IntegerField amount = amount();
        Checkbox all = new Checkbox(I18n.t(UiTexts.PLAN_ALL_AVAILABLE), true);
        amount.setEnabled(false);
        Div preview = new Div();
        Button save = new Button(I18n.t(UiTexts.PLAN_DISPATCH), _ -> {
            VisibleSystem destination = target.getValue();
            List<FleetDispatch> dispatches = dispatches(sources, amount, all);
            finish(dialog, destination != null
                    && games.dispatchFleets(id, player, view.turn(), destination.id(), dispatches));
        });
        Runnable update = () -> updateDispatchPreview(preview, save, sources, target, amount, all);
        sources.addValueChangeListener(_ -> update.run());
        target.addValueChangeListener(_ -> update.run());
        amount.addValueChangeListener(_ -> update.run());
        all.addValueChangeListener(_ -> { amount.setEnabled(!all.getValue()); update.run(); });
        update.run();
        dialog.add(sources, target, all, amount, preview);
        if (view.systems().stream().anyMatch(system -> system.industry() != null)) {
            dialog.add(new Paragraph(I18n.t(UiTexts.CONQUEST_SELF)));
        }
        footer(dialog, save);
    }

    private void updateDispatchPreview(Div preview, Button save, MultiSelectComboBox<VisibleSystem> sources,
                                       ComboBox<VisibleSystem> target, IntegerField amount, Checkbox all) {
        preview.removeAll();
        VisibleSystem destination = target.getValue();
        var dispatches = dispatches(sources, amount, all);
        boolean valid = destination != null && !dispatches.isEmpty() && dispatches.stream().allMatch(dispatch ->
                dispatch.sourceId() != destination.id() && dispatch.ships() > 0
                        && sources.getValue().stream().filter(s -> s.id() == dispatch.sourceId())
                        .anyMatch(s -> hasEnoughShips(s, dispatch.ships())));
        save.setEnabled(valid);
        if (destination == null) { return; }
        var totals = new java.util.TreeMap<Integer, Long>();
        dispatches.forEach(dispatch -> totals.merge(view.turn() + games.travelTurns(id, dispatch.sourceId(), destination.id()),
                (long) dispatch.ships(), Long::sum));
        totals.forEach((turn, ships) -> preview.add(new Paragraph(I18n.t(UiTexts.PLAN_ARRIVAL, destination.name(), turn, ships))));
    }

    private static List<FleetDispatch> dispatches(MultiSelectComboBox<VisibleSystem> sources, IntegerField amount, Checkbox all) {
        Integer count = amount.getValue();
        return sources.getValue().stream().sorted(java.util.Comparator.comparingInt(VisibleSystem::id))
                .map(system -> new FleetDispatch(system.id(), all.getValue()
                        ? Objects.requireNonNullElse(system.availableShips(), 0) : Objects.requireNonNullElse(count, 0))).toList();
    }

    private static boolean hasEnoughShips(VisibleSystem system, int ships) {
        Integer available = system.availableShips();
        return available != null && available >= ships;
    }

    void reserves() {
        Dialog dialog = dialog(UiTexts.PLAN_RESERVES);
        MultiSelectComboBox<VisibleSystem> systems = sources();
        IntegerField amount = amount();
        amount.setMin(0);
        amount.setValue(0);
        Checkbox production = new Checkbox(I18n.t(UiTexts.PLAN_KEEP_PRODUCTION));
        production.addValueChangeListener(event -> amount.setEnabled(!event.getValue()));
        Button save = new Button(I18n.t(UiTexts.PLAN_SAVE), _ -> {
            Integer reserve = amount.getValue();
            finish(dialog, reserve != null && games.setReserves(id, player, view.turn(),
                    systems.getValue().stream().map(VisibleSystem::id).toList(), reserve, production.getValue()));
        });
        dialog.add(systems, production, amount, new Paragraph(I18n.t(UiTexts.PLAN_RESERVE_HINT)));
        footer(dialog, save);
    }

    private ComboBox<VisibleSystem> targets() {
        ComboBox<VisibleSystem> target = new ComboBox<>(I18n.t(UiTexts.PLAN_TARGET));
        target.setItems(view.systems());
        target.setItemLabelGenerator(VisibleSystem::name);
        return target;
    }

    private MultiSelectComboBox<VisibleSystem> sources() {
        MultiSelectComboBox<VisibleSystem> sources = new MultiSelectComboBox<>(I18n.t(UiTexts.PLAN_SOURCES));
        sources.setWidthFull();
        sources.setItems(view.systems().stream().filter(s -> Objects.equals(s.ownerId(), player)).toList());
        sources.setItemLabelGenerator(VisibleSystem::name);
        return sources;
    }

    private static IntegerField amount() {
        IntegerField amount = new IntegerField(I18n.t(UiTexts.MAP_COLUMN_SHIPS));
        amount.setMin(1);
        amount.setValue(1);
        amount.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.EAGER);
        return amount;
    }

    private static Dialog dialog(String key) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(I18n.t(key));
        dialog.setWidth("min(36rem, 90vw)");
        dialog.addClassName("planning-dialog");
        return dialog;
    }

    private static void footer(Dialog dialog, Button save) {
        dialog.getFooter().add(new Button(I18n.t(UiTexts.ACCOUNT_DELETE_CANCEL), _ -> dialog.close()), save);
        dialog.open();
    }

    private void finish(Dialog dialog, boolean accepted) {
        if (!accepted) { Notification.show(I18n.t(UiTexts.PLAN_REJECTED)); return; }
        dialog.close();
        changed.run();
    }
}
