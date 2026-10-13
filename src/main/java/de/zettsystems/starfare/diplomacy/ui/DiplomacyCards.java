package de.zettsystems.starfare.diplomacy.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.diplomacy.values.*;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.CssProperties;
import java.util.Set;
import java.util.function.BiConsumer;

/** Small treaty cards, backed by a local notice draft and existing authorized commands. */
final class DiplomacyCards {
    private final DiplomacyDrafts drafts;
    private final BiConsumer<DiplomacyView, DiplomacyOrder> send;
    private final BiConsumer<DiplomacyView, AllianceGroup> leave;
    DiplomacyCards(DiplomacyDrafts drafts, BiConsumer<DiplomacyView, DiplomacyOrder> send,
                   BiConsumer<DiplomacyView, AllianceGroup> leave) {
        this.drafts = drafts; this.send = send; this.leave = leave;
    }
    Div group(DiplomacyView view, AllianceGroup group) {
        Div card = card();
        card.add(new Span(I18n.t(DiplomacyKeys.GROUP, group.id(), group.noticeRounds())));
        group.members().stream().sorted().forEach(member -> card.add(member(view, member)));
        group.departures().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach(exit ->
                card.add(new Span(I18n.t(UiTexts.DIP_DEPARTURE, name(view, exit.getKey()), exit.getValue())),
                        new Span(I18n.t(DiplomacyKeys.REMAINING, Math.max(0, exit.getValue() - view.turn())))));
        if (group.members().contains(view.player())) {
            if (!group.departures().containsKey(view.player())) {
                Button button = new Button(I18n.t(UiTexts.DIP_LEAVE), _ -> leave.accept(view, group));
                button.setEnabled(view.canAct()); card.add(button);
            }
            card.add(notice(view, new DiplomacyDrafts.Key(DiplomacyOrder.Action.NOTICE, group.id()),
                    group.noticeRounds(), DiplomacyKeys.EDIT_NOTICE, UiTexts.DIP_CHANGE));
        } else if (view.treaties().groupFor(view.player()).isEmpty()) {
            card.add(action(view, UiTexts.DIP_JOIN, new DiplomacyOrder(DiplomacyOrder.Action.JOIN, group.id(), group.noticeRounds())));
        }
        return card;
    }
    Div proposal(DiplomacyView view, TreatyProposal proposal) {
        Div card = card(); card.addClassName("diplomacy-proposal");
        String key = switch (proposal.kind()) { case FOUND -> UiTexts.DIP_FOUND_PROPOSAL; case JOIN -> UiTexts.DIP_JOIN_PROPOSAL; case NOTICE -> UiTexts.DIP_NOTICE_PROPOSAL; };
        card.add(new Span(I18n.t(key, name(view, proposal.candidate()), proposal.noticeRounds(), proposal.groupId(), names(view, proposal.voters()))));
        var remaining = new java.util.HashSet<>(proposal.voters()); remaining.removeAll(proposal.approvals());
        card.add(new Span(I18n.t(DiplomacyKeys.APPROVED, names(view, proposal.approvals()))),
                new Span(I18n.t(UiTexts.DIP_PENDING, names(view, remaining))));
        if (!proposal.approvals().contains(view.player())) {
            card.add(action(view, UiTexts.DIP_APPROVE, new DiplomacyOrder(DiplomacyOrder.Action.APPROVE, proposal.id(), 0)));
        } else { card.add(new Span(I18n.t(DiplomacyKeys.WAITING, names(view, remaining)))); }
        card.add(action(view, UiTexts.DIP_REJECT, new DiplomacyOrder(DiplomacyOrder.Action.REJECT, proposal.id(), 0)));
        return card;
    }
    /** AI empire in a game without AI alliances: shown, but explained instead of offered. */
    Div blockedPartner(DiplomacyView view, Player player) {
        Div card = card(); card.add(member(view, player.id()));
        card.getElement().setAttribute(de.zettsystems.starfare.style.HtmlAttributes.DIPLOMACY_PARTNER, String.valueOf(player.id()));
        Span reason = new Span(I18n.t(UiTexts.DIP_AI_BLOCKED));
        reason.addClassName("diplomacy-ai-blocked");
        card.add(reason);
        return card;
    }

    Div partner(DiplomacyView view, Player player) {
        Div card = card(); card.add(member(view, player.id()));
        card.getElement().setAttribute(de.zettsystems.starfare.style.HtmlAttributes.DIPLOMACY_PARTNER, String.valueOf(player.id()));
        card.add(notice(view, new DiplomacyDrafts.Key(DiplomacyOrder.Action.FOUND, player.id()),
                3, DiplomacyKeys.FOUND_WITH, UiTexts.DIP_FOUND));
        return card;
    }
    private Details notice(DiplomacyView view, DiplomacyDrafts.Key key, int initial, String caption, String action) {
        IntegerField field = new IntegerField(I18n.t(UiTexts.DIP_NOTICE));
        field.setMin(0); field.setMax(50); field.setValue(drafts.value(key, initial)); field.setStepButtonsVisible(true);
        field.setEnabled(view.canAct());
        boolean canPropose = view.canAct() && view.treaties().proposals().stream()
                .noneMatch(proposal -> proposal.voters().contains(view.player()));
        Button propose = new Button(I18n.t(action), _ -> {
            Integer value = field.getValue();
            if (value != null && value >= 0 && value <= 50) { send.accept(view, new DiplomacyOrder(key.action(), key.target(), value)); }
        });
        Integer initialValue = field.getValue();
        propose.setEnabled(canPropose && initialValue != null && initialValue >= 0 && initialValue <= 50);
        field.addValueChangeListener(event -> {
            Integer value = event.getValue();
            boolean valid = value != null && value >= 0 && value <= 50;
            propose.setEnabled(canPropose && valid);
            if (event.isFromClient() && value != null) { drafts.edit(key, value); }
        });
        Details details = new Details(I18n.t(caption), new Div(field, propose));
        details.setOpened(drafts.opened(key));
        details.addOpenedChangeListener(event -> drafts.open(key, event.isOpened()));
        return details;
    }
    private Button action(DiplomacyView view, String key, DiplomacyOrder order) {
        Button button = new Button(I18n.t(key), _ -> send.accept(view, order)); button.setEnabled(view.canAct()); return button;
    }
    private static Span member(DiplomacyView view, int id) {
        Span label = new Span(name(view, id)); label.addClassName("diplomacy-member");
        view.players().stream().filter(player -> player.id() == id).findFirst().ifPresent(player ->
                label.getStyle().set(CssProperties.BORDER_COLOR, player.colorHex()));
        return label;
    }
    private static Div card() { Div card = new Div(); card.addClassName("diplomacy-card"); return card; }
    private static String names(DiplomacyView view, Set<Integer> members) {
        return String.join(", ", members.stream().sorted().map(player -> name(view, player)).toList());
    }
    private static String name(DiplomacyView view, int player) {
        return view.players().stream().filter(candidate -> candidate.id() == player).map(Player::label).findFirst().orElse("?");
    }
}
