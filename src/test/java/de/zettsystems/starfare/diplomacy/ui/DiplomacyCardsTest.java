package de.zettsystems.starfare.diplomacy.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.diplomacy.values.AllianceGroup;
import de.zettsystems.starfare.diplomacy.values.DiplomacyOrder;
import de.zettsystems.starfare.diplomacy.values.DiplomacyState;
import de.zettsystems.starfare.diplomacy.values.DiplomacyView;
import de.zettsystems.starfare.diplomacy.values.TreatyProposal;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.style.CssProperties;
import de.zettsystems.starfare.style.HtmlAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiplomacyCardsTest {
    private static final List<Player> PLAYERS = List.of(
            new Player(1, "Ana", false, "#111111"), new Player(2, "Bo", false, "#222222"),
            new Player(3, "Cy", false, "#333333"), new Player(4, "Dee", true, "#444444"));

    private final List<DiplomacyOrder> sent = new ArrayList<>();
    private final List<DiplomacyView> sentViews = new ArrayList<>();
    private final List<AllianceGroup> left = new ArrayList<>();
    private final DiplomacyCards cards = new DiplomacyCards((view, order) -> { sentViews.add(view); sent.add(order); },
            (view, group) -> left.add(group));

    @Test
    void ownGroupShowsMembersAndLeaveButtonWithoutANoticeEditor() {
        var group = new AllianceGroup(5, Set.of(2, 1), 3, Map.of());
        var view = view(4, 1, true, state(group));

        var card = cards.group(view, group);

        assertThat(card.getClassNames()).contains("diplomacy-card");
        assertThat(spanTexts(card)).containsExactly("Bündnis 5 · Kündigungsfrist: 3 Runden", "Ana", "Bo");
        var members = spans(card).filter(span -> span.hasClassName("diplomacy-member")).toList();
        assertThat(members).extracting(span -> span.getStyle().get(CssProperties.BORDER_COLOR))
                .containsExactly("#111111", "#222222");
        assertThat(buttonTexts(card)).containsExactly("Bündnis kündigen");
        assertThat(descendants(card).filter(IntegerField.class::isInstance)).isEmpty();

        button(card, "Bündnis kündigen").click();
        assertThat(left).containsExactly(group);
        assertThat(sent).isEmpty();
    }

    @Test
    void departingMemberSeesRemainingRoundsButNoLeaveButton() {
        var group = new AllianceGroup(5, Set.of(1, 2), 3, Map.of(1, 7, 2, 9));
        var card = cards.group(view(5, 1, true, state(group)), group);

        assertThat(spanTexts(card)).containsSubsequence(
                "Ana scheidet zu Beginn von Runde 7 aus.", "Noch 2 Runden bis zum Austritt.",
                "Bo scheidet zu Beginn von Runde 9 aus.", "Noch 4 Runden bis zum Austritt.");
        assertThat(buttonTexts(card)).isEmpty();
    }

    @Test
    void remainingRoundsNeverDropBelowZero() {
        var group = new AllianceGroup(5, Set.of(1, 2), 3, Map.of(2, 7));
        var card = cards.group(view(8, 1, true, state(group)), group);

        assertThat(spanTexts(card)).contains("Bo scheidet zu Beginn von Runde 7 aus.", "Noch 0 Runden bis zum Austritt.");
        assertThat(button(card, "Bündnis kündigen")).isNotNull();
    }

    @Test
    void lockedOwnGroupDisablesLeaving() {
        var group = new AllianceGroup(5, Set.of(1, 2), 3, Map.of());
        var card = cards.group(view(4, 1, false, state(group)), group);

        assertThat(button(card, "Bündnis kündigen").isEnabled()).isFalse();
    }

    @Test
    void unalliedPlayerMayAskToJoinAForeignGroup() {
        var group = new AllianceGroup(5, Set.of(2, 3), 6, Map.of());
        var view = view(4, 1, true, state(group));
        var card = cards.group(view, group);

        assertThat(spanTexts(card)).containsExactly("Bündnis 5 · Kündigungsfrist: 6 Runden", "Bo", "Cy");
        assertThat(buttonTexts(card)).containsExactly("Beitritt beantragen");
        Button join = button(card, "Beitritt beantragen");
        assertThat(join.isEnabled()).isTrue();
        join.click();
        assertThat(sent).containsExactly(new DiplomacyOrder(DiplomacyOrder.Action.JOIN, 5));
        assertThat(sentViews).containsExactly(view);
    }

    @Test
    void joinIsDisabledWhenThePlayerCannotAct() {
        var group = new AllianceGroup(5, Set.of(2, 3), 6, Map.of());
        var card = cards.group(view(4, 1, false, state(group)), group);

        assertThat(button(card, "Beitritt beantragen").isEnabled()).isFalse();
    }

    @Test
    void memberOfAnotherGroupGetsNoActionOnAForeignGroup() {
        var own = new AllianceGroup(5, Set.of(1, 4), 3, Map.of());
        var foreign = new AllianceGroup(6, Set.of(2, 3), 3, Map.of());
        var card = cards.group(view(4, 1, true, new DiplomacyState(7, List.of(own, foreign), List.of())), foreign);

        assertThat(buttonTexts(card)).isEmpty();
    }

    @Test
    void unknownMembersAreShownAsQuestionMarkWithoutColor() {
        var group = new AllianceGroup(5, Set.of(2, 9), 3, Map.of());
        var card = cards.group(view(4, 1, true, state(group)), group);

        assertThat(spanTexts(card)).containsExactly("Bündnis 5 · Kündigungsfrist: 3 Runden", "Bo", "?");
        var unknown = spans(card).filter(span -> "?".equals(span.getText())).findFirst().orElseThrow();
        assertThat(unknown.getStyle().get(CssProperties.BORDER_COLOR)).isNull();
    }

    @Test
    void openFoundingProposalCanBeApprovedOrRejected() {
        var proposal = new TreatyProposal(1, TreatyProposal.Kind.FOUND, 0, 2, 3, Set.of(1, 2), Set.of(2));
        var view = view(4, 1, true, new DiplomacyState(2, List.of(), List.of(proposal)));

        var card = cards.proposal(view, proposal);

        assertThat(card.getClassNames()).contains("diplomacy-card", "diplomacy-proposal");
        assertThat(spanTexts(card)).containsExactly("Neues Bündnis: Ana, Bo",
                "Bereits zugestimmt: Bo", "Zustimmung ausstehend: Ana");
        assertThat(buttonTexts(card)).containsExactly("Zustimmen", "Ablehnen / zurückziehen");
        assertThat(button(card, "Zustimmen").isEnabled()).isTrue();
        button(card, "Zustimmen").click();
        button(card, "Ablehnen / zurückziehen").click();
        assertThat(sent).containsExactly(new DiplomacyOrder(DiplomacyOrder.Action.APPROVE, 1),
                new DiplomacyOrder(DiplomacyOrder.Action.REJECT, 1));
    }

    @Test
    void approvedJoinProposalWaitsForTheOthers() {
        var group = new AllianceGroup(5, Set.of(1, 2), 3, Map.of());
        var proposal = new TreatyProposal(6, TreatyProposal.Kind.JOIN, 5, 3, 3, Set.of(1, 2, 3), Set.of(1, 3));
        var view = view(4, 1, true, new DiplomacyState(7, List.of(group), List.of(proposal)));

        var card = cards.proposal(view, proposal);

        assertThat(spanTexts(card)).containsExactly("Cy beantragt Aufnahme in Bündnis 5.",
                "Bereits zugestimmt: Ana, Cy", "Zustimmung ausstehend: Bo", "Warte auf Bo");
        assertThat(buttonTexts(card)).containsExactly("Ablehnen / zurückziehen");
        button(card, "Ablehnen / zurückziehen").click();
        assertThat(sent).containsExactly(new DiplomacyOrder(DiplomacyOrder.Action.REJECT, 6));
    }

    @Test
    void proposalIsLockedWhenThePlayerCannotAct() {
        var proposal = new TreatyProposal(1, TreatyProposal.Kind.FOUND, 0, 2, 3, Set.of(1, 2), Set.of(2));
        var card = cards.proposal(view(4, 1, false, new DiplomacyState(2, List.of(), List.of(proposal))), proposal);

        assertThat(button(card, "Zustimmen").isEnabled()).isFalse();
        assertThat(button(card, "Ablehnen / zurückziehen").isEnabled()).isFalse();
    }

    @Test
    void partnerCardProposesAnAllianceWithOneClick() {
        var view = view(4, 1, true, DiplomacyState.EMPTY);
        var card = cards.partner(view, PLAYERS.get(1));

        assertThat(card.getElement().getAttribute(HtmlAttributes.DIPLOMACY_PARTNER)).isEqualTo("2");
        assertThat(spanTexts(card)).containsExactly("Bo");
        assertThat(buttonTexts(card)).containsExactly("Bündnis vorschlagen");
        assertThat(descendants(card).filter(IntegerField.class::isInstance)).isEmpty();
        Button found = button(card, "Bündnis vorschlagen");
        assertThat(found.isEnabled()).isTrue();

        found.click();
        assertThat(sent).containsExactly(new DiplomacyOrder(DiplomacyOrder.Action.FOUND, 2));
    }

    @Test
    void blockedPartnerIsExplainedInsteadOfOffered() {
        var card = cards.blockedPartner(view(4, 1, true, DiplomacyState.EMPTY), PLAYERS.get(3));

        assertThat(card.getElement().getAttribute(HtmlAttributes.DIPLOMACY_PARTNER)).isEqualTo("4");
        assertThat(spanTexts(card)).containsExactly("Dee", "KI-Imperien gehen in dieser Partie keine Bündnisse ein.");
        assertThat(spans(card).filter(span -> span.hasClassName("diplomacy-ai-blocked"))).hasSize(1);
        assertThat(buttonTexts(card)).isEmpty();
    }

    @Test
    void pendingVoteOfThePlayerBlocksNewProposals() {
        var proposal = new TreatyProposal(1, TreatyProposal.Kind.FOUND, 0, 1, 3, Set.of(1, 3), Set.of(1));
        var card = cards.partner(view(4, 1, true, new DiplomacyState(2, List.of(), List.of(proposal))), PLAYERS.get(1));

        assertThat(button(card, "Bündnis vorschlagen").isEnabled()).isFalse();
    }

    @Test
    void proposalsOfOthersDoNotBlockNewProposals() {
        var proposal = new TreatyProposal(1, TreatyProposal.Kind.FOUND, 0, 3, 3, Set.of(3, 4), Set.of(3));
        var card = cards.partner(view(4, 1, true, new DiplomacyState(2, List.of(), List.of(proposal))), PLAYERS.get(1));

        assertThat(button(card, "Bündnis vorschlagen").isEnabled()).isTrue();
    }

    @Test
    void lockedPartnerCardCannotPropose() {
        var card = cards.partner(view(4, 1, false, DiplomacyState.EMPTY), PLAYERS.get(1));

        assertThat(button(card, "Bündnis vorschlagen").isEnabled()).isFalse();
    }

    private static DiplomacyView view(int turn, int player, boolean canAct, DiplomacyState state) {
        return new DiplomacyView(turn, player, canAct, PLAYERS, state);
    }

    private static DiplomacyState state(AllianceGroup group) {
        return new DiplomacyState(group.id() + 1, List.of(group), List.of());
    }

    static Stream<Component> descendants(Component component) {
        return component.getChildren().flatMap(child -> Stream.concat(Stream.of(child), descendants(child)));
    }

    static Stream<Span> spans(Component component) {
        return descendants(component).filter(Span.class::isInstance).map(Span.class::cast);
    }

    static List<String> spanTexts(Component component) {
        return spans(component).map(Span::getText).filter(text -> !text.isEmpty()).toList();
    }

    static List<String> buttonTexts(Component component) {
        return descendants(component).filter(Button.class::isInstance).map(Button.class::cast).map(Button::getText).toList();
    }

    static Button button(Component component, String text) {
        return descendants(component).filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
    }

    static <T extends Component> T single(Component component, Class<T> type) {
        var matches = descendants(component).filter(type::isInstance).map(type::cast).toList();
        assertThat(matches).hasSize(1);
        return matches.getFirst();
    }
}
