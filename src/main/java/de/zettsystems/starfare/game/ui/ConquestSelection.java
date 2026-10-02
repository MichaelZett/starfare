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
    ConquestSelection(GameService games, GameId id, int player) {
        this.player = player;
        setPadding(false); setSpacing(false);
        var candidates = games.conquestCandidates(id, player);
        beneficiary.setItems(candidates);
        beneficiary.setItemLabelGenerator(Player::label);
        beneficiary.setValue(candidates.stream().filter(candidate -> candidate.id() == player).findFirst().orElse(null));
        beneficiary.setWidthFull();
        Paragraph hint = new Paragraph(I18n.t(UiTexts.CONQUEST_HINT)); hint.addClassName("conquest-hint");
        add(beneficiary, hint);
        addClassName("conquest-selection");
    }
    int beneficiary() {
        Player selected = beneficiary.getValue();
        return selected == null ? player : selected.id();
    }
}
