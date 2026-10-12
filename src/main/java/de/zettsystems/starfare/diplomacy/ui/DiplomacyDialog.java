package de.zettsystems.starfare.diplomacy.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import de.zettsystems.starfare.diplomacy.values.*;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.ui.UiTexts;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.i18n.I18n;
import java.util.HashSet;

/** The map host refreshes an open dialog; this adds no independent subscription. */
public class DiplomacyDialog extends Dialog {
    private final GameService game;
    private final GameId id;
    private final String account;
    private final Div content = new Div();
    private final DiplomacyDrafts drafts = new DiplomacyDrafts();
    private final DiplomacyCards cards;
    private @org.jspecify.annotations.Nullable ConfirmDialog leaveConfirmation;

    public DiplomacyDialog(GameService game, GameId id, String account) {
        this.game = game; this.id = id; this.account = account;
        cards = new DiplomacyCards(drafts, this::send, this::leave);
        setHeaderTitle(I18n.t(UiTexts.DIP_TITLE)); setWidth("640px");
        getElement().getStyle().set(de.zettsystems.starfare.style.CssProperties.MAX_WIDTH, "calc(100vw - 32px)");
        content.addClassName("diplomacy-content"); add(content);
        getFooter().add(new Button(I18n.t(UiTexts.DIP_CLOSE), _ -> close()));
        addOpenedChangeListener(event -> {
            var confirmation = leaveConfirmation;
            if (!event.isOpened() && confirmation != null) { confirmation.close(); leaveConfirmation = null; }
        });
    }
    public void refresh() {
        var current = game.diplomacyFor(id, account);
        if (current.isEmpty()) { close(); return; }
        var view = current.orElseThrow(); content.removeAll();
        if (!view.canAct()) { content.add(new Span(I18n.t(UiTexts.DIP_LOCKED))); }
        retainDrafts(view);
        var proposals = view.treaties().proposals().stream().filter(proposal -> proposal.voters().contains(view.player()))
                .sorted(java.util.Comparator.comparing((TreatyProposal proposal) -> proposal.approvals().contains(view.player()))
                        .thenComparingInt(TreatyProposal::id)).toList();
        if (!proposals.isEmpty()) {
            content.add(new H3(I18n.t(DiplomacyKeys.DECISIONS)));
            proposals.forEach(proposal -> content.add(cards.proposal(view, proposal)));
        }
        view.treaties().groupFor(view.player()).ifPresent(group -> {
            content.add(new H3(I18n.t(DiplomacyKeys.OWN)), cards.group(view, group));
        });
        Div others = new Div(); others.addClassName("diplomacy-others");
        view.treaties().groups().stream().filter(group -> !group.members().contains(view.player()))
                .sorted(java.util.Comparator.comparingInt(AllianceGroup::id)).forEach(group -> others.add(cards.group(view, group)));
        if (view.treaties().groupFor(view.player()).isEmpty()) {
            view.players().stream().filter(player -> player.id() != view.player() && view.treaties().groupFor(player.id()).isEmpty())
                    .forEach(player -> others.add(cards.partner(view, player)));
        }
        if (others.getChildren().findAny().isPresent()) { content.add(new H3(I18n.t(DiplomacyKeys.OTHERS)), others); }
        content.add(new Details(I18n.t(DiplomacyKeys.BACKGROUND), new Span(I18n.t(UiTexts.DIP_RULES))));
    }
    private void retainDrafts(DiplomacyView view) {
        var valid = new HashSet<DiplomacyDrafts.Key>();
        view.treaties().groupFor(view.player()).ifPresent(group -> valid.add(new DiplomacyDrafts.Key(DiplomacyOrder.Action.NOTICE, group.id())));
        if (view.treaties().groupFor(view.player()).isEmpty()) {
            view.players().stream().filter(player -> player.id() != view.player() && view.treaties().groupFor(player.id()).isEmpty())
                    .forEach(player -> valid.add(new DiplomacyDrafts.Key(DiplomacyOrder.Action.FOUND, player.id())));
        }
        if (drafts.retain(valid)) { Notification.show(I18n.t(DiplomacyKeys.LOST_TARGET)); }
    }
    private void send(DiplomacyView view, DiplomacyOrder order) {
        if (!isOpened()) { return; }
        if (game.diplomacyOrder(id, account, view.turn(), order)) {
            drafts.completed(new DiplomacyDrafts.Key(order.action(), order.subject()));
        } else { Notification.show(I18n.t(UiTexts.DIP_REJECTED)); }
        refresh();
    }
    private void leave(DiplomacyView view, AllianceGroup group) {
        ConfirmDialog confirm = new ConfirmDialog(); confirm.setId("diplomacy-leave-confirm");
        leaveConfirmation = confirm;
        confirm.setHeader(I18n.t(DiplomacyKeys.LEAVE_TITLE));
        confirm.setText(I18n.t(DiplomacyKeys.LEAVE_EFFECT, group.departureTurn(view.turn())));
        confirm.setCancelable(true); confirm.setCancelText(I18n.t(UiTexts.MAP_DIALOG_CANCEL));
        confirm.setConfirmText(I18n.t(DiplomacyKeys.CONFIRM_LEAVE));
        confirm.addConfirmListener(_ -> send(view, new DiplomacyOrder(DiplomacyOrder.Action.LEAVE, group.id(), 0)));
        confirm.open();
    }
}
