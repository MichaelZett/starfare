package de.zettsystems.starfare.diplomacy.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.textfield.IntegerField;
import de.zettsystems.starfare.diplomacy.values.AllianceGroup;
import de.zettsystems.starfare.diplomacy.values.DiplomacyOrder;
import de.zettsystems.starfare.diplomacy.values.DiplomacyState;
import de.zettsystems.starfare.diplomacy.values.DiplomacyView;
import de.zettsystems.starfare.diplomacy.values.TreatyProposal;
import de.zettsystems.starfare.game.application.GameService;
import de.zettsystems.starfare.game.values.GameId;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.style.HtmlAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static de.zettsystems.starfare.diplomacy.ui.DiplomacyCardsTest.button;
import static de.zettsystems.starfare.diplomacy.ui.DiplomacyCardsTest.clientValue;
import static de.zettsystems.starfare.diplomacy.ui.DiplomacyCardsTest.descendants;
import static de.zettsystems.starfare.diplomacy.ui.DiplomacyCardsTest.single;
import static de.zettsystems.starfare.diplomacy.ui.DiplomacyCardsTest.spanTexts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DiplomacyDialogTest {
    private static final GameId GAME = GameId.of("diplomacy-dialog");
    private static final String ACCOUNT = "ana";
    private static final List<Player> PLAYERS = List.of(
            new Player(1, "Ana", false, "#111111"), new Player(2, "Bo", false, "#222222"),
            new Player(3, "Cy", false, "#333333"), new Player(4, "Dee", true, "#444444"),
            new Player(5, "Eve", false, "#555555"));
    private static final String LOST_TARGET =
            "Das Ziel einer Fristeingabe ist weggefallen. Die zugehörige Eingabe wurde geschlossen.";

    private final GameService game = mock(GameService.class);
    private final UI ui = new UI();
    private final List<String> notifications = new ArrayList<>();
    private DiplomacyDialog dialog;

    @BeforeEach
    void setUp() {
        ui.setLocale(Locale.GERMAN);
        UI.setCurrent(ui);
        dialog = new DiplomacyDialog(game, GAME, ACCOUNT, notifications::add);
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    @Test
    void missingViewClosesTheDialog() {
        dialog.open();
        assertThat(dialog.isOpened()).isTrue();

        dialog.refresh();

        assertThat(dialog.isOpened()).isFalse();
        assertThat(content().getChildren()).isEmpty();
    }

    @Test
    void lockedViewIsMarkedReadOnly() {
        show(view(false, DiplomacyState.EMPTY, true));
        assertThat(spanTexts(content()).getFirst())
                .isEqualTo("Nach Abgabe oder Spielende können Verträge nur gelesen werden.");

        show(view(true, DiplomacyState.EMPTY, true));
        assertThat(spanTexts(content())).doesNotContain("Nach Abgabe oder Spielende können Verträge nur gelesen werden.");
    }

    @Test
    void unalliedPlayerSeesForeignGroupsSortedAndAllFreeEmpiresAsPartners() {
        var later = new AllianceGroup(9, Set.of(3, 5), 4, Map.of());
        var earlier = new AllianceGroup(7, Set.of(2, 6), 2, Map.of());
        var players = new ArrayList<>(PLAYERS);
        players.add(new Player(6, "Fay", false, "#666666"));
        show(new DiplomacyView(4, 1, true, players, new DiplomacyState(10, List.of(later, earlier), List.of()), false));

        assertThat(headings()).containsExactly("Weitere Imperien und Bündnisse");
        Div others = others();
        assertThat(others.getChildren().map(DiplomacyDialogTest::cardTitle).toList())
                .containsExactly("Bündnis 7 · Kündigungsfrist: 2 Runden", "Bündnis 9 · Kündigungsfrist: 4 Runden", "Dee");
        assertThat(partners()).containsExactly("4");
        assertThat(spanTexts(others)).contains("KI-Imperien gehen in dieser Partie keine Bündnisse ein.");
        assertThat(lastChild()).isInstanceOf(Details.class);
        assertThat(((Details) lastChild()).getSummaryText()).isEqualTo("Bündnisregeln");
    }

    @Test
    void aiEmpiresAreOfferedWhenAiAlliancesAreAllowed() {
        show(view(true, DiplomacyState.EMPTY, true));

        assertThat(partners()).containsExactly("2", "3", "4", "5");
        assertThat(spanTexts(others())).doesNotContain("KI-Imperien gehen in dieser Partie keine Bündnisse ein.");
        assertThat(descendants(others()).filter(Details.class::isInstance)).hasSize(4);
    }

    @Test
    void alliedPlayerSeesTheOwnGroupFirstAndNoPartners() {
        var own = new AllianceGroup(5, Set.of(1, 2), 3, Map.of());
        var foreign = new AllianceGroup(6, Set.of(4, 5), 3, Map.of());
        show(view(true, new DiplomacyState(7, List.of(foreign, own), List.of()), true));

        assertThat(headings()).containsExactly("Dein Bündnis", "Weitere Imperien und Bündnisse");
        var children = content().getChildren().toList();
        assertThat(cardTitle(children.get(1))).isEqualTo("Bündnis 5 · Kündigungsfrist: 3 Runden");
        assertThat(others().getChildren().map(DiplomacyDialogTest::cardTitle).toList())
                .containsExactly("Bündnis 6 · Kündigungsfrist: 3 Runden");
        assertThat(partners()).isEmpty();
    }

    @Test
    void othersSectionIsOmittedWhenEveryoneIsInTheOwnGroup() {
        var own = new AllianceGroup(5, Set.of(1, 2, 3, 4, 5), 3, Map.of());
        show(view(true, new DiplomacyState(6, List.of(own), List.of()), true));

        assertThat(headings()).containsExactly("Dein Bündnis");
        assertThat(descendants(content()).filter(component -> component.hasClassName("diplomacy-others"))).isEmpty();
        assertThat(content().getChildren()).hasSize(3);
    }

    @Test
    void onlyProposalsThePlayerVotesOnAreDecisions() {
        var mine = new TreatyProposal(1, TreatyProposal.Kind.FOUND, 0, 2, 4, Set.of(1, 2), Set.of(2));
        var foreign = new TreatyProposal(2, TreatyProposal.Kind.FOUND, 0, 3, 4, Set.of(3, 5), Set.of(3));
        show(view(true, new DiplomacyState(3, List.of(), List.of(foreign, mine)), true));

        assertThat(headings()).containsExactly("Deine Entscheidungen", "Weitere Imperien und Bündnisse");
        var proposals = descendants(content()).filter(component -> component.hasClassName("diplomacy-proposal")).toList();
        assertThat(proposals).hasSize(1);
        assertThat(cardTitle(proposals.getFirst())).isEqualTo("Neues Bündnis: Ana, Bo · vorgeschlagene Kündigungsfrist: 4 Runden");
        assertThat(content().getChildren().toList().get(1)).isSameAs(proposals.getFirst());
    }

    @Test
    void draftSurvivesARefreshWhileItsTargetRemains() {
        show(view(true, DiplomacyState.EMPTY, true));
        clientValue(noticeField("2"), 9);
        noticeDetails("2").setOpened(true);

        show(view(true, DiplomacyState.EMPTY, true));

        assertThat(noticeField("2").getValue()).isEqualTo(9);
        assertThat(noticeDetails("2").isOpened()).isTrue();
        assertThat(noticeField("3").getValue()).isEqualTo(3);
        assertThat(notifications()).isEmpty();
    }

    @Test
    void draftForAPartnerWhoJoinedAnotherGroupIsDropped() {
        show(view(true, DiplomacyState.EMPTY, true));
        clientValue(noticeField("2"), 9);
        noticeDetails("2").setOpened(true);
        noticeDetails("3").setOpened(true);

        var group = new AllianceGroup(1, Set.of(2, 3), 3, Map.of());
        show(view(true, new DiplomacyState(2, List.of(group), List.of()), true));
        assertThat(partners()).containsExactly("4", "5");
        assertThat(notifications()).containsExactly(LOST_TARGET);

        show(view(true, DiplomacyState.EMPTY, true));
        assertThat(noticeField("2").getValue()).isEqualTo(3);
        assertThat(noticeDetails("2").isOpened()).isFalse();
        assertThat(noticeDetails("3").isOpened()).isFalse();
        assertThat(notifications()).containsExactly(LOST_TARGET);
    }

    @Test
    void draftForAnAiPartnerIsDroppedOnceAiAlliancesAreForbidden() {
        show(view(true, DiplomacyState.EMPTY, true));
        clientValue(noticeField("4"), 11);

        show(view(true, DiplomacyState.EMPTY, false));
        assertThat(notifications()).containsExactly(LOST_TARGET);

        show(view(true, DiplomacyState.EMPTY, true));
        assertThat(noticeField("4").getValue()).isEqualTo(3);
    }

    @Test
    void foundingDraftsAreDroppedWhenThePlayerJoinsAGroup() {
        show(view(true, DiplomacyState.EMPTY, true));
        clientValue(noticeField("2"), 9);

        var own = new AllianceGroup(1, Set.of(1, 3), 6, Map.of());
        show(view(true, new DiplomacyState(2, List.of(own), List.of()), true));
        assertThat(notifications()).containsExactly(LOST_TARGET);
        assertThat(single(content().getChildren().toList().get(1), IntegerField.class).getValue()).isEqualTo(6);

        show(view(true, DiplomacyState.EMPTY, true));
        assertThat(noticeField("2").getValue()).isEqualTo(3);
    }

    @Test
    void noticeDraftOfTheOwnGroupSurvivesUntilThePlayerHasLeft() {
        var own = new AllianceGroup(5, Set.of(1, 2), 3, Map.of());
        var allied = view(true, new DiplomacyState(6, List.of(own), List.of()), true);
        show(allied);
        clientValue(ownNoticeField(), 12);

        show(allied);
        assertThat(ownNoticeField().getValue()).isEqualTo(12);
        assertThat(notifications()).isEmpty();

        show(view(true, DiplomacyState.EMPTY, true));
        assertThat(notifications()).containsExactly(LOST_TARGET);
        show(allied);
        assertThat(ownNoticeField().getValue()).isEqualTo(3);
    }

    @Test
    void acceptedOrderClearsItsDraftAndRefreshes() {
        var view = view(true, DiplomacyState.EMPTY, true);
        show(view);
        dialog.open();
        clientValue(noticeField("2"), 9);
        var order = new DiplomacyOrder(DiplomacyOrder.Action.FOUND, 2, 9);
        when(game.diplomacyOrder(GAME, ACCOUNT, 4, order)).thenReturn(true);

        button(partnerCard("2"), "Bündnis vorschlagen").click();

        verify(game).diplomacyOrder(GAME, ACCOUNT, 4, order);
        assertThat(noticeField("2").getValue()).isEqualTo(3);
        assertThat(notifications()).isEmpty();
    }

    @Test
    void rejectedOrderKeepsTheDraftAndExplains() {
        show(view(true, DiplomacyState.EMPTY, true));
        dialog.open();
        clientValue(noticeField("2"), 9);

        button(partnerCard("2"), "Bündnis vorschlagen").click();

        assertThat(noticeField("2").getValue()).isEqualTo(9);
        assertThat(notifications()).containsExactly("Vertragsaktion abgelehnt. Der Stand wurde aktualisiert.");
    }

    @Test
    void closedDialogSendsNothing() {
        show(view(true, DiplomacyState.EMPTY, true));

        button(partnerCard("2"), "Bündnis vorschlagen").click();

        verify(game, never()).diplomacyOrder(any(), anyString(), anyInt(), any());
    }

    private void show(DiplomacyView view) {
        when(game.diplomacyFor(GAME, ACCOUNT)).thenReturn(Optional.of(view));
        dialog.refresh();
    }

    private static DiplomacyView view(boolean canAct, DiplomacyState state, boolean aiAlliancesAllowed) {
        return new DiplomacyView(4, 1, canAct, PLAYERS, state, aiAlliancesAllowed);
    }

    private Div content() {
        return descendants(dialog).filter(component -> component.hasClassName("diplomacy-content"))
                .map(Div.class::cast).findFirst().orElseThrow();
    }

    private Div others() {
        return descendants(content()).filter(component -> component.hasClassName("diplomacy-others"))
                .map(Div.class::cast).findFirst().orElseThrow();
    }

    private Component lastChild() {
        return content().getChildren().toList().getLast();
    }

    private List<String> headings() {
        return content().getChildren().filter(H3.class::isInstance).map(H3.class::cast).map(H3::getText).toList();
    }

    private List<String> partners() {
        return descendants(content()).map(component -> component.getElement().getAttribute(HtmlAttributes.DIPLOMACY_PARTNER))
                .filter(java.util.Objects::nonNull).toList();
    }

    private Component partnerCard(String player) {
        return descendants(content())
                .filter(component -> player.equals(component.getElement().getAttribute(HtmlAttributes.DIPLOMACY_PARTNER)))
                .findFirst().orElseThrow();
    }

    private IntegerField noticeField(String player) {
        return single(partnerCard(player), IntegerField.class);
    }

    private Details noticeDetails(String player) {
        return single(partnerCard(player), Details.class);
    }

    private IntegerField ownNoticeField() {
        return single(content().getChildren().toList().get(1), IntegerField.class);
    }

    private List<String> notifications() {
        return notifications;
    }

    private static String cardTitle(Component card) {
        return spanTexts(card).getFirst();
    }
}
