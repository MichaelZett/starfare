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
import java.util.function.Consumer;

/** The map host refreshes an open dialog; this adds no independent subscription. */
public class DiplomacyDialog extends Dialog {
    private final GameService game;
    private final GameId id;
    private final String account;
    private final Div content = new Div();
    private final DiplomacyCards cards;
    private final Consumer<String> notify;
    private @org.jspecify.annotations.Nullable ConfirmDialog leaveConfirmation;

    public DiplomacyDialog(GameService game, GameId id, String account) {
        this(game, id, account, Notification::show);
    }
    /** {@code notify} shows a short message; tests capture it instead of opening a notification. */
    DiplomacyDialog(GameService game, GameId id, String account, Consumer<String> notify) {
        this.game = game; this.id = id; this.account = account; this.notify = notify;
        cards = new DiplomacyCards(this::send, this::leave);
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
        // Former notice changes no longer decide anything; they lapse at the next round.
        var proposals = view.treaties().proposals().stream().filter(proposal -> proposal.voters().contains(view.player()))
                .filter(proposal -> proposal.kind() != TreatyProposal.Kind.NOTICE)
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
                    .forEach(player -> others.add(!view.aiAlliancesAllowed() && player.ai()
                            ? cards.blockedPartner(view, player) : cards.partner(view, player)));
        }
        if (others.getChildren().findAny().isPresent()) { content.add(new H3(I18n.t(DiplomacyKeys.OTHERS)), others); }
        content.add(new Details(I18n.t(DiplomacyKeys.BACKGROUND),
                new Span(I18n.t(UiTexts.DIP_RULES, AllianceGroup.NOTICE_ROUNDS))));
    }
    private void send(DiplomacyView view, DiplomacyOrder order) {
        if (!isOpened()) { return; }
        if (!game.diplomacyOrder(id, account, view.turn(), order)) { notify.accept(I18n.t(UiTexts.DIP_REJECTED)); }
        refresh();
    }
    private void leave(DiplomacyView view, AllianceGroup group) {
        ConfirmDialog confirm = new ConfirmDialog(); confirm.setId("diplomacy-leave-confirm");
        leaveConfirmation = confirm;
        confirm.setHeader(I18n.t(DiplomacyKeys.LEAVE_TITLE));
        confirm.setText(I18n.t(DiplomacyKeys.LEAVE_EFFECT, group.departureTurn(view.turn())));
        confirm.setCancelable(true); confirm.setCancelText(I18n.t(UiTexts.MAP_DIALOG_CANCEL));
        confirm.setConfirmText(I18n.t(DiplomacyKeys.CONFIRM_LEAVE));
        confirm.addConfirmListener(_ -> send(view, new DiplomacyOrder(DiplomacyOrder.Action.LEAVE, group.id())));
        confirm.open();
    }
}
