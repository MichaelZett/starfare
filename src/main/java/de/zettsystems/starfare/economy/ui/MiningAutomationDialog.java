package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.economy.values.MiningAutomationView;
import de.zettsystems.starfare.economy.values.MiningPlan;
import de.zettsystems.starfare.economy.values.MiningRequest;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.i18n.I18n;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/** Explicit global preview before enabling or changing persistent mining assistance. */
public final class MiningAutomationDialog {
    private final Dialog dialog = new Dialog();
    private final Checkbox enabled = new Checkbox(I18n.t(UiTexts.MINING_ENABLED));
    private final IntegerField rounds = new IntegerField(I18n.t(UiTexts.MINING_ROUNDS));
    private final MultiSelectComboBox<Integer> systems = new MultiSelectComboBox<>(I18n.t(UiTexts.MINING_SYSTEMS));
    private final MultiSelectComboBox<Integer> protectedSystems = new MultiSelectComboBox<>(I18n.t(UiTexts.MINING_PROTECTED_SYSTEMS));
    private final Div result = new Div();
    private final Button save = new Button(I18n.t(UiTexts.MINING_SAVE));
    private @Nullable MiningRequest prepared;

    private MiningAutomationDialog(GameService game, GameId id, String account, MiningAutomationView view, Set<Integer> localPins) {
        dialog.setHeaderTitle(I18n.t(UiTexts.MINING_TITLE));
        dialog.setWidth("min(44rem, 95vw)");
        enabled.setValue(view.policy().enabled());
        rounds.setMin(0); rounds.setMax(10); rounds.setStepButtonsVisible(true); rounds.setValue(view.policy().reserveRounds());
        rounds.setId("mining-reserve-rounds");
        var choices = view.systems().keySet().stream().sorted().toList();
        systems.setItems(choices); systems.setItemLabelGenerator(key -> Objects.requireNonNull(view.systems().get(key))); systems.setWidthFull();
        systems.setValue(view.policy().revision() == 0 ? view.systems().keySet() : view.policy().systems().stream()
                .filter(view.systems()::containsKey).collect(Collectors.toSet()));
        protectedSystems.setItems(choices); protectedSystems.setItemLabelGenerator(key -> Objects.requireNonNull(view.systems().get(key))); protectedSystems.setWidthFull();
        Set<Integer> locks = new HashSet<>(view.policy().protectedSystems()); locks.addAll(localPins);
        locks.retainAll(view.systems().keySet()); protectedSystems.setValue(locks);
        dialog.add(new Paragraph(I18n.t(UiTexts.MINING_EXPLANATION)), enabled, rounds, systems, protectedSystems,
                new Paragraph(I18n.t(UiTexts.MINING_MANUAL_HINT)));
        if (view.policy().plannedTurn() > 0) {
            dialog.add(new Paragraph(I18n.t(UiTexts.MINING_LAST_PLAN, view.policy().plannedTurn())));
            Div previous = new Div(); showPlan(previous, view.policy().lastPlan(), view); dialog.add(previous);
        }
        result.getElement().setAttribute("aria-live", "polite");
        dialog.add(result);
        Button preview = new Button(I18n.t(UiTexts.MINING_PREVIEW), _ -> {
            Integer value = rounds.getValue();
            if (value == null) { return; }
            Set<Integer> protectedIds = protectedSystems.getValue().stream().filter(systems.getValue()::contains).collect(Collectors.toSet());
            MiningRequest request = new MiningRequest(view.turn(), view.policy(), view.plans(), enabled.getValue(),
                    value, systems.getValue(), protectedIds);
            var proposal = game.previewMining(id, account, request);
            result.removeAll();
            if (proposal.isEmpty()) { invalidate(); Notification.show(I18n.t(UiTexts.MINING_REJECTED)); return; }
            showPlan(result, proposal.orElseThrow(), view);
            prepared = request.withReviewed(proposal.orElseThrow()); save.setEnabled(true);
        });
        save.setEnabled(false);
        save.addClickListener(_ -> {
            MiningRequest request = prepared;
            if (request != null && game.configureMining(id, account, request)) { dialog.close(); }
            else { invalidate(); Notification.show(I18n.t(UiTexts.MINING_REJECTED)); }
        });
        enabled.addValueChangeListener(_ -> invalidate()); rounds.addValueChangeListener(_ -> invalidate());
        systems.addValueChangeListener(_ -> invalidate());
        protectedSystems.addValueChangeListener(event -> {
            Set<Integer> mandatory = localPins.stream().filter(view.systems()::containsKey).collect(Collectors.toSet());
            if (!event.getValue().containsAll(mandatory)) {
                Set<Integer> protectedIds = new HashSet<>(event.getValue()); protectedIds.addAll(mandatory);
                protectedSystems.setValue(protectedIds);
            }
            invalidate();
        });
        enabled.setEnabled(view.editable()); rounds.setEnabled(view.editable());
        systems.setEnabled(view.editable()); protectedSystems.setEnabled(view.editable()); preview.setEnabled(view.editable());
        dialog.getFooter().add(preview, save, new Button(I18n.t(UiTexts.MANAGE_CLOSE), _ -> dialog.close()));
        dialog.open();
    }

    private void invalidate() { prepared = null; save.setEnabled(false); result.removeAll(); }

    private static void showPlan(Div output, MiningPlan plan, MiningAutomationView view) {
        output.add(new Paragraph(I18n.t(UiTexts.MINING_FORECAST, plan.demand(), plan.target(), plan.stock(), plan.shortfall())));
        plan.changes().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach(entry -> {
            String name = Objects.requireNonNullElse(view.systems().get(entry.getKey()), String.valueOf(entry.getKey()));
            output.add(new Paragraph(I18n.t(UiTexts.MINING_CHANGE, name, entry.getValue().before(), entry.getValue().after())));
        });
        if (plan.shortfall() > 0) { output.add(new Paragraph(I18n.t(UiTexts.MINING_SHORTFALL))); }
    }

    public static void open(GameService game, GameId id, String account, Set<Integer> localPins) {
        game.miningAutomationFor(id, account).ifPresent(view -> new MiningAutomationDialog(game, id, account, view, localPins));
    }
}
