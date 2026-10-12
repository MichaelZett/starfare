package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.i18n.I18n;

/** The choice is persisted with the command, then travels with its fleet. */
final class ConquestSelection extends VerticalLayout {
    private final ComboBox<Player> beneficiary = new ComboBox<>(I18n.t(UiTexts.CONQUEST_BENEFICIARY));
    private final int player;
    private final boolean available;
    private final java.util.Set<Integer> allies;
    ConquestSelection(GameService games, GameId id, int player) {
        this.player = player;
        setPadding(false); setSpacing(false);
        var candidates = games.conquestCandidates(id, player);
        available = candidates.size() > 1;
        allies = candidates.stream().map(Player::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
        beneficiary.setItems(candidates);
        beneficiary.setItemLabelGenerator(Player::empireNameOrName);
        beneficiary.setValue(candidates.stream().filter(candidate -> candidate.id() == player).findFirst().orElse(null));
        beneficiary.setWidthFull();
        Paragraph hint = new Paragraph(I18n.t(UiTexts.UX_CONQUEST_HINT)); hint.addClassName("conquest-hint");
        var options = new VerticalLayout(beneficiary, hint);
        options.setPadding(false);
        var details = new com.vaadin.flow.component.details.Details(I18n.t(UiTexts.UX_CONQUEST_PARTNER), options);
        var choice = new com.vaadin.flow.component.html.Span();
        choice.addClassName("conquest-result");
        beneficiary.addValueChangeListener(event -> {
            var selected = event.getValue();
            choice.setVisible(selected != null && selected.id() != player);
            if (selected != null) { choice.setText(I18n.t(UiTexts.UX_CONQUEST_RESULT, selected.empireNameOrName())); }
        });
        choice.setVisible(false);
        add(details, choice);
        addClassName("conquest-selection");
    }
    boolean availableFor(@org.jspecify.annotations.Nullable Integer owner) { return available && (owner == null || !allies.contains(owner)); }
    int beneficiary() {
        Player selected = beneficiary.getValue();
        return selected == null ? player : selected.id();
    }
}
