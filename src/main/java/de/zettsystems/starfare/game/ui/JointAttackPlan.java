package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.value.ValueChangeMode;
import de.zettsystems.starfare.fleet.values.FleetDispatch;
import de.zettsystems.starfare.game.values.VisibleSystem;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.HtmlAttributes;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

/**
 * The joint attack as a plan in the sidebar (K9): from the click on a target it shows the target's known defence,
 * the chosen sources with their ships, start and arrival, the possible sources by arrival, the sum and exactly one
 * button that plans the whole attack. Map badges and the ticks here pick the same sources. Amounts are kept by the
 * view across refreshes; editing one only updates the plan, so typing is not interrupted.
 */
final class JointAttackPlan extends Div {

    /** One own system that can reach the target this round. */
    record Source(int systemId, String name, int arrival, int available) { }

    private final Map<Integer, IntegerField> amounts = new LinkedHashMap<>();
    private final Map<Integer, Integer> remembered;
    private final Span summary = new Span();
    private final Button confirm = new Button();
    private final boolean ownTarget;
    private final int commonArrival;

    /**
     * @param target     the system to meet at
     * @param ownTarget  whether the target is the player's own (a joint send, not an attack)
     * @param sources    every possible source, chosen or not
     * @param chosen     ids of the chosen sources
     * @param turn       the current round
     * @param remembered amounts per source the player typed before, kept by the view; updated here
     * @param onToggle   picks or drops a source, like its map badge
     * @param onConfirm  plans the attack; false when the game refused it
     * @param onCancel   closes the plan
     */
    @SuppressWarnings("java:S107") // rationale: one call site builds the plan from the view's state.
    JointAttackPlan(VisibleSystem target, boolean ownTarget, List<Source> sources, java.util.Set<Integer> chosen,
                    int turn, Map<Integer, Integer> remembered, IntConsumer onToggle,
                    Predicate<List<FleetDispatch>> onConfirm, Runnable onCancel) {
        this.remembered = remembered;
        this.ownTarget = ownTarget;
        addClassName("joint-plan");
        List<Source> byArrival = sources.stream()
                .sorted(Comparator.comparingInt(Source::arrival).thenComparing(Source::name)).toList();
        List<Source> picked = byArrival.stream().filter(source -> chosen.contains(source.systemId())).toList();
        List<Source> open = byArrival.stream().filter(source -> !chosen.contains(source.systemId())).toList();
        commonArrival = picked.stream().mapToInt(Source::arrival).max().orElse(0);

        H3 title = new H3(I18n.t(ownTarget ? UiTexts.JOINT_PLAN_TITLE_SEND : UiTexts.JOINT_PLAN_TITLE_ATTACK,
                target.name()));
        title.addClassName("joint-plan-title");
        Button close = new Button(VaadinIcon.CLOSE_SMALL.create(), _ -> onCancel.run());
        close.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
        close.setAriaLabel(I18n.t(UiTexts.JOINT_PLAN_CLOSE));
        Div head = new Div(title, close);
        head.addClassName("joint-plan-head");
        Span defence = new Span(I18n.t(UiTexts.JOINT_PLAN_DEFENCE, target.name(), known(target.garrison(), target),
                known(target.productionPerTurn(), target)));
        defence.addClassName("joint-plan-defence");
        add(head, defence);

        if (!picked.isEmpty()) {
            add(section(UiTexts.JOINT_PLAN_CHOSEN));
            picked.forEach(source -> add(chosenRow(source, turn, onToggle)));
        }
        if (!open.isEmpty()) {
            add(section(UiTexts.JOINT_PLAN_POSSIBLE));
            open.forEach(source -> add(possibleRow(source, onToggle)));
        }
        if (picked.isEmpty()) {
            Paragraph empty = new Paragraph(I18n.t(UiTexts.JOINT_PLAN_EMPTY));
            empty.addClassName("joint-plan-empty");
            add(empty);
        } else if (picked.stream().anyMatch(source -> source.arrival() < commonArrival)) {
            Paragraph garrison = new Paragraph(I18n.t(UiTexts.JOINT_DIALOG_GARRISON));
            garrison.addClassName("joint-plan-note");
            add(garrison);
        }

        summary.addClassName("joint-plan-summary");
        confirm.addThemeVariants(ButtonVariant.PRIMARY);
        confirm.addClassName("joint-plan-confirm");
        confirm.addClickListener(_ -> {
            if (!onConfirm.test(dispatches())) { Notification.show(I18n.t(UiTexts.PLAN_REJECTED)); }
        });
        Button cancel = new Button(I18n.t(UiTexts.JOINT_PLAN_CANCEL), _ -> onCancel.run());
        cancel.addThemeVariants(ButtonVariant.TERTIARY);
        Div actions = new Div(confirm, cancel);
        actions.addClassName("joint-plan-actions");
        add(summary, actions);
        update();
    }

