package de.zettsystems.starfare.game.ui;

import de.zettsystems.starfare.game.values.GameOutcome;
import de.zettsystems.starfare.game.values.Player;
import de.zettsystems.starfare.i18n.I18n;
import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

final class OutcomeLabels {
    private OutcomeLabels() { }
    static String winners(@Nullable GameOutcome outcome, List<Player> players) {
        return winners(outcome, players, Player::label);
    }
    static String winningEmpires(@Nullable GameOutcome outcome, List<Player> players) {
        return winners(outcome, players, Player::empireNameOrName);
    }
    private static String winners(@Nullable GameOutcome outcome, List<Player> players, Function<Player, String> label) {
        if (outcome == null || !outcome.hasWinner()) { return I18n.t(UiTexts.ARCHIVE_NO_WINNER); }
        String names = players.stream().filter(p -> outcome.wonBy(p.id())).map(label).collect(Collectors.joining(", "));
        Integer group = outcome.allianceId();
        return group == null ? names : I18n.t(UiTexts.ALLIANCE_WINNERS, group, names);
    }
}
