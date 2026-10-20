package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.style.HtmlAttributes;
import de.zettsystems.starfare.report.values.CoalitionSide;
import de.zettsystems.starfare.report.values.TurnEvent;
import java.util.List;

/** Battle facts only; the ownership outcome is shown by the report after acknowledgement. */
final class CoalitionPresentation {
    private CoalitionPresentation() { }
    static void populate(Div visual, de.zettsystems.starfare.report.values.BattleReplay replay) {
        List<CoalitionSide> sides = replay.coalitions();
        visual.addClassName("coalition-replay");
        if (replay.defenderSideId() == null) {
            visual.addClassName("coalition-roles-unknown");
            visual.add(new Span(I18n.t(UiTexts.BATTLE_ROLES_UNKNOWN)));
        }
        Div attackers = new Div(); attackers.addClassName("coalition-attackers");
        Div defenders = new Div(); defenders.addClassName("coalition-defenders");
        visual.add(attackers, defenders);
        for (var side : sides.stream().sorted(java.util.Comparator.comparingInt(CoalitionSide::id)).toList()) {
            Div card = new Div(); card.addClassNames("battle-side", "coalition-side");
            card.getElement().setAttribute(HtmlAttributes.COALITION_SIDE, Integer.toString(side.id()));
            boolean defender = Integer.valueOf(side.id()).equals(replay.defenderSideId());
            if (replay.defenderSideId() != null) {
                card.add(new Span(I18n.t(defender ? UiTexts.BATTLE_REPLAY_DEFENDERS : UiTexts.BATTLE_REPLAY_ATTACKERS)));
            }
            card.add(new Span(identity(side)));
            BattleCondition.addTo(card, side.ships(), side.strength(), side.baseStrength(), side.id(),
                    CombatStrengthFormat.forCoalition(side.strength(), sides.stream().map(CoalitionSide::strength).toList(),
                            com.vaadin.flow.component.UI.getCurrentOrThrow().getLocale()));
            Span count = new Span(Integer.toString(side.ships())); count.addClassName("battle-number");
            count.getElement().setAttribute(HtmlAttributes.COALITION_COUNT, ""); card.add(count);
            for (var member : side.members()) {
                Span row = new Span(I18n.t(UiTexts.COALITION_MEMBER_START, memberName(member), member.ships()));
                row.getElement().setAttribute(HtmlAttributes.COALITION_MEMBER, Integer.toString(member.playerId()));
                row.getElement().setAttribute(HtmlAttributes.COALITION_FINAL, I18n.t(UiTexts.COALITION_MEMBER_END,
                        memberName(member), member.remaining(), member.ships() - member.remaining()));
                card.add(row);
            }
            (defender ? defenders : attackers).add(card);
        }
        if (sides.size() > 2 && replay.defenderSideId() != null) {
            visual.add(new Span(I18n.t(UiTexts.BATTLE_MULTIPLE_ATTACKERS)));
        }
        Span result = new Span(I18n.t(UiTexts.COALITION_RESOLVED));
        result.addClassName("battle-replay-result"); result.getElement().setAttribute(HtmlAttributes.BATTLE_RESULT, ""); visual.add(result);
    }
    static String identity(CoalitionSide side) {
        return side.members().stream().map(CoalitionPresentation::memberName).reduce((a, b) -> a + " + " + b).orElse("");
    }
    private static String memberName(CoalitionSide.Member member) {
        return member.playerId() == 0 ? I18n.t(UiTexts.BATTLE_REPLAY_NEUTRAL) : member.name();
    }
    static String report(TurnEvent.CoalitionBattle battle) {
        String survivors = battle.sides().stream().map(side -> I18n.t(UiTexts.COALITION_SIDE_RESULT, identity(side), side.remaining()))
                .reduce((a, b) -> a + " · " + b).orElse("");
        String owner = battle.owner() == null ? I18n.t(UiTexts.BATTLE_REPLAY_NEUTRAL) : battle.ownerName();
        return I18n.t(UiTexts.COALITION_REPORT, battle.systemName(), survivors, owner);
    }
}