    /** A known number, with "~" when it is only an estimate; "?" without any. */
    private static String known(@org.jspecify.annotations.Nullable Integer value, VisibleSystem system) {
        if (value == null) { return "?"; }
        return system.approximate() ? "~" + value : String.valueOf(value);
    }

    private static Span section(String key) {
        Span heading = new Span(I18n.t(key));
        heading.addClassName("joint-plan-section");
        return heading;
    }

    private Div chosenRow(Source source, int turn, IntConsumer onToggle) {
        Checkbox tick = new Checkbox(source.name(), true);
        tick.addValueChangeListener(_ -> onToggle.accept(source.systemId()));
        IntegerField ships = new IntegerField();
        ships.setAriaLabel(I18n.t(UiTexts.MAP_COLUMN_SHIPS) + " " + source.name());
        ships.setMin(1);
        ships.setMax(source.available());
        ships.setStepButtonsVisible(true);
        ships.setValueChangeMode(ValueChangeMode.EAGER);
        int value = remembered.getOrDefault(source.systemId(), source.available());
        ships.setValue(Math.clamp(value, 1, source.available()));
        ships.addValueChangeListener(event -> {
            Integer typed = event.getValue();
            if (typed != null) { remembered.put(source.systemId(), typed); }
            update();
        });
        amounts.put(source.systemId(), ships);
        Button all = new Button(I18n.t(UiTexts.JOINT_PLAN_ALL), _ -> ships.setValue(source.available()));
        all.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
        int delay = commonArrival - source.arrival();
        Span start = new Span(delay <= 0 ? I18n.t(UiTexts.JOINT_DIALOG_START_NOW, commonArrival)
                : I18n.t(UiTexts.JOINT_DIALOG_START_LATER, turn + delay, delay, commonArrival));
        start.addClassName("joint-plan-start");
        Div amount = new Div(ships, all);
        amount.addClassName("joint-plan-amount");
        Div row = new Div(tick, amount, start);
        row.addClassNames("joint-plan-row", "joint-plan-chosen");
        row.getElement().setAttribute(HtmlAttributes.DATA_SYSTEM_ID, String.valueOf(source.systemId()));
        return row;
    }

    private static Div possibleRow(Source source, IntConsumer onToggle) {
        Checkbox tick = new Checkbox(I18n.t(UiTexts.JOINT_PLAN_SOURCE, source.name(), source.arrival(),
                source.available()), false);
        tick.addValueChangeListener(_ -> onToggle.accept(source.systemId()));
        Div row = new Div(tick);
        row.addClassNames("joint-plan-row", "joint-plan-possible");
        row.getElement().setAttribute(HtmlAttributes.DATA_SYSTEM_ID, String.valueOf(source.systemId()));
        return row;
    }

    /** Valid amounts only: an empty field or one above what the source has drops out and keeps the button off. */
    List<FleetDispatch> dispatches() {
        return amounts.entrySet().stream().filter(entry -> valid(entry.getValue()))
                .map(entry -> new FleetDispatch(entry.getKey(), Objects.requireNonNull(entry.getValue().getValue())))
                .toList();
    }

    private static boolean valid(IntegerField field) {
        Integer ships = field.getValue();
        return ships != null && ships > 0 && ships <= field.getMax();
    }

    private void update() {
        List<FleetDispatch> dispatches = dispatches();
        int ships = dispatches.stream().mapToInt(FleetDispatch::ships).sum();
        boolean ready = !amounts.isEmpty() && dispatches.size() == amounts.size();
        summary.setText(amounts.isEmpty() ? "" : I18n.t(UiTexts.JOINT_PLAN_SUMMARY, commonArrival, ships));
        summary.setVisible(!amounts.isEmpty());
        confirm.setText(amounts.isEmpty() ? I18n.t(UiTexts.JOINT_DIALOG_CONFIRM) : confirmText(ships));
        confirm.setEnabled(ready);
    }

    private String confirmText(int ships) {
        String key = ownTarget ? UiTexts.JOINT_PLAN_CONFIRM_SEND : UiTexts.JOINT_PLAN_CONFIRM_ATTACK;
        return I18n.t(key, ships, commonArrival);
    }
}
