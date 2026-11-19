package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.report.values.TurnEvent;

import java.util.function.Supplier;

/** Report line and replay sides of a round event; a pending battle hides its outcome. */
final class RoundEventTexts {
    private RoundEventTexts() {
    }

    static String text(TurnEvent event, boolean pending) {
        return switch (event) {
            case TurnEvent.CoalitionBattle battle -> battle(pending, battle.systemName(), () -> CoalitionPresentation.report(battle));
            case TurnEvent.Recycling r -> I18n.t(UiTexts.RECYCLING_EVENT, r.systemName(), r.ships(), r.recoveredTenths() / 10.0);
            case TurnEvent.Production production -> I18n.t(UiTexts.ROUND_EVENT_PRODUCTION,
                    production.systemName(), production.amount());
            case TurnEvent.Reinforcement reinforcement -> I18n.t(UiTexts.ROUND_EVENT_REINFORCEMENT,
                    reinforcement.systemName(), reinforcement.ships(), reinforcement.totalGarrison(), reinforcement.fleetLabel());
            case TurnEvent.BattleWon battle -> battle(pending, battle.systemName(), () -> battleWon(battle));
            case TurnEvent.BattleLost battle -> battle(pending, battle.systemName(), () -> I18n.t(UiTexts.ROUND_EVENT_BATTLE_LOST,
                    battle.systemName(), battle.attacking(), battle.defending(), battle.defendersLeft()));
            case TurnEvent.SystemLost lost -> battle(pending, lost.systemName(),
                    () -> I18n.t(UiTexts.ROUND_EVENT_SYSTEM_LOST, lost.systemName()));
            case TurnEvent.DefenseHeld held -> battle(pending, held.systemName(), () -> I18n.t(UiTexts.ROUND_EVENT_DEFENSE_HELD,
                    held.systemName(), held.attacking(), held.defendersLeft()));
            case TurnEvent.Victory victory -> victory(victory);
            case TurnEvent.Defeat defeat -> I18n.t(UiTexts.ROUND_EVENT_DEFEAT, defeat.winnerName());
            case TurnEvent.ResearchCompleted research -> I18n.t(UiTexts.RESEARCH_COMPLETED, research.level(), research.level() * 10);
            case TurnEvent.EconomySummary _ -> "";
            case TurnEvent.Cosmic news -> CosmicNewsTexts.text(news);
            case TurnEvent.FinalScore score -> CosmicNewsTexts.score(score);
        };
    }

    static BattleReplayDialog.BattleSides sides(TurnEvent event) {
        return switch (event) {
            case TurnEvent.BattleWon battle -> new BattleReplayDialog.BattleSides(you(battle.attackerName()),
                    battle.wasNeutral() ? I18n.t(UiTexts.BATTLE_REPLAY_NEUTRAL) : opponent(battle.defenderName()));
            case TurnEvent.BattleLost battle -> new BattleReplayDialog.BattleSides(you(battle.attackerName()),
                    opponent(battle.defenderName()));
            case TurnEvent.SystemLost lost -> new BattleReplayDialog.BattleSides(opponent(lost.attackerName()),
                    you(lost.defenderName()));
            case TurnEvent.DefenseHeld held -> new BattleReplayDialog.BattleSides(opponent(held.attackerName()),
                    you(held.defenderName()));
            case TurnEvent.CoalitionBattle _, TurnEvent.Recycling _, TurnEvent.Production _, TurnEvent.Reinforcement _,
                 TurnEvent.Victory _, TurnEvent.Defeat _, TurnEvent.EconomySummary _, TurnEvent.ResearchCompleted _,
                 TurnEvent.Cosmic _, TurnEvent.FinalScore _ -> new BattleReplayDialog.BattleSides("", "");
        };
    }

    private static String battle(boolean pending, String systemName, Supplier<String> outcome) {
        return pending ? I18n.t(UiTexts.ROUND_EVENT_BATTLE_READY, systemName) : outcome.get();
    }

    private static String battleWon(TurnEvent.BattleWon battle) {
        return I18n.t(battle.wasNeutral() ? UiTexts.ROUND_EVENT_BATTLE_WON_NEUTRAL : UiTexts.ROUND_EVENT_BATTLE_WON_ENEMY,
                battle.systemName(), battle.attacking(), battle.defending(), battle.remaining());
    }

    private static String victory(TurnEvent.Victory victory) {
        if (victory.byScore()) {
            return I18n.t(UiTexts.ROUND_EVENT_VICTORY_SCORE);
        }
        return victory.allianceId() == null
                ? I18n.t(UiTexts.ROUND_EVENT_VICTORY, victory.systemPercent())
                : I18n.t(UiTexts.ROUND_EVENT_ALLIANCE_VICTORY, victory.allianceId(),
                        String.join(", ", victory.winnerNames()), victory.systemPercent());
    }

    private static String you(String name) {
        return name.isBlank() ? I18n.t(UiTexts.BATTLE_REPLAY_YOU) : I18n.t(UiTexts.BATTLE_REPLAY_YOU) + " · " + name;
    }

    private static String opponent(String name) {
        return name.isBlank() ? I18n.t(UiTexts.BATTLE_REPLAY_OPPONENT) : name;
    }
}
