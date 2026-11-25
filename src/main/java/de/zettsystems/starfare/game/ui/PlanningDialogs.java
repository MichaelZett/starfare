package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.fleet.values.FleetDispatch;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.*;
import de.zettsystems.starfare.i18n.I18n;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

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
            Integer arrival = destination == null || source == null ? null : arrivalTurn(source, destination.id());
            preview.setText(destination == null || arrival == null ? "" : I18n.t(UiTexts.PLAN_ARRIVAL,
                    destination.name(), arrival, amount.getValue()));
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
                    .filter(candidate -> candidate.id() == beneficiary).map(Player::empireNameOrName).findFirst().orElse("?"))));
        }
        footer(dialog, save);
    }

    /**
     * Confirmation of a joint attack picked on the map: per source the ships (all available by default), its start
     * and the common arrival. Faster sources start later; the game launches them in their round.
     */
    void jointAttack(JointArrival joint, Runnable done) {
        VisibleSystem target = system(joint.targetId());
        Integer common = joint.commonArrival();
        if (target == null || common == null) { return; }
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(I18n.t(UiTexts.JOINT_DIALOG_TITLE, target.name()));
        dialog.setWidth("min(32rem, 90vw)");
        dialog.addClassNames("planning-dialog", "joint-attack-dialog");
        Paragraph summary = new Paragraph();
        summary.addClassName("joint-attack-summary");
        java.util.Map<Integer, IntegerField> amounts = new java.util.LinkedHashMap<>();
        Button confirm = new Button(I18n.t(UiTexts.JOINT_DIALOG_CONFIRM));
        Runnable update = () -> {
            List<FleetDispatch> dispatches = jointDispatches(amounts);
            confirm.setEnabled(dispatches.size() == amounts.size());
            summary.setText(I18n.t(UiTexts.JOINT_DIALOG_SUMMARY, common,
                    dispatches.stream().mapToInt(FleetDispatch::ships).sum()));
        };
        for (var entry : joint.selectedArrivals()) {
            VisibleSystem source = system(entry.getKey());
            if (source == null) { continue; }
            int available = Objects.requireNonNullElse(source.availableShips(), 0);
            IntegerField ships = amount();
            ships.setLabel(source.name());
            ships.setMax(available);
            ships.setValue(available);
            ships.addValueChangeListener(_ -> update.run());
            amounts.put(source.id(), ships);
            var start = new com.vaadin.flow.component.html.Span(startText(common - entry.getValue(), common));
            start.addClassName("joint-attack-start");
            var row = new com.vaadin.flow.component.html.Div(ships, start);
            row.addClassName("joint-attack-row");
            dialog.add(row);
        }
        dialog.add(summary, new Paragraph(I18n.t(UiTexts.JOINT_DIALOG_GARRISON)));
        if (view.systems().stream().anyMatch(system -> system.industry() != null)) {
            dialog.add(new Paragraph(I18n.t(UiTexts.CONQUEST_SELF)));
        }
        update.run();
        confirm.addClickListener(_ -> {
            boolean accepted = games.planJointAttack(id, player, view.turn(), target.id(), jointDispatches(amounts));
            if (!accepted) { Notification.show(I18n.t(UiTexts.PLAN_REJECTED)); return; }
            dialog.close();
            done.run();
        });
        confirm.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.PRIMARY);
        footer(dialog, confirm);
    }

    private String startText(int delay, int arrival) {
        return delay <= 0 ? I18n.t(UiTexts.JOINT_DIALOG_START_NOW, arrival)
                : I18n.t(UiTexts.JOINT_DIALOG_START_LATER, view.turn() + delay, delay, arrival);
    }

    /** Only valid amounts: a source left empty or above what it has drops out, which keeps the confirmation off. */
    private static List<FleetDispatch> jointDispatches(java.util.Map<Integer, IntegerField> amounts) {
        return amounts.entrySet().stream().filter(entry -> {
            Integer ships = entry.getValue().getValue();
            Integer max = entry.getValue().getMax();
            return ships != null && ships > 0 && ships <= max;
        }).map(entry -> new FleetDispatch(entry.getKey(), Objects.requireNonNull(entry.getValue().getValue()))).toList();
    }

    private @Nullable VisibleSystem system(int systemId) {
        return view.systems().stream().filter(system -> system.id() == systemId).findFirst().orElse(null);
    }

    /** Arrival along the route the order would take, as the game plans it; none if the target is out of reach. */
    private @Nullable Integer arrivalTurn(int source, int target) {
        return games.routeFor(id, player, source, target).map(route -> view.turn() + route.rounds()).orElse(null);
    }

    private ComboBox<VisibleSystem> targets() {
        ComboBox<VisibleSystem> target = new ComboBox<>(I18n.t(UiTexts.PLAN_TARGET));
        target.setItems(view.systems());
        target.setItemLabelGenerator(VisibleSystem::name);
        return target;
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
