package de.zettsystems.starfare.diplomacy.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.diplomacy.values.*;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.i18n.I18n;

/** Treaty controls consume only immutable values and account-authorized service methods. */
public class DiplomacyDialog extends Dialog {
    private final GameService game;
    private final GameId id;
    private final String account;
    private final Div content = new Div();
    public DiplomacyDialog(GameService game, GameId id, String account) {
        this.game = game; this.id = id; this.account = account;
        setHeaderTitle(I18n.t(UiTexts.DIP_TITLE));
        setWidth("640px");
        getElement().getStyle().set(de.zettsystems.starfare.style.CssProperties.MAX_WIDTH, "calc(100vw - 32px)");
        content.addClassName("diplomacy-content"); add(content);
        getFooter().add(new Button(I18n.t(UiTexts.DIP_CLOSE), _ -> close()));
    }
    public void refresh() {
        var current = game.diplomacyFor(id, account);
        if (current.isEmpty()) { close(); return; }
        var view = current.orElseThrow(); content.removeAll();
        content.add(new Span(I18n.t(UiTexts.DIP_RULES)));
        if (!view.canAct()) { content.add(new Span(I18n.t(UiTexts.DIP_LOCKED))); }
        for (var group : view.treaties().groups()) { addGroup(view, group); }
        for (var proposal : view.treaties().proposals()) {
            if (proposal.voters().contains(view.player())) { addProposal(view, proposal); }
        }
        addRequests(view);
    }
    private void addGroup(DiplomacyView view, AllianceGroup group) {
        Div card = card();
        card.add(new Span(I18n.t(UiTexts.DIP_GROUP, group.id(), names(view, group.members()), group.noticeRounds())));
        group.departures().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach(exit ->
                card.add(new Span(I18n.t(UiTexts.DIP_DEPARTURE, name(view, exit.getKey()), exit.getValue()))));
        if (group.members().contains(view.player())) {
            if (!group.departures().containsKey(view.player())) {
                card.add(action(view, UiTexts.DIP_LEAVE, new DiplomacyOrder(DiplomacyOrder.Action.LEAVE, group.id(), 0)));
            }
            IntegerField notice = noticeField(group.noticeRounds());
            Button change = new Button(I18n.t(UiTexts.DIP_CHANGE), _ -> send(view, new DiplomacyOrder(DiplomacyOrder.Action.NOTICE, group.id(), value(notice))));
            change.setEnabled(view.canAct()); card.add(notice, change);
        } else if (view.treaties().groupFor(view.player()).isEmpty()) {
            card.add(action(view, UiTexts.DIP_JOIN, new DiplomacyOrder(DiplomacyOrder.Action.JOIN, group.id(), group.noticeRounds())));
        }
        content.add(card);
    }
    private void addProposal(DiplomacyView view, TreatyProposal proposal) {
        Div card = card();
        String key = switch (proposal.kind()) { case FOUND -> UiTexts.DIP_FOUND_PROPOSAL; case JOIN -> UiTexts.DIP_JOIN_PROPOSAL; case NOTICE -> UiTexts.DIP_NOTICE_PROPOSAL; };
        card.add(new Span(I18n.t(key, name(view, proposal.candidate()), proposal.noticeRounds(), proposal.groupId(), names(view, proposal.voters()))));
        var remaining = new java.util.HashSet<>(proposal.voters()); remaining.removeAll(proposal.approvals());
        card.add(new Span(I18n.t(UiTexts.DIP_PENDING, names(view, remaining))));
        if (!proposal.approvals().contains(view.player())) {
            card.add(action(view, UiTexts.DIP_APPROVE, new DiplomacyOrder(DiplomacyOrder.Action.APPROVE, proposal.id(), 0)));
        }
        card.add(action(view, UiTexts.DIP_REJECT, new DiplomacyOrder(DiplomacyOrder.Action.REJECT, proposal.id(), 0)));
        content.add(card);
    }
    private void addRequests(DiplomacyView view) {
        if (view.treaties().groupFor(view.player()).isPresent()) { return; }
        Div card = card();
        ComboBox<Player> partner = new ComboBox<>(I18n.t(UiTexts.DIP_PARTNER));
        partner.setItems(view.players().stream().filter(player -> player.id() != view.player() && view.treaties().groupFor(player.id()).isEmpty()).toList());
        partner.setItemLabelGenerator(Player::label);
        IntegerField notice = noticeField(3);
        Button propose = new Button(I18n.t(UiTexts.DIP_FOUND), _ -> {
            Player chosen = partner.getValue();
            if (chosen != null) { send(view, new DiplomacyOrder(DiplomacyOrder.Action.FOUND, chosen.id(), value(notice))); }
        });
        propose.setEnabled(false);
        partner.addValueChangeListener(event -> propose.setEnabled(view.canAct() && event.getValue() != null));
        card.add(partner, notice, propose); content.add(card);
    }
    private Button action(DiplomacyView view, String key, DiplomacyOrder order) {
        Button button = new Button(I18n.t(key), _ -> send(view, order)); button.setEnabled(view.canAct()); return button;
    }
    private void send(DiplomacyView view, DiplomacyOrder order) {
        if (!game.diplomacyOrder(id, account, view.turn(), order)) { Notification.show(I18n.t(UiTexts.DIP_REJECTED)); }
        refresh();
    }
    private static IntegerField noticeField(int initial) {
        IntegerField field = new IntegerField(I18n.t(UiTexts.DIP_NOTICE));
        field.setMin(0); field.setMax(50); field.setValue(initial); field.setStepButtonsVisible(true); return field;
    }
    private static int value(IntegerField field) { Integer current = field.getValue(); return current == null ? -1 : current; }
    private static Div card() { Div card = new Div(); card.addClassName("diplomacy-card"); return card; }
    private static String names(DiplomacyView view, java.util.Set<Integer> members) {
        return String.join(", ", members.stream().sorted().map(player -> name(view, player)).toList());
    }
    private static String name(DiplomacyView view, int player) {
        return view.players().stream().filter(candidate -> candidate.id() == player).map(Player::label).findFirst().orElse("?");
    }
}
