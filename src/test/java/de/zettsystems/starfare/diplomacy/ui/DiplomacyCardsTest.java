package de.zettsystems.starfare.diplomacy.ui;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.diplomacy.values.AllianceGroup;
import de.zettsystems.starfare.diplomacy.values.DiplomacyDrafts;
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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiplomacyCardsTest {
    private static final List<Player> PLAYERS = List.of(
            new Player(1, "Ana", false, "#111111"), new Player(2, "Bo", false, "#222222"),
            new Player(3, "Cy", false, "#333333"), new Player(4, "Dee", true, "#444444"));
    private static final DiplomacyDrafts.Key FOUND_WITH_BO = new DiplomacyDrafts.Key(DiplomacyOrder.Action.FOUND, 2);
    private static final DiplomacyDrafts.Key NOTICE_GROUP_5 = new DiplomacyDrafts.Key(DiplomacyOrder.Action.NOTICE, 5);

    private final DiplomacyDrafts drafts = new DiplomacyDrafts();
    private final List<DiplomacyOrder> sent = new ArrayList<>();
    private final List<DiplomacyView> sentViews = new ArrayList<>();
    private final List<AllianceGroup> left = new ArrayList<>();
    private final DiplomacyCards cards = new DiplomacyCards(drafts, (view, order) -> { sentViews.add(view); sent.add(order); },
            (view, group) -> left.add(group));

    @Test
    void ownGroupShowsMembersLeaveButtonAndNoticeEditor() {
        var group = new AllianceGroup(5, Set.of(2, 1), 3, Map.of());
        var view = view(4, 1, true, state(group));

        var card = cards.group(view, group);

        assertThat(card.getClassNames()).contains("diplomacy-card");
        assertThat(spanTexts(card)).containsExactly("Bündnis 5 · Kündigungsfrist: 3 Runden", "Ana", "Bo", "Frist ändern");
        var members = spans(card).filter(span -> span.hasClassName("diplomacy-member")).toList();
        assertThat(members).extracting(span -> span.getStyle().get(CssProperties.BORDER_COLOR))
                .containsExactly("#111111", "#222222");
        Button leave = button(card, "Bündnis kündigen");
        assertThat(leave.isEnabled()).isTrue();
        assertThat(buttonTexts(card)).doesNotContain("Beitritt beantragen");
        Details notice = single(card, Details.class);
        assertThat(notice.getSummaryText()).isEqualTo("Frist ändern");
        assertThat(single(card, IntegerField.class).getValue()).isEqualTo(3);

        leave.click();
        assertThat(left).containsExactly(group);
        button(card, "Neue Frist vorschlagen").click();
        assertThat(sent).containsExactly(new DiplomacyOrder(DiplomacyOrder.Action.NOTICE, 5, 3));
        assertThat(sentViews).containsExactly(view);
    }

    @Test
    void departingMemberSeesRemainingRoundsButNoLeaveButton() {
        var group = new AllianceGroup(5, Set.of(1, 2), 3, Map.of(1, 7, 2, 9));
        var card = cards.group(view(5, 1, true, state(group)), group);

        assertThat(spanTexts(card)).containsSubsequence(
                "Ana scheidet zu Beginn von Runde 7 aus.", "Noch 2 Runden bis zum Austritt.",
                "Bo scheidet zu Beginn von Runde 9 aus.", "Noch 4 Runden bis zum Austritt.");
        assertThat(buttonTexts(card)).containsExactly("Neue Frist vorschlagen");
        assertThat(descendants(card).filter(Details.class::isInstance)).hasSize(1);
    }

    @Test
    void remainingRoundsNeverDropBelowZero() {
        var group = new AllianceGroup(5, Set.of(1, 2), 3, Map.of(2, 7));
        var card = cards.group(view(8, 1, true, state(group)), group);

        assertThat(spanTexts(card)).contains("Bo scheidet zu Beginn von Runde 7 aus.", "Noch 0 Runden bis zum Austritt.");
        assertThat(button(card, "Bündnis kündigen")).isNotNull();
    }

    @Test
    void lockedOwnGroupDisablesEveryAction() {
        var group = new AllianceGroup(5, Set.of(1, 2), 3, Map.of());
        var card = cards.group(view(4, 1, false, state(group)), group);

        assertThat(button(card, "Bündnis kündigen").isEnabled()).isFalse();
        assertThat(button(card, "Neue Frist vorschlagen").isEnabled()).isFalse();
        assertThat(single(card, IntegerField.class).isEnabled()).isFalse();
    }

    @Test
    void unalliedPlayerMayAskToJoinAForeignGroup() {
        var group = new AllianceGroup(5, Set.of(2, 3), 6, Map.of());
        var view = view(4, 1, true, state(group));
        var card = cards.group(view, group);

        assertThat(spanTexts(card)).containsExactly("Bündnis 5 · Kündigungsfrist: 6 Runden", "Bo", "Cy");
        assertThat(buttonTexts(card)).containsExactly("Beitritt beantragen");
        assertThat(descendants(card).filter(Details.class::isInstance)).isEmpty();
        Button join = button(card, "Beitritt beantragen");
        assertThat(join.isEnabled()).isTrue();
        join.click();
        assertThat(sent).containsExactly(new DiplomacyOrder(DiplomacyOrder.Action.JOIN, 5, 6));
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
        assertThat(descendants(card).filter(Details.class::isInstance)).isEmpty();
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
        var proposal = new TreatyProposal(1, TreatyProposal.Kind.FOUND, 0, 2, 4, Set.of(1, 2), Set.of(2));
        var view = view(4, 1, true, new DiplomacyState(2, List.of(), List.of(proposal)));

        var card = cards.proposal(view, proposal);

        assertThat(card.getClassNames()).contains("diplomacy-card", "diplomacy-proposal");
        assertThat(spanTexts(card)).containsExactly("Neues Bündnis: Ana, Bo · vorgeschlagene Kündigungsfrist: 4 Runden",
                "Bereits zugestimmt: Bo", "Zustimmung ausstehend: Ana");
        assertThat(buttonTexts(card)).containsExactly("Zustimmen", "Ablehnen / zurückziehen");
        assertThat(button(card, "Zustimmen").isEnabled()).isTrue();
        button(card, "Zustimmen").click();
        button(card, "Ablehnen / zurückziehen").click();
        assertThat(sent).containsExactly(new DiplomacyOrder(DiplomacyOrder.Action.APPROVE, 1, 0),
                new DiplomacyOrder(DiplomacyOrder.Action.REJECT, 1, 0));
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
        assertThat(sent).containsExactly(new DiplomacyOrder(DiplomacyOrder.Action.REJECT, 6, 0));
    }

    @Test
    void noticeProposalNamesItsInitiatorAndIsLockedWhenThePlayerCannotAct() {
        var group = new AllianceGroup(5, Set.of(1, 2), 3, Map.of());
        var proposal = new TreatyProposal(6, TreatyProposal.Kind.NOTICE, 5, 2, 8, Set.of(1, 2), Set.of(2));
        var view = view(4, 1, false, new DiplomacyState(7, List.of(group), List.of(proposal)));

        var card = cards.proposal(view, proposal);

        assertThat(spanTexts(card).getFirst()).isEqualTo(
                "Bo schlägt für Bündnis 5 eine Frist von 8 Runden vor. Laufende Kündigungen behalten ihr Datum.");
        assertThat(button(card, "Zustimmen").isEnabled()).isFalse();
        assertThat(button(card, "Ablehnen / zurückziehen").isEnabled()).isFalse();
    }

    @Test
    void partnerCardOffersAFoundingProposalWithThreeRoundsByDefault() {
        var view = view(4, 1, true, DiplomacyState.EMPTY);
        var card = cards.partner(view, PLAYERS.get(1));

        assertThat(card.getElement().getAttribute(HtmlAttributes.DIPLOMACY_PARTNER)).isEqualTo("2");
        assertThat(spanTexts(card)).containsExactly("Bo", "Bündnis gründen");
        Details details = single(card, Details.class);
        assertThat(details.getSummaryText()).isEqualTo("Bündnis gründen");
        assertThat(details.isOpened()).isFalse();
        IntegerField field = single(card, IntegerField.class);
        assertThat(field.getLabel()).isEqualTo("Kündigungsfrist (0–50 Runden)");
        assertThat(field.getMin()).isZero();
        assertThat(field.getMax()).isEqualTo(50);
        assertThat(field.isStepButtonsVisible()).isTrue();
        assertThat(field.isEnabled()).isTrue();
        assertThat(field.getValue()).isEqualTo(3);
        Button found = button(card, "Bündnis vorschlagen");
        assertThat(found.isEnabled()).isTrue();

        found.click();
        assertThat(sent).containsExactly(new DiplomacyOrder(DiplomacyOrder.Action.FOUND, 2, 3));
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
    void noticeEditorRestoresTheDraftValueAndOpenState() {
        drafts.edit(FOUND_WITH_BO, 17);
        drafts.open(FOUND_WITH_BO, true);

        var card = cards.partner(view(4, 1, true, DiplomacyState.EMPTY), PLAYERS.get(1));

        assertThat(single(card, IntegerField.class).getValue()).isEqualTo(17);
        assertThat(single(card, Details.class).isOpened()).isTrue();
        button(card, "Bündnis vorschlagen").click();
        assertThat(sent).containsExactly(new DiplomacyOrder(DiplomacyOrder.Action.FOUND, 2, 17));
    }

    @Test
    void openingAndClosingTheEditorIsRememberedInTheDrafts() {
        var card = cards.partner(view(4, 1, true, DiplomacyState.EMPTY), PLAYERS.get(1));
        Details details = single(card, Details.class);

        details.setOpened(true);
        assertThat(drafts.opened(FOUND_WITH_BO)).isTrue();
        details.setOpened(false);
        assertThat(drafts.opened(FOUND_WITH_BO)).isFalse();
    }

    @Test
    void clientEditsAreStoredAsDraftsAndEnableProposingOnlyWithinBounds() {
        var card = cards.partner(view(4, 1, true, DiplomacyState.EMPTY), PLAYERS.get(1));
        IntegerField field = single(card, IntegerField.class);
        Button found = button(card, "Bündnis vorschlagen");

        clientValue(field, 0);
        assertThat(found.isEnabled()).isTrue();
        assertThat(drafts.value(FOUND_WITH_BO, -100)).isZero();

        clientValue(field, 50);
        assertThat(found.isEnabled()).isTrue();
        assertThat(drafts.value(FOUND_WITH_BO, -100)).isEqualTo(50);

        clientValue(field, 51);
        assertThat(found.isEnabled()).isFalse();
        assertThat(drafts.value(FOUND_WITH_BO, -100)).isEqualTo(51);

        clientValue(field, -1);
        assertThat(found.isEnabled()).isFalse();
        assertThat(drafts.value(FOUND_WITH_BO, -100)).isEqualTo(-1);

        clientValue(field, null);
        assertThat(found.isEnabled()).isFalse();
        assertThat(drafts.value(FOUND_WITH_BO, -100)).isEqualTo(-1);
    }

    @Test
    void serverSideValueChangesUpdateTheButtonButNotTheDraft() {
        var card = cards.partner(view(4, 1, true, DiplomacyState.EMPTY), PLAYERS.get(1));
        IntegerField field = single(card, IntegerField.class);
        Button found = button(card, "Bündnis vorschlagen");

        field.setValue(60);
        assertThat(found.isEnabled()).isFalse();
        field.setValue(8);
        assertThat(found.isEnabled()).isTrue();
        assertThat(drafts.value(FOUND_WITH_BO, -100)).isEqualTo(-100);
    }

    @Test
    void proposingSendsOnlyValuesWithinBounds() {
        var card = cards.partner(view(4, 1, true, DiplomacyState.EMPTY), PLAYERS.get(1));
        IntegerField field = single(card, IntegerField.class);
        Button found = button(card, "Bündnis vorschlagen");

        for (Integer value : new Integer[] {51, -1, null}) {
            field.setValue(value);
            found.click();
        }
        assertThat(sent).isEmpty();

        field.setValue(0);
        found.click();
        field.setValue(50);
        found.click();
        assertThat(sent).containsExactly(new DiplomacyOrder(DiplomacyOrder.Action.FOUND, 2, 0),
                new DiplomacyOrder(DiplomacyOrder.Action.FOUND, 2, 50));
    }

    @Test
    void forgedClickWithAnOutOfRangeValueSendsNothing() {
        var card = cards.partner(view(4, 1, true, DiplomacyState.EMPTY), PLAYERS.get(1));
        IntegerField field = single(card, IntegerField.class);
        Button found = button(card, "Bündnis vorschlagen");

        for (Integer value : new Integer[] {51, -1, null}) {
            field.setValue(value);
            assertThat(found.isEnabled()).isFalse();
            ComponentUtil.fireEvent(found, new ClickEvent<>(found));
        }
        assertThat(sent).isEmpty();

        field.setValue(50);
        ComponentUtil.fireEvent(found, new ClickEvent<>(found));
        assertThat(sent).containsExactly(new DiplomacyOrder(DiplomacyOrder.Action.FOUND, 2, 50));
    }

    @Test
    void initialDraftValueDecidesWhetherProposingIsEnabled() {
        assertThat(foundEnabledWithDraft(-1)).isFalse();
        assertThat(foundEnabledWithDraft(0)).isTrue();
        assertThat(foundEnabledWithDraft(50)).isTrue();
        assertThat(foundEnabledWithDraft(51)).isFalse();
    }

    @Test
    void pendingVoteOfThePlayerBlocksNewProposals() {
        var proposal = new TreatyProposal(1, TreatyProposal.Kind.FOUND, 0, 1, 4, Set.of(1, 3), Set.of(1));
        var card = cards.partner(view(4, 1, true, new DiplomacyState(2, List.of(), List.of(proposal))), PLAYERS.get(1));
        IntegerField field = single(card, IntegerField.class);
        Button found = button(card, "Bündnis vorschlagen");

        assertThat(field.isEnabled()).isTrue();
        assertThat(found.isEnabled()).isFalse();
        clientValue(field, 5);
        assertThat(found.isEnabled()).isFalse();
    }

    @Test
    void proposalsOfOthersDoNotBlockNewProposals() {
        var proposal = new TreatyProposal(1, TreatyProposal.Kind.FOUND, 0, 3, 4, Set.of(3, 4), Set.of(3));
        var card = cards.partner(view(4, 1, true, new DiplomacyState(2, List.of(), List.of(proposal))), PLAYERS.get(1));

        assertThat(button(card, "Bündnis vorschlagen").isEnabled()).isTrue();
    }

    @Test
    void lockedNoticeEditorStaysDisabledForValidValues() {
        var card = cards.partner(view(4, 1, false, DiplomacyState.EMPTY), PLAYERS.get(1));
        IntegerField field = single(card, IntegerField.class);
        Button found = button(card, "Bündnis vorschlagen");

        assertThat(field.isEnabled()).isFalse();
        assertThat(found.isEnabled()).isFalse();
        clientValue(field, 5);
        assertThat(found.isEnabled()).isFalse();
    }

    @Test
    void groupNoticeDraftIsKeptPerGroup() {
        var group = new AllianceGroup(5, Set.of(1, 2), 3, Map.of());
        drafts.edit(NOTICE_GROUP_5, 12);
        var card = cards.group(view(4, 1, true, state(group)), group);

        assertThat(single(card, IntegerField.class).getValue()).isEqualTo(12);
        clientValue(single(card, IntegerField.class), 20);
        assertThat(drafts.value(NOTICE_GROUP_5, 0)).isEqualTo(20);
        assertThat(drafts.value(FOUND_WITH_BO, 0)).isZero();
    }

    private boolean foundEnabledWithDraft(int draft) {
        drafts.edit(FOUND_WITH_BO, draft);
        return button(cards.partner(view(4, 1, true, DiplomacyState.EMPTY), PLAYERS.get(1)), "Bündnis vorschlagen").isEnabled();
    }

    private static DiplomacyView view(int turn, int player, boolean canAct, DiplomacyState state) {
        return new DiplomacyView(turn, player, canAct, PLAYERS, state);
    }

    private static DiplomacyState state(AllianceGroup group) {
        return new DiplomacyState(group.id() + 1, List.of(group), List.of());
    }

    static void clientValue(IntegerField field, @Nullable Integer value) {
        Integer old = field.getValue();
        field.setValue(value);
        ComponentUtil.fireEvent(field, new AbstractField.ComponentValueChangeEvent<>(field, field, old, true));
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
