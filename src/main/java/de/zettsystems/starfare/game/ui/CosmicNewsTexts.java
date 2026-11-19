package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.i18n.I18n;
import de.zettsystems.starfare.report.values.TurnEvent;

import java.util.stream.Collectors;

/** Report lines of the SectorForces news ticker and the final score. */
final class CosmicNewsTexts {
    private CosmicNewsTexts() {
    }

    static String text(TurnEvent.Cosmic news) {
        return switch (news.news()) {
            case STORM -> I18n.t(UiTexts.SECTOR_NEWS_STORM);
            case SPACETIME_QUAKE -> I18n.t(UiTexts.SECTOR_NEWS_SPACETIME_QUAKE);
            case DISAPPEARANCE -> news.remaining() > 0
                    ? I18n.t(UiTexts.SECTOR_NEWS_DISAPPEARANCE, news.fleetNo(), news.systemName(), news.ships(), news.remaining())
                    : I18n.t(UiTexts.SECTOR_NEWS_DISAPPEARANCE_ALL, news.fleetNo(), news.systemName(), news.ships());
            case SPACEPORT_QUAKE_LIGHT -> I18n.t(UiTexts.SECTOR_NEWS_QUAKE_LIGHT, news.systemName(), news.ships(), news.remaining());
            case SPACEPORT_QUAKE_HEAVY -> I18n.t(UiTexts.SECTOR_NEWS_QUAKE_HEAVY, news.systemName(), news.ships(), news.remaining());
            case SUN_UNSTABLE -> I18n.t(UiTexts.SECTOR_NEWS_SUN_UNSTABLE, news.systemName(), news.round());
            case SUPERNOVA -> I18n.t(UiTexts.SECTOR_NEWS_SUPERNOVA, news.systemName());
            case REFUGEES -> I18n.t(UiTexts.SECTOR_NEWS_REFUGEES, news.systemName(), news.ships(), news.fleetNo(),
                    news.targetName(), news.round());
            case PIRATES_FLEE -> I18n.t(UiTexts.SECTOR_NEWS_PIRATES_FLEE, news.systemName(), news.ships(),
                    news.targetName(), news.round());
            case PANIC_JUMP -> I18n.t(UiTexts.SECTOR_NEWS_PANIC_JUMP, news.fleetNo(), news.systemName(),
                    news.targetName(), news.round());
            case PIRATE_RAID_WON -> I18n.t(UiTexts.SECTOR_NEWS_RAID_WON, news.systemName(), news.ships(), news.remaining());
            case PIRATE_RAID_REPELLED -> I18n.t(UiTexts.SECTOR_NEWS_RAID_REPELLED, news.systemName(), news.ships(),
                    news.remaining());
            case ROUND_LIMIT_NEAR -> I18n.t(UiTexts.SECTOR_NEWS_ROUND_LIMIT, news.round());
        };
    }

    static String score(TurnEvent.FinalScore score) {
        String lines = score.lines().stream()
                .map(line -> I18n.t(UiTexts.SECTOR_SCORE_LINE, line.name(), line.points()))
                .collect(Collectors.joining(" · "));
        return I18n.t(UiTexts.SECTOR_SCORE_TITLE, score.round(), lines);
    }
}
